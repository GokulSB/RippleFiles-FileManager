package com.ripple.filemanager.archive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.ripple.filemanager.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ArchiveCreateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationId = 1003
    private val channelId = "archive_create_channel"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Archive Creation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of archive compression jobs"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val inputPaths = inputData.getStringArray("inputPaths") ?: return@withContext Result.failure()
        val outPath = inputData.getString("outPath") ?: return@withContext Result.failure()
        val formatStr = inputData.getString("format") ?: "zip"
        val level = inputData.getInt("level", 5)
        val password = inputData.getString("password")
        val encryptNames = inputData.getBoolean("encryptNames", false)
        val splitSize = inputData.getLong("splitSize", 0L)
        val charset = inputData.getString("charset") ?: "UTF-8"
        val deleteAfterArchive = inputData.getBoolean("deleteAfterArchive", false)
        val archiveDisplayName = inputData.getString("displayName") ?: File(outPath).name

        val sourceFiles = inputPaths.map { File(it) }.filter { it.exists() }
        if (sourceFiles.isEmpty()) {
            return@withContext Result.failure(workDataOf("error" to "No valid input files found"))
        }

        val destFile = File(outPath)
        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)

        // Set initial foreground info so long-running job survives app leaving
        try {
            setForeground(createForegroundInfo(archiveDisplayName, 0, cancelIntent))
        } catch (e: Exception) {
            // Best effort if work already started
        }

        val format = ArchiveFormat.values().firstOrNull {
            it.extension.equals(formatStr, ignoreCase = true)
        } ?: ArchiveFormat.ZIP

        val compressionMethod = CompressionMethodType.values().firstOrNull {
            it.levelNumber == level
        } ?: CompressionMethodType.NORMAL

        val splitOption = SplitVolumeOption.values().firstOrNull {
            it.sizeBytes == splitSize
        } ?: SplitVolumeOption.NO_SPLIT

        val config = ArchiveCreateConfig(
            name = destFile.name,
            format = format,
            compressionMethod = compressionMethod,
            deleteAfterArchive = deleteAfterArchive,
            password = password,
            encryptFileNames = encryptNames,
            splitVolume = splitOption,
            charset = charset
        )

        try {
            ArchiveCreateManager.createArchive(
                sourceFiles = sourceFiles,
                destinationFile = destFile,
                config = config,
                onProgress = { percent, currentFile ->
                    if (isStopped) {
                        destFile.delete()
                        return@createArchive
                    }
                    updateNotification(archiveDisplayName, percent, currentFile, cancelIntent)
                    kotlinx.coroutines.runBlocking {
                        setProgress(
                            workDataOf(
                                "progressPercent" to percent,
                                "currentFile" to currentFile
                            )
                        )
                    }
                },
                isCancelled = { isStopped },
                context = context
            )

            if (isStopped) {
                if (destFile.exists()) destFile.delete()
                return@withContext Result.failure(workDataOf("error" to "Compression was cancelled"))
            }

            showCompletionNotification(archiveDisplayName, destFile.absolutePath)
            Result.success(workDataOf("outPath" to destFile.absolutePath))
        } catch (e: Exception) {
            if (destFile.exists()) {
                destFile.delete()
            }
            showErrorNotification(archiveDisplayName, e.message ?: "Compression failed")
            Result.failure(workDataOf("error" to (e.message ?: "Compression failed")))
        } finally {
            if (isStopped && destFile.exists()) {
                destFile.delete()
            }
        }
    }

    private fun createForegroundInfo(
        title: String,
        progress: Int,
        cancelIntent: PendingIntent
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Compressing $title")
            .setContentText("Compressing... $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    private fun updateNotification(
        title: String,
        progress: Int,
        currentFile: String,
        cancelIntent: PendingIntent
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Compressing $title")
            .setContentText("$progress% - $currentFile")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun showCompletionNotification(title: String, outputPath: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Compression complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 1, notification)
    }

    private fun showErrorNotification(title: String, error: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Compression failed")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 2, notification)
    }
}
