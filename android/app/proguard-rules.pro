# ============================================================================
# Ripple Files ProGuard & R8 Optimization Rules
# ============================================================================

# R8 Diagnostic Output
-printusage build/outputs/mapping/release/usage.txt
-printseeds build/outputs/mapping/release/seeds.txt
-printconfiguration build/outputs/mapping/release/full-config.txt

# General attributes preservation for reflection, line numbers and stack traces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Preserve native JNI methods across all packages
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve Enum values and valueOf methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve Parcelable Creator
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Suppress standard JVM/JDK warnings not present on Android runtime
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn javax.xml.**
-dontwarn javax.net.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn org.ietf.jgss.**
-dontwarn sun.misc.**
-dontwarn sun.security.**
-dontwarn java.lang.management.**

# ============================================================================
# App Models & Data Classes (Serialized / Deserialized via Gson & Reflection)
# ============================================================================
-keepclassmembers class com.ripple.filemanager.data.** {
    <fields>;
}
-keep class com.ripple.filemanager.localsend.*Dto { *; }
-keepclassmembers class com.ripple.filemanager.localsend.** {
    <fields>;
}
-keep class fi.iki.elonen.** { *; }
-keepclassmembers class com.ripple.filemanager.FileItem {
    <fields>;
}
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-dontwarn com.google.gson.**

# ============================================================================
# Services, BroadcastReceivers & WorkManager Workers
# ============================================================================
-keep class com.ripple.filemanager.** extends android.app.Service
-keep class com.ripple.filemanager.** extends android.content.BroadcastReceiver
-keep class com.ripple.filemanager.** extends androidx.work.ListenableWorker
-keep class com.ripple.filemanager.** extends androidx.work.Worker
-keep class com.ripple.filemanager.archive.ArchiveExtractWorker { *; }

# ============================================================================
# Cryptography & BouncyCastle (TLS, KeyStore, Certificates)
# ============================================================================
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn androidx.security.crypto.**

# ============================================================================
# LocalSend & Networking (NanoHTTPD, OkHttp3, Okio)
# ============================================================================
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ============================================================================
# SMB, SFTP & FTP Network Storage Providers
# ============================================================================
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**
-keep class net.schmizz.sshj.** { *; }
-keep class com.hierynomus.sshj.** { *; }
-dontwarn net.schmizz.sshj.**
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**
-keep class com.thegrizzlylabs.sardineandroid.** { *; }
-dontwarn com.thegrizzlylabs.sardineandroid.**
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**

# ============================================================================
# Cloud Providers (Dropbox & Google Drive)
# ============================================================================
-keep class com.dropbox.core.** { *; }
-dontwarn com.dropbox.core.**
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.drive.**
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.auth.api.identity.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keepclassmembers class com.google.android.gms.auth.api.identity.AuthorizationResult {
    public *;
}

# ============================================================================
# MEGA SDK JNI Bindings (SWIG generated)
# ============================================================================
-keepclasseswithmembernames class nz.mega.sdk.** {
    native <methods>;
}
-keepclassmembers class nz.mega.sdk.** {
    long swigCPtr;
    boolean swigCMemOwn;
    <init>(long, boolean);
}
-keep class nz.mega.sdk.megaJNI { *; }
-keep class nz.mega.sdk.MegaApi { *; }
-keep class nz.mega.sdk.MegaApiLock { *; }
-keep class nz.mega.sdk.*Listener* { *; }
-keep class nz.mega.sdk.MegaNode { *; }
-keep class nz.mega.sdk.MegaNodeList { *; }
-keep class nz.mega.sdk.MegaError { *; }
-keep class nz.mega.sdk.MegaRequest { *; }
-keep class nz.mega.sdk.MegaTransfer { *; }
-keep class nz.mega.sdk.MegaUser { *; }
-keep class nz.mega.sdk.MegaAccountDetails { *; }
-keep class nz.mega.sdk.MegaClient { *; }
-keep class nz.mega.sdk.MegaFileSource { *; }
-dontwarn nz.mega.sdk.**

# ============================================================================
# Archive Handling (Unrar5j & Zip4j)
# ============================================================================
-dontwarn be.stef.rar.**
-dontwarn net.lingala.zip4j.**

# ============================================================================
# Shizuku & Root Providers
# ============================================================================
-keep class moe.shizuku.** implements android.os.IInterface { *; }
-keep class dev.rikka.shizuku.** implements android.os.IInterface { *; }
-keep class moe.shizuku.api.ShizukuApiConstants { *; }
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class com.ripple.filemanager.shizuku.** { *; }
-keep interface com.ripple.filemanager.shizuku.** { *; }
-dontwarn moe.shizuku.**
-dontwarn dev.rikka.shizuku.**
-dontwarn com.github.topjohnwu.libsu.**

# ============================================================================
# Media, Documents & Images (ExoPlayer, Coil, PDFBox, POI)
# ============================================================================
-dontwarn androidx.media3.**
-dontwarn coil.**
-dontwarn org.apache.poi.**
-dontwarn com.tom_roush.pdfbox.**
-keep class * implements coil.fetch.Fetcher { *; }
-keep class * implements coil.fetch.Fetcher$Factory { *; }
-keep class * implements coil.decode.Decoder { *; }
-keep class * implements coil.decode.Decoder$Factory { *; }
-keep class com.ripple.filemanager.ui.DocumentThumbnailFetcher** { *; }
-keep class coil.decode.VideoFrameDecoder** { *; }

# ============================================================================
# Coroutines & Compose Runtime
# ============================================================================
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.compose.**

# ============================================================================
# Missing Optional Third-Party Classes (POI, Saxon, Log4j, Swing, OSGi)
# ============================================================================
-dontwarn javax.el.**
-dontwarn javax.swing.**
-dontwarn net.sf.saxon.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.logging.log4j.**
-dontwarn net.engio.mbassy.**
