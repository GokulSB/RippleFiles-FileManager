package com.ripple.filemanager.archive

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    private val _archiveProgress = MutableStateFlow<ArchiveProgress?>(null)
    val archiveProgress: StateFlow<ArchiveProgress?> = _archiveProgress.asStateFlow()

    private var currentSource: Uri? = null
    private var currentDest: Uri? = null

    init {
        viewModelScope.launch {
            workManager.getWorkInfosByTagLiveData("archive_extract").observeForever { workInfos ->
                if (workInfos.isNullOrEmpty()) return@observeForever
                
                val workInfo = workInfos.firstOrNull { !it.state.isFinished || it.state == WorkInfo.State.SUCCEEDED || it.state == WorkInfo.State.FAILED }
                if (workInfo != null) {
                    val status = workInfo.progress.getString("status") ?: 
                                (if (workInfo.state == WorkInfo.State.SUCCEEDED) "complete" else if (workInfo.state == WorkInfo.State.FAILED) "failed" else null)
                    
                    when (status) {
                        "needs_password" -> {
                            val attemptFailed = workInfo.progress.getBoolean("attemptFailed", false)
                            _archiveProgress.value = ArchiveProgress.NeedsPassword(attemptFailed)
                        }
                        "failed" -> {
                            val error = workInfo.progress.getString("error") ?: workInfo.outputData.getString("error") ?: "Extraction failed"
                            _archiveProgress.value = ArchiveProgress.Failed(error)
                        }
                        "complete" -> {
                            val outputDir = workInfo.progress.getString("outputDir") ?: workInfo.outputData.getString("outputDir") ?: ""
                            _archiveProgress.value = ArchiveProgress.Complete(outputDir)
                        }
                        else -> {
                            if (workInfo.state == WorkInfo.State.RUNNING) {
                                val filesDone = workInfo.progress.getInt("filesDone", 0)
                                val filesTotal = workInfo.progress.getInt("filesTotal", 0)
                                val currentEntryName = workInfo.progress.getString("currentEntryName") ?: ""
                                _archiveProgress.value = ArchiveProgress.Running(filesDone, filesTotal, currentEntryName)
                            }
                        }
                    }
                }
            }
        }
    }

    fun extract(source: Uri, dest: Uri) {
        currentSource = source
        currentDest = dest
        _archiveProgress.value = null
        
        viewModelScope.launch {
            try {
                val handler = ArchiveHandlerFactory.create(getApplication(), source)
                val entries = handler.listEntries(source)
                
                if (entries.isNotEmpty() && entries.first().isEncrypted) {
                    _archiveProgress.value = ArchiveProgress.NeedsPassword(attemptFailed = false)
                    return@launch
                }
                
                enqueueWorker(source, dest, null)
            } catch (e: Exception) {
                _archiveProgress.value = ArchiveProgress.Failed(e.message ?: "Failed to prepare extraction")
            }
        }
    }

    fun submitPassword(password: String) {
        val source = currentSource ?: return
        val dest = currentDest ?: return

        viewModelScope.launch {
            try {
                val handler = ArchiveHandlerFactory.create(getApplication(), source)
                val isValid = handler.testPassword(source, password)
                if (isValid) {
                    enqueueWorker(source, dest, password)
                } else {
                    _archiveProgress.value = ArchiveProgress.NeedsPassword(attemptFailed = true)
                }
            } catch (e: Exception) {
                _archiveProgress.value = ArchiveProgress.Failed(e.message ?: "Failed to verify password")
            }
        }
    }

    private var _currentArchiveName = MutableStateFlow<String?>(null)
    val currentArchiveName: StateFlow<String?> = _currentArchiveName.asStateFlow()

    fun resetState() {
        _archiveProgress.value = null
        _currentArchiveName.value = null
    }

    fun cancelExtraction() {
        currentSource?.let {
            workManager.cancelUniqueWork(it.toString())
        }
        _archiveProgress.value = null
    }

    /**
     * Detects whether the source Uri points to a RAR archive using magic bytes.
     * Returns true for RAR, false for ZIP or unsupported.
     */
    suspend fun detectIsRar(source: Uri): Boolean {
        return try {
            val handler = ArchiveHandlerFactory.create(getApplication(), source)
            handler is RarArchiveHandler
        } catch (e: Exception) {
            false
        }
    }

    private fun enqueueWorker(source: Uri, dest: Uri, password: String?) {
        val inputData = Data.Builder()
            .putString("source", source.toString())
            .putString("dest", dest.toString())
            .putString("password", password)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ArchiveExtractWorker>()
            .setInputData(inputData)
            .addTag("archive_extract")
            .build()

        // ExistingWorkPolicy.KEEP prevents duplicate runs
        workManager.enqueueUniqueWork(
            source.toString(),
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}
