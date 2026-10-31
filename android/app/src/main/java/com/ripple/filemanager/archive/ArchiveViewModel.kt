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
    private var autoClearJob: kotlinx.coroutines.Job? = null
    private val activeWorkIds = java.util.Collections.synchronizedSet(mutableSetOf<java.util.UUID>())

    init {
        // Prune finished work from previous sessions on fresh launch
        workManager.pruneWork()

        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("archive_extract").collect { workInfos ->
                if (workInfos.isEmpty()) return@collect
                
                // Track any work that is currently active (running or enqueued)
                workInfos.filter { !it.state.isFinished }.forEach { workInfo ->
                    activeWorkIds.add(workInfo.id)
                }

                // Only consider work that is currently unfinished or was active in this session
                val currentWork = workInfos.firstOrNull { !it.state.isFinished }
                    ?: workInfos.firstOrNull { it.id in activeWorkIds && (it.state == WorkInfo.State.SUCCEEDED || it.state == WorkInfo.State.FAILED) }

                if (currentWork == null) {
                    // Stale completed work from previous session — do NOT show stale pill on cold start
                    return@collect
                }

                when (currentWork.state) {
                    WorkInfo.State.RUNNING -> {
                        autoClearJob?.cancel()
                        val status = currentWork.progress.getString("status")
                        when (status) {
                            "needs_password" -> {
                                val attemptFailed = currentWork.progress.getBoolean("attemptFailed", false)
                                _archiveProgress.value = ArchiveProgress.NeedsPassword(attemptFailed)
                            }
                            "failed" -> {
                                val error = currentWork.progress.getString("error") ?: "Extraction failed"
                                _archiveProgress.value = ArchiveProgress.Failed(error)
                            }
                            else -> {
                                val filesDone = currentWork.progress.getInt("filesDone", 0)
                                val filesTotal = currentWork.progress.getInt("filesTotal", 0)
                                val currentEntryName = currentWork.progress.getString("currentEntryName") ?: ""
                                _archiveProgress.value = ArchiveProgress.Running(filesDone, filesTotal, currentEntryName)
                            }
                        }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val outputDir = currentWork.outputData.getString("outputDir")
                            ?: currentWork.progress.getString("outputDir") ?: ""
                        _archiveProgress.value = ArchiveProgress.Complete(outputDir)
                        scheduleAutoClear(currentWork.id)
                    }
                    WorkInfo.State.FAILED -> {
                        val error = currentWork.outputData.getString("error")
                            ?: currentWork.progress.getString("error") ?: "Extraction failed"
                        val isNeedsPassword = error == "needs_password"
                        if (isNeedsPassword) {
                            val attemptFailed = currentWork.outputData.getBoolean("attemptFailed", false)
                            _archiveProgress.value = ArchiveProgress.NeedsPassword(attemptFailed)
                        } else {
                            _archiveProgress.value = ArchiveProgress.Failed(error)
                        }
                        activeWorkIds.remove(currentWork.id)
                    }
                    WorkInfo.State.CANCELLED -> {
                        autoClearJob?.cancel()
                        _archiveProgress.value = null
                        activeWorkIds.remove(currentWork.id)
                    }
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                        // In queue, keep waiting
                    }
                }
            }
        }
    }

    private fun scheduleAutoClear(workId: java.util.UUID) {
        autoClearJob?.cancel()
        autoClearJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            _archiveProgress.value = null
            activeWorkIds.remove(workId)
            workManager.pruneWork()
        }
    }

    fun extract(source: Uri, dest: Uri, initialPassword: String? = null) {
        currentSource = source
        currentDest = dest
        _archiveProgress.value = null
        
        viewModelScope.launch {
            try {
                if (initialPassword != null) {
                    enqueueWorker(source, dest, initialPassword)
                    return@launch
                }

                val handler = ArchiveHandlerFactory.create(getApplication(), source)
                val isEncrypted = handler.isEncrypted(source)
                if (isEncrypted) {
                    _archiveProgress.value = ArchiveProgress.NeedsPassword(attemptFailed = false)
                    return@launch
                }

                val entries = handler.listEntries(source)
                if (entries.any { it.isEncrypted }) {
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
        autoClearJob?.cancel()
        activeWorkIds.clear()
        _archiveProgress.value = null
        _currentArchiveName.value = null
        workManager.pruneWork()
    }

    fun cancelExtraction() {
        autoClearJob?.cancel()
        currentSource?.let {
            workManager.cancelUniqueWork(it.toString())
        }
        activeWorkIds.clear()
        _archiveProgress.value = null
        workManager.pruneWork()
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

        activeWorkIds.add(workRequest.id)

        // ExistingWorkPolicy.REPLACE ensures a new extraction can run even if previous finished
        workManager.enqueueUniqueWork(
            source.toString(),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
