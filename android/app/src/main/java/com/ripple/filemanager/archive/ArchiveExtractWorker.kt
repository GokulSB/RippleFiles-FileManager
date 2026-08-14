package com.ripple.filemanager.archive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import com.ripple.filemanager.R
import com.ripple.filemanager.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class ArchiveExtractWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationId = 1002
    private val channelId = "archive_extract_channel"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Archive Extraction",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sourceStr = inputData.getString("source") ?: return@withContext Result.failure()
        val destStr = inputData.getString("dest") ?: return@withContext Result.failure()
        val password = inputData.getString("password")
        val archiveName = inputData.getString("archiveName") ?: "Archive"

        val sourceUri = Uri.parse(sourceStr)
        val destUri = Uri.parse(destStr)

        val isSaf = destUri.scheme == "content"

        val handler = try {
            ArchiveHandlerFactory.create(context, sourceUri)
        } catch (e: Exception) {
            showErrorNotification(archiveName, e.message ?: "Unsupported format")
            return@withContext Result.failure(workDataOf("error" to (e.message ?: "Unsupported format")))
        }

        // Branch once at the top of the worker for SAF vs Local, 
        // to handle the actual writing if we were doing the loop here.
        // But since the interface delegates to handler.extract(), we rely on the handler
        // to do the writing, or we use a temp dir if it's SAF and then copy.
        // Given the prompt constraints, we will extract to a local temp dir if it's SAF,
        // then copy to SAF. This avoids "special-case per entry" in the handlers.
        val targetDestDir = if (isSaf) {
            Uri.fromFile(File(context.cacheDir, "extract_${System.currentTimeMillis()}"))
        } else {
            destUri
        }

        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(id)

        try {
            var finalResult: Result = Result.success()

            handler.extract(sourceUri, targetDestDir, password).collect { progress ->
                when (progress) {
                    is ArchiveProgress.Running -> {
                        val percent = if (progress.filesTotal > 0) {
                            (progress.filesDone * 100) / progress.filesTotal
                        } else {
                            0
                        }
                        updateNotification(archiveName, percent, progress.filesDone, progress.filesTotal, cancelIntent)
                        
                        setProgress(
                            workDataOf(
                                "filesDone" to progress.filesDone,
                                "filesTotal" to progress.filesTotal,
                                "currentEntryName" to progress.currentEntryName
                            )
                        )
                    }
                    is ArchiveProgress.Complete -> {
                        var outputPath = progress.outputDir
                        if (isSaf) {
                            // Copy from temp local dir to SAF dest
                            val tempDir = File(progress.outputDir)
                            val docTree = DocumentFile.fromTreeUri(context, destUri)
                            if (docTree != null) {
                                copyToSaf(tempDir, docTree)
                            }
                            tempDir.deleteRecursively()
                            outputPath = destUri.toString()
                        }
                        
                        showCompletionNotification(archiveName, outputPath)
                        setProgress(workDataOf("status" to "complete", "outputDir" to outputPath))
                        finalResult = Result.success(workDataOf("outputDir" to outputPath))
                    }
                    is ArchiveProgress.NeedsPassword -> {
                        setProgress(workDataOf("status" to "needs_password", "attemptFailed" to progress.attemptFailed))
                        finalResult = Result.failure(workDataOf("error" to "needs_password", "attemptFailed" to progress.attemptFailed))
                    }
                    is ArchiveProgress.Failed -> {
                        showErrorNotification(archiveName, progress.reason)
                        setProgress(workDataOf("status" to "failed", "error" to progress.reason))
                        finalResult = Result.failure(workDataOf("error" to progress.reason))
                    }
                }
            }

            finalResult
        } catch (e: Exception) {
            showErrorNotification(archiveName, e.message ?: "Unknown error")
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        } finally {
            // Clean up partially written entries if cancelled
            if (isStopped) {
                if (isSaf) {
                    File(targetDestDir.path!!).deleteRecursively()
                } else {
                    // Try to clean up local dir
                    File(destUri.path!!).deleteRecursively()
                }
            }
        }
    }

    private fun copyToSaf(sourceFile: File, destDoc: DocumentFile) {
        if (sourceFile.isDirectory) {
            val dir = destDoc.createDirectory(sourceFile.name) ?: return
            sourceFile.listFiles()?.forEach { child ->
                copyToSaf(child, dir)
            }
        } else {
            val fileDoc = destDoc.createFile("*/*", sourceFile.name) ?: return
            context.contentResolver.openOutputStream(fileDoc.uri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
        }
    }

    private fun createForegroundInfo(
        title: String,
        progress: Int,
        max: Int,
        cancelIntent: PendingIntent
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText("Extracting... $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    private fun updateNotification(
        title: String,
        progress: Int,
        filesDone: Int,
        filesTotal: Int,
        cancelIntent: PendingIntent
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText("Extracting... $filesDone / $filesTotal files")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun showCompletionNotification(title: String, outputDir: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = Intent.ACTION_VIEW
            data = Uri.parse(outputDir)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText("Extraction complete")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "View", pendingIntent)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(notificationId + 1, notification)
    }

    private fun showErrorNotification(title: String, error: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText("Extraction failed: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 2, notification)
    }
}
