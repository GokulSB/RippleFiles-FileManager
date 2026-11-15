// Native7zBridge.cpp - High-performance JNI bridge for 7-Zip-zstd
#include "StdAfx.h"
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>

#include "../../../Common/MyWindows.h"
#include "../../../Common/MyInitGuid.h"
#include "../../../Common/Defs.h"
#include "../../../Common/IntToString.h"
#include "../../../Common/StringConvert.h"
#include "../../../Common/UTFConvert.h"

#include "../../../Windows/DLL.h"
#include "../../../Windows/FileDir.h"
#include "../../../Windows/FileFind.h"
#include "../../../Windows/FileIO.h"
#include "../../../Windows/FileName.h"
#include "../../../Windows/NtCheck.h"
#include "../../../Windows/PropVariant.h"
#include "../../../Windows/PropVariantConv.h"
#include "../../../Windows/TimeUtils.h"

#include "../../Common/FileStreams.h"
#include "../../Archive/IArchive.h"
#include "../../IPassword.h"

#define TAG "Native7zBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

using namespace NWindows;
using namespace NFile;
using namespace NDir;

static FString CmdStringToFString(const char *s)
{
  return us2fs(GetUnicodeString(s));
}

static jstring UStringToJString(JNIEnv *env, const UString &s)
{
  AString utf8;
  ConvertUnicodeToUTF8(s, utf8);
  return env->NewStringUTF(utf8.Ptr());
}

static UString JStringToUString(JNIEnv *env, jstring js)
{
  if (!js)
    return UString();
  const char *utf8 = env->GetStringUTFChars(js, NULL);
  UString res;
  if (utf8)
  {
    ConvertUTF8ToUnicode(AString(utf8), res);
    env->ReleaseStringUTFChars(js, utf8);
  }
  return res;
}

// -------------------------------------------------------------
// Format metadata
// -------------------------------------------------------------
struct CFormatInfo
{
  UInt32 Index;
  UString Name;
  UString Ext;
  UString AddExt;
  GUID ClsId;
  bool Update;
  CByteBuffer Sig;
  UInt32 SigOffset;

  CFormatInfo() : Index(0), Update(false), SigOffset(0) { memset(&ClsId, 0, sizeof(ClsId)); }
};

static NDLL::CLibrary g_Lib7z;
static Func_CreateObject g_CreateObject = NULL;
static Func_GetNumberOfFormats g_GetNumberOfFormats = NULL;
static Func_GetHandlerProperty2 g_GetHandlerProperty2 = NULL;
static CObjectVector<CFormatInfo> g_Formats;
static bool g_Initialized = false;

// -------------------------------------------------------------
// Multi-Threaded JNI Support
// -------------------------------------------------------------
static JavaVM *g_JavaVM = NULL;
static pthread_key_t g_TlsKey;
static pthread_once_t g_TlsOnce = PTHREAD_ONCE_INIT;

static void TlsThreadDestructor(void *val)
{
  if (g_JavaVM && val != NULL)
  {
    g_JavaVM->DetachCurrentThread();
  }
}

static void InitTls()
{
  pthread_key_create(&g_TlsKey, TlsThreadDestructor);
}

static JNIEnv *GetCurrentThreadEnv()
{
  if (!g_JavaVM)
    return NULL;

  JNIEnv *env = NULL;
  jint res = g_JavaVM->GetEnv((void **)&env, JNI_VERSION_1_6);
  if (res == JNI_OK)
    return env;

  if (res == JNI_EDETACHED)
  {
    pthread_once(&g_TlsOnce, InitTls);
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6;
    args.name = (char *)"7z-Worker";
    args.group = NULL;
    if (g_JavaVM->AttachCurrentThread(&env, &args) == 0)
    {
      pthread_setspecific(g_TlsKey, (void *)1);
      return env;
    }
  }

  return NULL;
}

// -------------------------------------------------------------
// Zip-Slip Path Traversal Detection
// -------------------------------------------------------------
static bool IsZipSlipPath(const UString &path)
{
  if (path.IsEmpty())
    return false;
  if (path[0] == L'/' || path[0] == L'\\')
    return true;
  if (path.Len() >= 2 && path[1] == L':')
    return true;

  int start = 0;
  int len = path.Len();
  for (int i = 0; i <= len; i++)
  {
    if (i == len || path[i] == L'/' || path[i] == L'\\')
    {
      int segLen = i - start;
      if (segLen == 2 && path[start] == L'.' && path[start + 1] == L'.')
        return true;
      start = i + 1;
    }
  }
  return false;
}

// -------------------------------------------------------------
// Archive Open Callback (with Volume & Password support)
// -------------------------------------------------------------
class CArchiveOpenCallback Z7_final:
  public IArchiveOpenCallback,
  public IArchiveOpenVolumeCallback,
  public ICryptoGetTextPassword,
  public CMyUnknownImp
{
  Z7_IFACES_IMP_UNK_3(IArchiveOpenCallback, IArchiveOpenVolumeCallback, ICryptoGetTextPassword)

public:
  bool PasswordIsDefined;
  UString Password;
  UString FolderPrefix;
  jobject Callback;
  jmethodID PasswordReqMethod;

  CArchiveOpenCallback() : PasswordIsDefined(false), Callback(NULL), PasswordReqMethod(NULL) {}

  ~CArchiveOpenCallback()
  {
    if (Callback)
    {
      JNIEnv *env = GetCurrentThreadEnv();
      if (env)
        env->DeleteGlobalRef(Callback);
      Callback = NULL;
    }
  }

  void SetPassword(const UString &pwd)
  {
    Password = pwd;
    PasswordIsDefined = !pwd.IsEmpty();
  }
};

Z7_COM7F_IMF(CArchiveOpenCallback::SetTotal(const UInt64 * /* files */, const UInt64 * /* bytes */)) { return S_OK; }
Z7_COM7F_IMF(CArchiveOpenCallback::SetCompleted(const UInt64 * /* files */, const UInt64 * /* bytes */)) { return S_OK; }

Z7_COM7F_IMF(CArchiveOpenCallback::GetProperty(PROPID propID, PROPVARIANT *value))
{
  NCOM::CPropVariant prop;
  if (propID == kpidName)
  {
    // Return empty or volume name if available
    prop.Detach(value);
    return S_OK;
  }
  return S_OK;
}

Z7_COM7F_IMF(CArchiveOpenCallback::GetStream(const wchar_t *name, IInStream **inStream))
{
  *inStream = NULL;
  UString fullPath = FolderPrefix + name;
  CInFileStream *fileStream = new CInFileStream;
  CMyComPtr<IInStream> inStreamLoc(fileStream);

  if (!fileStream->Open(us2fs(fullPath)))
  {
    LOGW("Multi-volume: could not open volume '%ls'", (const wchar_t *)fullPath);
    return S_FALSE;
  }

  *inStream = inStreamLoc.Detach();
  return S_OK;
}

Z7_COM7F_IMF(CArchiveOpenCallback::CryptoGetTextPassword(BSTR *password))
{
  if (!PasswordIsDefined && Callback && PasswordReqMethod)
  {
    JNIEnv *env = GetCurrentThreadEnv();
    if (env)
    {
      jstring jPwd = (jstring)env->CallObjectMethod(Callback, PasswordReqMethod);
      if (jPwd)
      {
        SetPassword(JStringToUString(env, jPwd));
        env->DeleteLocalRef(jPwd);
      }
    }
  }

  if (!PasswordIsDefined)
    return E_ABORT;

  return StringToBstr(Password, password);
}

// -------------------------------------------------------------
// Archive Extract Callback (with Zip-Slip check, Progress & Conflict)
// -------------------------------------------------------------
class CArchiveExtractCallback Z7_final:
  public IArchiveExtractCallback,
  public ICryptoGetTextPassword,
  public CMyUnknownImp
{
  Z7_IFACES_IMP_UNK_2(IArchiveExtractCallback, ICryptoGetTextPassword)
  Z7_IFACE_COM7_IMP(IProgress)

  CMyComPtr<IInArchive> _archiveHandler;
  FString _directoryPath;
  UString _filePath;
  FString _diskFilePath;
  bool _extractMode;
  bool _isDir;
  COutFileStream *_outFileStreamSpec;
  CMyComPtr<ISequentialOutStream> _outFileStream;

  // JNI callback state
  jobject _callback;
  jmethodID _isCancelledMethod;
  jmethodID _onProgressMethod;
  jmethodID _onPasswordMethod;
  jmethodID _onConflictMethod;

  UInt64 _totalBytes;
  UInt64 _completedBytes;
  int _conflictPolicy; // 0=prompt, 1=overwrite_all, 2=skip_all

public:
  UInt64 NumErrors;
  UInt64 NumFiles;
  bool PasswordIsDefined;
  UString Password;

  CArchiveExtractCallback() :
    _outFileStreamSpec(NULL),
    _callback(NULL),
    _isCancelledMethod(NULL),
    _onProgressMethod(NULL),
    _onPasswordMethod(NULL),
    _onConflictMethod(NULL),
    _totalBytes(0),
    _completedBytes(0),
    _conflictPolicy(0),
    NumErrors(0),
    NumFiles(0),
    PasswordIsDefined(false)
  {}

  ~CArchiveExtractCallback()
  {
    if (_callback)
    {
      JNIEnv *env = GetCurrentThreadEnv();
      if (env)
        env->DeleteGlobalRef(_callback);
      _callback = NULL;
    }
  }

  void Init(IInArchive *archive, const FString &destDir, JNIEnv *env, jobject cb, const UString &pwd)
  {
    _archiveHandler = archive;
    _directoryPath = destDir;
    NName::NormalizeDirPathPrefix(_directoryPath);
    _callback = NULL;
    _isCancelledMethod = NULL;
    _onProgressMethod = NULL;
    _onPasswordMethod = NULL;
    _onConflictMethod = NULL;
    _conflictPolicy = 0;
    NumErrors = 0;
    NumFiles = 0;
    _totalBytes = 0;
    _completedBytes = 0;

    if (!pwd.IsEmpty())
    {
      Password = pwd;
      PasswordIsDefined = true;
    }
    else
    {
      PasswordIsDefined = false;
    }

    if (env && cb)
    {
      _callback = env->NewGlobalRef(cb);
      jclass cbClass = env->GetObjectClass(_callback);
      _isCancelledMethod = env->GetMethodID(cbClass, "isCancelled", "()Z");
      _onProgressMethod = env->GetMethodID(cbClass, "onProgress", "(JJLjava/lang/String;)V");
      _onPasswordMethod = env->GetMethodID(cbClass, "onPasswordRequired", "()Ljava/lang/String;");
      _onConflictMethod = env->GetMethodID(cbClass, "onFileConflict", "(Ljava/lang/String;)I");
      env->DeleteLocalRef(cbClass);
    }
  }

  bool CheckCancel()
  {
    if (_callback && _isCancelledMethod)
    {
      JNIEnv *env = GetCurrentThreadEnv();
      if (env)
      {
        jboolean cancelled = env->CallBooleanMethod(_callback, _isCancelledMethod);
        if (cancelled)
          return true;
      }
    }
    return false;
  }
};

Z7_COM7F_IMF(CArchiveExtractCallback::SetTotal(UInt64 size))
{
  _totalBytes = size;
  return S_OK;
}

Z7_COM7F_IMF(CArchiveExtractCallback::SetCompleted(const UInt64 *completeValue))
{
  if (completeValue)
    _completedBytes = *completeValue;

  if (CheckCancel())
    return E_ABORT;

  if (_callback && _onProgressMethod)
  {
    JNIEnv *env = GetCurrentThreadEnv();
    if (env)
    {
      jstring curItem = UStringToJString(env, _filePath);
      env->CallVoidMethod(_callback, _onProgressMethod, (jlong)_completedBytes, (jlong)_totalBytes, curItem);
      if (curItem)
        env->DeleteLocalRef(curItem);
    }
  }
  return S_OK;
}

Z7_COM7F_IMF(CArchiveExtractCallback::GetStream(UInt32 index, ISequentialOutStream **outStream, Int32 askExtractMode))
{
  *outStream = NULL;
  _outFileStream.Release();

  if (CheckCancel())
    return E_ABORT;

  NCOM::CPropVariant prop;
  RINOK(_archiveHandler->GetProperty(index, kpidPath, &prop));
  if (prop.vt == VT_EMPTY)
    _filePath = L"[Content]";
  else if (prop.vt == VT_BSTR)
    _filePath = prop.bstrVal;
  else
    return E_FAIL;

  if (askExtractMode != NArchive::NExtract::NAskMode::kExtract)
    return S_OK;

  // -----------------------------------------------------------
  // Zip-Slip Security Check: strictly reject path traversal!
  // -----------------------------------------------------------
  if (IsZipSlipPath(_filePath))
  {
    LOGE("ZIP-SLIP SECURITY VIOLATION: Entry rejected: '%ls'", (const wchar_t *)_filePath);
    NumErrors++;
    return E_ABORT;
  }

  // Check if directory
  _isDir = false;
  NCOM::CPropVariant propDir;
  if (_archiveHandler->GetProperty(index, kpidIsDir, &propDir) == S_OK)
  {
    if (propDir.vt == VT_BOOL)
      _isDir = VARIANT_BOOLToBool(propDir.boolVal);
  }

  FString fullProcessedPath = _directoryPath + us2fs(_filePath);

  // Additional Zip-Slip canonical path check: output path must start with destination prefix
  if (fullProcessedPath.Find(_directoryPath) != 0)
  {
    LOGE("ZIP-SLIP SECURITY VIOLATION: Path escapes destination folder: '%s'", (const char *)fullProcessedPath);
    NumErrors++;
    return E_ABORT;
  }

  _diskFilePath = fullProcessedPath;

  if (_isDir)
  {
    CreateComplexDir(fullProcessedPath);
    return S_OK;
  }

  // Handle overwrite conflict if file already exists
  NFind::CFileInfo fi;
  if (fi.Find(fullProcessedPath))
  {
    if (_conflictPolicy == 2) // Skip all
    {
      return S_OK;
    }
    else if (_conflictPolicy != 1) // Not overwrite all -> check with callback
    {
      if (_callback && _onConflictMethod)
      {
        JNIEnv *env = GetCurrentThreadEnv();
        if (env)
        {
          jstring jRelPath = UStringToJString(env, _filePath);
          int decision = env->CallIntMethod(_callback, _onConflictMethod, jRelPath);
          if (jRelPath)
            env->DeleteLocalRef(jRelPath);

          if (decision == 1) // Skip
            return S_OK;
          if (decision == 2) // Overwrite all
            _conflictPolicy = 1;
          if (decision == 3) // Skip all
          {
            _conflictPolicy = 2;
            return S_OK;
          }
        }
      }
    }
    // Delete existing file before overwrite
    DeleteFileAlways(fullProcessedPath);
  }

  // Create parent dirs
  int slashPos = _filePath.ReverseFind_PathSepar();
  if (slashPos >= 0)
    CreateComplexDir(_directoryPath + us2fs(_filePath.Left(slashPos)));

  _outFileStreamSpec = new COutFileStream;
  CMyComPtr<ISequentialOutStream> outStreamLoc(_outFileStreamSpec);
  if (!_outFileStreamSpec->Create_ALWAYS(fullProcessedPath))
  {
    LOGE("Cannot create output file: '%s'", (const char *)fullProcessedPath);
    NumErrors++;
    return E_ABORT;
  }

  _outFileStream = outStreamLoc;
  *outStream = outStreamLoc.Detach();
  NumFiles++;
  return S_OK;
}

Z7_COM7F_IMF(CArchiveExtractCallback::PrepareOperation(Int32 askExtractMode))
{
  _extractMode = (askExtractMode == NArchive::NExtract::NAskMode::kExtract);
  return S_OK;
}

Z7_COM7F_IMF(CArchiveExtractCallback::SetOperationResult(Int32 operationResult))
{
  if (operationResult != NArchive::NExtract::NOperationResult::kOK)
  {
    NumErrors++;
    LOGE("Extract operation result error (%d) on entry '%ls'", operationResult, (const wchar_t *)_filePath);
  }
  if (_outFileStream)
  {
    _outFileStreamSpec->Close();
  }
  _outFileStream.Release();
  return S_OK;
}

Z7_COM7F_IMF(CArchiveExtractCallback::CryptoGetTextPassword(BSTR *password))
{
  if (!PasswordIsDefined && _callback && _onPasswordMethod)
  {
    JNIEnv *env = GetCurrentThreadEnv();
    if (env)
    {
      jstring jPwd = (jstring)env->CallObjectMethod(_callback, _onPasswordMethod);
      if (jPwd)
      {
        Password = JStringToUString(env, jPwd);
        PasswordIsDefined = !Password.IsEmpty();
        env->DeleteLocalRef(jPwd);
      }
    }
  }

  if (!PasswordIsDefined)
    return E_ABORT;

  return StringToBstr(Password, password);
}

// -------------------------------------------------------------
// Archive Creation Callback
// -------------------------------------------------------------
struct CDirItem : public NWindows::NFile::NFind::CFileInfoBase
{
  UString Path_For_Handler;
  FString FullPath;

  CDirItem(const NWindows::NFile::NFind::CFileInfo &fi) : CFileInfoBase(fi) {}
};

class CArchiveUpdateCallback Z7_final :
  public IArchiveUpdateCallback2,
  public ICryptoGetTextPassword2,
  public CMyUnknownImp
{
  Z7_IFACES_IMP_UNK_2(IArchiveUpdateCallback2, ICryptoGetTextPassword2)
  Z7_IFACE_COM7_IMP(IProgress)
  Z7_IFACE_COM7_IMP(IArchiveUpdateCallback)

public:
  UInt64 SplitVolumeSize;
  UString VolBaseName;
  UString VolExt;
  const CObjectVector<CDirItem> *DirItems;

  bool PasswordIsDefined;
  UString Password;

  jobject Callback;
  jmethodID OnProgressMethod;
  jmethodID IsCancelledMethod;

  UInt64 TotalBytes;
  UInt64 CompletedBytes;
  UInt64 NumErrors;

  CArchiveUpdateCallback() :
    SplitVolumeSize(0),
    DirItems(NULL),
    PasswordIsDefined(false),
    Callback(NULL),
    OnProgressMethod(NULL),
    IsCancelledMethod(NULL),
    TotalBytes(0),
    CompletedBytes(0),
    NumErrors(0)
  {}

  ~CArchiveUpdateCallback()
  {
    if (Callback)
    {
      JNIEnv *env = GetCurrentThreadEnv();
      if (env)
        env->DeleteGlobalRef(Callback);
      Callback = NULL;
    }
  }

  void Init(JNIEnv *env, jobject cb, const UString &pwd)
  {
    Password = pwd;
    PasswordIsDefined = !pwd.IsEmpty();
    if (env && cb)
    {
      Callback = env->NewGlobalRef(cb);
      jclass cbClass = env->GetObjectClass(Callback);
      IsCancelledMethod = env->GetMethodID(cbClass, "isCancelled", "()Z");
      OnProgressMethod = env->GetMethodID(cbClass, "onProgress", "(JJLjava/lang/String;)V");
      env->DeleteLocalRef(cbClass);
    }
  }
};

Z7_COM7F_IMF(CArchiveUpdateCallback::SetTotal(UInt64 size))
{
  TotalBytes = size;
  return S_OK;
}

Z7_COM7F_IMF(CArchiveUpdateCallback::SetCompleted(const UInt64 *completeValue))
{
  if (completeValue)
    CompletedBytes = *completeValue;

  if (Callback && IsCancelledMethod)
  {
    JNIEnv *env = GetCurrentThreadEnv();
    if (env && env->CallBooleanMethod(Callback, IsCancelledMethod))
      return E_ABORT;
  }

  if (Callback && OnProgressMethod)
  {
    JNIEnv *env = GetCurrentThreadEnv();
    if (env)
      env->CallVoidMethod(Callback, OnProgressMethod, (jlong)CompletedBytes, (jlong)TotalBytes, NULL);
  }
  return S_OK;
}

Z7_COM7F_IMF(CArchiveUpdateCallback::GetUpdateItemInfo(UInt32 /* index */, Int32 *newData, Int32 *newProperties, UInt32 *indexInArchive))
{
  if (newData) *newData = BoolToInt(true);
  if (newProperties) *newProperties = BoolToInt(true);
  if (indexInArchive) *indexInArchive = (UInt32)(Int32)-1;
  return S_OK;
}

Z7_COM7F_IMF(CArchiveUpdateCallback::GetProperty(UInt32 index, PROPID propID, PROPVARIANT *value))
{
  NCOM::CPropVariant prop;
  if (propID == kpidIsAnti)
  {
    prop = false;
    prop.Detach(value);
    return S_OK;
  }

  const CDirItem &di = (*DirItems)[index];
  switch (propID)
  {
    case kpidPath: prop = di.Path_For_Handler; break;
    case kpidIsDir: prop = di.IsDir(); break;
    case kpidSize: prop = di.Size; break;
    case kpidMTime: PropVariant_SetFrom_FiTime(prop, di.MTime); break;
    case kpidAttrib: prop = (UInt32)di.GetWinAttrib(); break;
    case kpidPosixAttrib: prop = (UInt32)di.GetPosixAttrib(); break;
  }
  prop.Detach(value);
  return S_OK;
}

Z7_COM7F_IMF(CArchiveUpdateCallback::GetStream(UInt32 index, ISequentialInStream **inStream))
{
  *inStream = NULL;
  const CDirItem &di = (*DirItems)[index];
  if (di.IsDir())
    return S_OK;

  CInFileStream *inStreamSpec = new CInFileStream;
  CMyComPtr<ISequentialInStream> inStreamLoc(inStreamSpec);
  if (!inStreamSpec->Open(di.FullPath))
  {
    LOGE("Cannot open input file for compression: '%s'", (const char *)di.FullPath);
    NumErrors++;
    return S_FALSE;
  }
  *inStream = inStreamLoc.Detach();
  return S_OK;
}

Z7_COM7F_IMF(CArchiveUpdateCallback::SetOperationResult(Int32 /* operationResult */)) { return S_OK; }

Z7_COM7F_IMF(CArchiveUpdateCallback::GetVolumeSize(UInt32 /* index */, UInt64 *size))
{
  if (SplitVolumeSize == 0)
    return S_FALSE;
  *size = SplitVolumeSize;
  return S_OK;
}

Z7_COM7F_IMF(CArchiveUpdateCallback::GetVolumeStream(UInt32 index, ISequentialOutStream **volumeStream))
{
  *volumeStream = NULL;
  wchar_t temp[32];
  ConvertUInt32ToString(index + 1, temp);
  UString volNum = temp;
  while (volNum.Len() < 3)
    volNum.InsertAtFront(L'0');

  UString volFileName = VolBaseName + L"." + volNum;
  if (!VolExt.IsEmpty())
    volFileName += VolExt;

  COutFileStream *streamSpec = new COutFileStream;
  CMyComPtr<ISequentialOutStream> streamLoc(streamSpec);
  if (!streamSpec->Create_NEW(us2fs(volFileName)))
    return GetLastError_noZero_HRESULT();

  *volumeStream = streamLoc.Detach();
  return S_OK;
}

Z7_COM7F_IMF(CArchiveUpdateCallback::CryptoGetTextPassword2(Int32 *passwordIsDefined, BSTR *password))
{
  *passwordIsDefined = BoolToInt(PasswordIsDefined);
  return StringToBstr(Password, password);
}

// -------------------------------------------------------------
// Native Archive Context (Handle for open archive)
// -------------------------------------------------------------
struct NativeArchiveContext
{
  CMyComPtr<IInArchive> Archive;
  CInFileStream *FileStreamSpec;
  CMyComPtr<IInStream> FileStream;
  UString Path;
  UString Password;
  int FormatIndex;
  UString FormatName;

  NativeArchiveContext() : FileStreamSpec(NULL), FormatIndex(-1) {}
  ~NativeArchiveContext()
  {
    if (Archive)
    {
      Archive->Close();
      Archive.Release();
    }
    FileStream.Release();
  }
};

// -------------------------------------------------------------
// JNI Implementation
// -------------------------------------------------------------
extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void * /* reserved */)
{
  g_JavaVM = vm;
  return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_com_ripple_filemanager_archive_sevenzip_Native7zBridge_nativeInit(
  JNIEnv *env, jclass /* clazz */, jstring lib7zPath)
{
  if (!g_JavaVM)
    env->GetJavaVM(&g_JavaVM);

  if (g_Initialized)
    return JNI_TRUE;

  UString libPathU = JStringToUString(env, lib7zPath);
  FString libPathF = us2fs(libPathU);

  if (!g_Lib7z.Load(libPathF))
  {
    LOGE("Failed to load lib7z.so from: %s", (const char *)libPathF);
    return JNI_FALSE;
  }

  g_CreateObject = Z7_GET_PROC_ADDRESS(Func_CreateObject, g_Lib7z.Get_HMODULE(), "CreateObject");
  g_GetNumberOfFormats = Z7_GET_PROC_ADDRESS(Func_GetNumberOfFormats, g_Lib7z.Get_HMODULE(), "GetNumberOfFormats");
  g_GetHandlerProperty2 = Z7_GET_PROC_ADDRESS(Func_GetHandlerProperty2, g_Lib7z.Get_HMODULE(), "GetHandlerProperty2");

  if (!g_CreateObject || !g_GetNumberOfFormats || !g_GetHandlerProperty2)
  {
    LOGE("Failed to locate required exports from lib7z.so");
    return JNI_FALSE;
  }

  UInt32 numFormats = 0;
  if (g_GetNumberOfFormats(&numFormats) != S_OK || numFormats == 0)
  {
    LOGE("GetNumberOfFormats returned error or 0");
    return JNI_FALSE;
  }

  g_Formats.Clear();
  for (UInt32 i = 0; i < numFormats; i++)
  {
    CFormatInfo fmt;
    fmt.Index = i;

    NCOM::CPropVariant prop;
    if (g_GetHandlerProperty2(i, NArchive::NHandlerPropID::kName, &prop) == S_OK && prop.vt == VT_BSTR)
      fmt.Name = prop.bstrVal;

    if (g_GetHandlerProperty2(i, NArchive::NHandlerPropID::kExtension, &prop) == S_OK && prop.vt == VT_BSTR)
      fmt.Ext = prop.bstrVal;

    if (g_GetHandlerProperty2(i, NArchive::NHandlerPropID::kClassID, &prop) == S_OK && prop.vt == VT_BSTR)
    {
      UINT len = ::SysStringByteLen(prop.bstrVal);
      if (len == sizeof(GUID))
        memcpy(&fmt.ClsId, prop.bstrVal, sizeof(GUID));
    }

    if (g_GetHandlerProperty2(i, NArchive::NHandlerPropID::kUpdate, &prop) == S_OK && prop.vt == VT_BOOL)
      fmt.Update = VARIANT_BOOLToBool(prop.boolVal);

    if (g_GetHandlerProperty2(i, NArchive::NHandlerPropID::kSignatureOffset, &prop) == S_OK && prop.vt == VT_UI4)
      fmt.SigOffset = prop.ulVal;

    if (g_GetHandlerProperty2(i, NArchive::NHandlerPropID::kSignature, &prop) == S_OK && prop.vt == VT_BSTR)
    {
      UINT len = ::SysStringByteLen(prop.bstrVal);
      fmt.Sig.CopyFrom((const Byte *)prop.bstrVal, len);
    }

    g_Formats.Add(fmt);
  }

  LOGI("Initialized Native 7z Bridge: %u archive formats registered.", (unsigned)g_Formats.Size());
  g_Initialized = true;
  return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL
Java_com_ripple_filemanager_archive_sevenzip_Native7zBridge_nativeSupportedFormats(
  JNIEnv *env, jclass /* clazz */)
{
  if (!g_Initialized)
    return NULL;

  jclass infoClass = env->FindClass("com/ripple/filemanager/archive/sevenzip/NativeFormatInfo");
  if (!infoClass)
    return NULL;

  jmethodID ctor = env->GetMethodID(infoClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;ZII)V");
  if (!ctor)
    return NULL;

  jobjectArray result = env->NewObjectArray((jsize)g_Formats.Size(), infoClass, NULL);
  for (unsigned i = 0; i < g_Formats.Size(); i++)
  {
    const CFormatInfo &f = g_Formats[i];
    jstring name = UStringToJString(env, f.Name);
    jstring ext = UStringToJString(env, f.Ext);
    jobject obj = env->NewObject(infoClass, ctor, name, ext, (jboolean)f.Update, (jint)f.Sig.Size(), (jint)f.SigOffset);

    env->SetObjectArrayElement(result, (jsize)i, obj);
    env->DeleteLocalRef(name);
    env->DeleteLocalRef(ext);
    env->DeleteLocalRef(obj);
  }

  return result;
}

JNIEXPORT jlong JNICALL
Java_com_ripple_filemanager_archive_sevenzip_Native7zBridge_nativeOpen(
  JNIEnv *env, jclass /* clazz */, jstring path, jstring password, jstring volumeHint)
{
  if (!g_Initialized || !path)
    return 0;

  UString arcPathU = JStringToUString(env, path);
  FString arcPathF = us2fs(arcPathU);
  UString pwdU = JStringToUString(env, password);

  CInFileStream *fileSpec = new CInFileStream;
  CMyComPtr<IInStream> fileStream = fileSpec;
  if (!fileSpec->Open(arcPathF))
  {
    LOGE("Cannot open file: '%s'", (const char *)arcPathF);
    return 0;
  }

  // Read header for signature identification
  Byte headerBuf[65536];
  UInt32 headerSize = 0;
  fileStream->Read(headerBuf, sizeof(headerBuf), &headerSize);
  fileStream->Seek(0, STREAM_SEEK_SET, NULL);

  // Extract extension
  AString arcPathA((const char *)arcPathF);
  int dotPos = arcPathA.ReverseFind_Dot();
  AString extA;
  if (dotPos >= 0)
    extA = arcPathA.Ptr(dotPos + 1);
  extA.MakeLower_Ascii();
  UString extU = GetUnicodeString(extA);

  // Candidate format search
  CRecordVector<UInt32> candidates;
  // 1. Signature match
  for (unsigned i = 0; i < g_Formats.Size(); i++)
  {
    const CFormatInfo &f = g_Formats[i];
    if (f.Sig.Size() > 0 && f.SigOffset + f.Sig.Size() <= headerSize)
    {
      if (memcmp(headerBuf + f.SigOffset, f.Sig, f.Sig.Size()) == 0)
        candidates.Add(i);
    }
  }
  // 2. Extension match
  for (unsigned i = 0; i < g_Formats.Size(); i++)
  {
    const CFormatInfo &f = g_Formats[i];
    if (candidates.FindInSorted(i) < 0 && !extU.IsEmpty() && !f.Ext.IsEmpty())
    {
      if (f.Ext.Find(extU) >= 0)
        candidates.Add(i);
    }
  }
  // 3. Fallback: all formats
  for (unsigned i = 0; i < g_Formats.Size(); i++)
  {
    if (candidates.FindInSorted(i) < 0)
      candidates.Add(i);
  }

  CMyComPtr<IInArchive> archive;
  int matchedFormatIndex = -1;

  CArchiveOpenCallback *openCallbackSpec = new CArchiveOpenCallback;
  CMyComPtr<IArchiveOpenCallback> openCallback(openCallbackSpec);
  openCallbackSpec->SetPassword(pwdU);

  // Derive folder prefix for multi-volume resolution
  int lastSlash = arcPathU.ReverseFind_PathSepar();
  if (lastSlash >= 0)
    openCallbackSpec->FolderPrefix = arcPathU.Left(lastSlash + 1);

  for (unsigned ci = 0; ci < candidates.Size(); ci++)
  {
    UInt32 fi = candidates[ci];
    const CFormatInfo &fmt = g_Formats[fi];

    CMyComPtr<IInArchive> testArchive;
    if (g_CreateObject(&fmt.ClsId, &IID_IInArchive, (void **)&testArchive) != S_OK || !testArchive)
      continue;

    fileStream->Seek(0, STREAM_SEEK_SET, NULL);
    const UInt64 scanSize = 1 << 23;
    HRESULT res = testArchive->Open(fileStream, &scanSize, openCallback);
    if (res == S_OK)
    {
      archive = testArchive;
      matchedFormatIndex = (int)fi;
      break;
    }
  }

  if (!archive || matchedFormatIndex < 0)
  {
    LOGW("Could not open file as supported archive: '%s'", (const char *)arcPathF);
    return 0;
  }

  NativeArchiveContext *ctx = new NativeArchiveContext();
  ctx->Archive = archive;
  ctx->FileStreamSpec = fileSpec;
  ctx->FileStream = fileStream;
  ctx->Path = arcPathU;
  ctx->Password = pwdU;
  ctx->FormatIndex = matchedFormatIndex;
  ctx->FormatName = g_Formats[matchedFormatIndex].Name;

  LOGI("Opened archive '%s' with format '%ls'", (const char *)arcPathF, (const wchar_t *)ctx->FormatName);
  return (jlong)(intptr_t)ctx;
}

JNIEXPORT jobjectArray JNICALL
Java_com_ripple_filemanager_archive_sevenzip_Native7zBridge_nativeList(
  JNIEnv *env, jclass /* clazz */, jlong handle)
{
  NativeArchiveContext *ctx = (NativeArchiveContext *)(intptr_t)handle;
  if (!ctx || !ctx->Archive)
    return NULL;

  UInt32 numItems = 0;
  if (ctx->Archive->GetNumberOfItems(&numItems) != S_OK)
    return NULL;

  jclass entryClass = env->FindClass("com/ripple/filemanager/archive/sevenzip/NativeArchiveEntry");
  if (!entryClass)
    return NULL;

  jmethodID ctor = env->GetMethodID(entryClass, "<init>", "(ILjava/lang/String;JJZJJZLjava/lang/String;)V");
  if (!ctor)
    return NULL;

  jobjectArray result = env->NewObjectArray((jsize)numItems, entryClass, NULL);

  for (UInt32 i = 0; i < numItems; i++)
  {
    // Path
    NCOM::CPropVariant propPath;
    ctx->Archive->GetProperty(i, kpidPath, &propPath);
    UString itemPath;
    if (propPath.vt == VT_BSTR)
      itemPath = propPath.bstrVal;
    else if (propPath.vt == VT_EMPTY)
      itemPath = L"[Content]";

    // Size
    NCOM::CPropVariant propSize;
    ctx->Archive->GetProperty(i, kpidSize, &propSize);
    UInt64 size = 0;
    ConvertPropVariantToUInt64(propSize, size);

    // PackSize
    NCOM::CPropVariant propPackSize;
    ctx->Archive->GetProperty(i, kpidPackSize, &propPackSize);
    UInt64 packSize = 0;
    ConvertPropVariantToUInt64(propPackSize, packSize);

    // IsDir
    NCOM::CPropVariant propIsDir;
    ctx->Archive->GetProperty(i, kpidIsDir, &propIsDir);
    bool isDir = (propIsDir.vt == VT_BOOL && VARIANT_BOOLToBool(propIsDir.boolVal));

    // CRC
    NCOM::CPropVariant propCrc;
    ctx->Archive->GetProperty(i, kpidCRC, &propCrc);
    UInt32 crc = 0;
    if (propCrc.vt == VT_UI4)
      crc = propCrc.ulVal;

    // MTime
    NCOM::CPropVariant propMTime;
    ctx->Archive->GetProperty(i, kpidMTime, &propMTime);
    jlong epochMillis = 0;
    if (propMTime.vt == VT_FILETIME)
    {
      const FILETIME &ft = propMTime.filetime;
      UInt64 ft64 = (((UInt64)ft.dwHighDateTime) << 32) | ft.dwLowDateTime;
      if (ft64 >= 116444736000000000ULL)
        epochMillis = (jlong)((ft64 - 116444736000000000ULL) / 10000ULL);
    }

    // Encrypted
    NCOM::CPropVariant propEncrypted;
    ctx->Archive->GetProperty(i, kpidEncrypted, &propEncrypted);
    bool isEncrypted = (propEncrypted.vt == VT_BOOL && VARIANT_BOOLToBool(propEncrypted.boolVal));

    // Method
    NCOM::CPropVariant propMethod;
    ctx->Archive->GetProperty(i, kpidMethod, &propMethod);
    UString methodStr;
    if (propMethod.vt == VT_BSTR)
      methodStr = propMethod.bstrVal;

    jstring jPath = UStringToJString(env, itemPath);
    jstring jMethod = UStringToJString(env, methodStr);

    jobject jEntry = env->NewObject(entryClass, ctor,
      (jint)i, jPath, (jlong)size, (jlong)packSize, (jboolean)isDir,
      (jlong)crc, epochMillis, (jboolean)isEncrypted, jMethod);

    env->SetObjectArrayElement(result, (jsize)i, jEntry);
    env->DeleteLocalRef(jPath);
    env->DeleteLocalRef(jMethod);
    env->DeleteLocalRef(jEntry);
  }

  return result;
}

JNIEXPORT jboolean JNICALL
Java_com_ripple_filemanager_archive_sevenzip_Native7zBridge_nativeExtract(
  JNIEnv *env, jclass /* clazz */, jlong handle, jintArray indices,
  jstring destDir, jstring password, jobject callback)
{
  NativeArchiveContext *ctx = (NativeArchiveContext *)(intptr_t)handle;
  if (!ctx || !ctx->Archive || !destDir)
    return JNI_FALSE;

  UString destDirU = JStringToUString(env, destDir);
  UString pwdU = JStringToUString(env, password);
  if (pwdU.IsEmpty())
    pwdU = ctx->Password;

  CreateComplexDir(us2fs(destDirU));

  CArchiveExtractCallback *extractCallbackSpec = new CArchiveExtractCallback;
  CMyComPtr<IArchiveExtractCallback> extractCallback(extractCallbackSpec);
  extractCallbackSpec->Init(ctx->Archive, us2fs(destDirU), env, callback, pwdU);

  UInt32 *indicesPtr = NULL;
  UInt32 numIndices = (UInt32)(Int32)-1;
  jsize jIndicesLen = 0;

  if (indices)
  {
    jIndicesLen = env->GetArrayLength(indices);
    if (jIndicesLen > 0)
    {
      indicesPtr = new UInt32[jIndicesLen];
      jint *body = env->GetIntArrayElements(indices, NULL);
      for (int i = 0; i < jIndicesLen; i++)
        indicesPtr[i] = (UInt32)body[i];
      env->ReleaseIntArrayElements(indices, body, JNI_ABORT);
      numIndices = (UInt32)jIndicesLen;
    }
  }

  HRESULT res = ctx->Archive->Extract(indicesPtr, numIndices, false, extractCallback);
  if (indicesPtr)
    delete[] indicesPtr;

  if (res != S_OK || extractCallbackSpec->NumErrors > 0)
  {
    LOGE("Extract returned error 0x%08X (errors: %llu)", (unsigned)res, (unsigned long long)extractCallbackSpec->NumErrors);
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_ripple_filemanager_archive_sevenzip_Native7zBridge_nativeTest(
  JNIEnv *env, jclass /* clazz */, jlong handle, jintArray indices,
  jstring password, jobject callback)
{
  NativeArchiveContext *ctx = (NativeArchiveContext *)(intptr_t)handle;
  if (!ctx || !ctx->Archive)
    return JNI_FALSE;

  UString pwdU = JStringToUString(env, password);
  if (pwdU.IsEmpty())
    pwdU = ctx->Password;

  CArchiveExtractCallback *testCallbackSpec = new CArchiveExtractCallback;
  CMyComPtr<IArchiveExtractCallback> testCallback(testCallbackSpec);
  testCallbackSpec->Init(ctx->Archive, FString(), env, callback, pwdU);

  UInt32 *indicesPtr = NULL;
  UInt32 numIndices = (UInt32)(Int32)-1;
  if (indices)
  {
    jsize len = env->GetArrayLength(indices);
    if (len > 0)
    {
      indicesPtr = new UInt32[len];
      jint *body = env->GetIntArrayElements(indices, NULL);
      for (int i = 0; i < len; i++)
        indicesPtr[i] = (UInt32)body[i];
      env->ReleaseIntArrayElements(indices, body, JNI_ABORT);
      numIndices = (UInt32)len;
    }
  }

  HRESULT res = ctx->Archive->Extract(indicesPtr, numIndices, true, testCallback);
  if (indicesPtr)
    delete[] indicesPtr;

  return (res == S_OK && testCallbackSpec->NumErrors == 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_ripple_filemanager_archive_sevenzip_Native7zBridge_nativeCreate(
  JNIEnv *env, jclass /* clazz */, jobjectArray inputPaths, jstring outPath,
  jstring format, jint level, jstring password, jboolean encryptNames,
  jlong splitSize, jobject callback)
{
  if (!g_Initialized || !inputPaths || !outPath || !format)
    return JNI_FALSE;

  UString formatU = JStringToUString(env, format);

  // STRICT COMPLIANCE: RAR creation is forbidden by license
  if (formatU.IsEqualTo_Ascii_NoCase("rar") || formatU.IsEqualTo_Ascii_NoCase("rar5"))
  {
    LOGE("RAR creation is strictly forbidden by license.");
    return JNI_FALSE;
  }

  int formatIndex = -1;
  for (unsigned i = 0; i < g_Formats.Size(); i++)
  {
    if (StringsAreEqualNoCase(g_Formats[i].Name, formatU) ||
        g_Formats[i].Ext.Find(formatU) >= 0)
    {
      formatIndex = (int)i;
      break;
    }
  }

  if (formatIndex < 0 || !g_Formats[formatIndex].Update)
  {
    LOGE("Archive creation format '%ls' is unsupported or read-only", (const wchar_t *)formatU);
    return JNI_FALSE;
  }

  const CFormatInfo &fmt = g_Formats[formatIndex];
  CMyComPtr<IOutArchive> outArchive;
  if (g_CreateObject(&fmt.ClsId, &IID_IOutArchive, (void **)&outArchive) != S_OK || !outArchive)
  {
    LOGE("Cannot instantiate IOutArchive for format '%ls'", (const wchar_t *)formatU);
    return JNI_FALSE;
  }

  jsize numInputs = env->GetArrayLength(inputPaths);
  UString outPathU = JStringToUString(env, outPath);
  FString outPathF = us2fs(outPathU);
  LOGI("nativeCreate: format='%ls', outPath='%s', numInputs=%d", (const wchar_t *)formatU, (const char *)outPathF, (int)numInputs);

  // Configure compression options
  CMyComPtr<ISetProperties> setProperties;
  outArchive.QueryInterface(IID_ISetProperties, (void **)&setProperties);
  if (setProperties)
  {
    CObjectVector<UString> names;
    CRecordVector<NCOM::CPropVariant> values;

    // Compression level
    names.Add(L"x");
    NCOM::CPropVariant propLevel = (UInt32)level;
    values.Add(propLevel);

    // Encrypt file names (headers) for 7z
    if (encryptNames && formatU.IsEqualTo_Ascii_NoCase("7z"))
    {
      names.Add(L"he");
      NCOM::CPropVariant propHe = true;
      values.Add(propHe);
    }

    // UTF-8 code page for zip entries
    if (formatU.IsEqualTo_Ascii_NoCase("zip"))
    {
      names.Add(L"cu");
      NCOM::CPropVariant propCu = true;
      values.Add(propCu);
    }

    CRecordVector<const wchar_t *> namePtrs;
    for (unsigned i = 0; i < names.Size(); i++)
      namePtrs.Add((const wchar_t *)names[i]);

    setProperties->SetProperties(namePtrs.ConstData(), values.ConstData(), names.Size());
  }

  // Enumerate input files
  CObjectVector<CDirItem> dirItems;
  UInt64 totalInputBytes = 0;

  for (int i = 0; i < numInputs; i++)
  {
    jstring jPath = (jstring)env->GetObjectArrayElement(inputPaths, i);
    UString inPathU = JStringToUString(env, jPath);
    env->DeleteLocalRef(jPath);

    FString inPathF = us2fs(inPathU);
    NFind::CFileInfo fi;
    if (!fi.Find(inPathF))
      continue;

    CDirItem di(fi);
    // Relative item name inside archive
    int lastSlash = inPathU.ReverseFind_PathSepar();
    di.Path_For_Handler = (lastSlash >= 0) ? UString(inPathU.Ptr(lastSlash + 1)) : inPathU;
    di.FullPath = inPathF;
    dirItems.Add(di);
    totalInputBytes += fi.Size;
  }

  if (dirItems.IsEmpty())
  {
    LOGE("No valid input files found for compression.");
    return JNI_FALSE;
  }

  COutFileStream *outFileStreamSpec = new COutFileStream;
  CMyComPtr<IOutStream> outFileStream = outFileStreamSpec;
  if (!outFileStreamSpec->Create_ALWAYS(outPathF))
  {
    LOGE("Cannot create archive file '%s'", (const char *)outPathF);
    return JNI_FALSE;
  }

  CArchiveUpdateCallback *updateCallbackSpec = new CArchiveUpdateCallback;
  CMyComPtr<IArchiveUpdateCallback2> updateCallback(updateCallbackSpec);
  updateCallbackSpec->DirItems = &dirItems;
  updateCallbackSpec->TotalBytes = totalInputBytes;
  updateCallbackSpec->SplitVolumeSize = (UInt64)splitSize;
  UString pwdU = JStringToUString(env, password);
  updateCallbackSpec->Init(env, callback, pwdU);

  HRESULT res = outArchive->UpdateItems(outFileStream, dirItems.Size(), updateCallback);
  outFileStreamSpec->Close();

  if (res != S_OK || updateCallbackSpec->NumErrors > 0)
  {
    LOGE("UpdateItems failed with error 0x%08X (errors: %llu) for format '%ls'", (unsigned)res, (unsigned long long)updateCallbackSpec->NumErrors, (const wchar_t *)formatU);
    DeleteFileAlways(outPathF);
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_ripple_filemanager_archive_sevenzip_Native7zBridge_nativeClose(
  JNIEnv * /* env */, jclass /* clazz */, jlong handle)
{
  NativeArchiveContext *ctx = (NativeArchiveContext *)(intptr_t)handle;
  if (ctx)
  {
    delete ctx;
  }
}

} // extern "C"
