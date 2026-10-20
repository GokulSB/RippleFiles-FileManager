package com.ripple.filemanager.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.File
import java.util.zip.ZipFile
import android.graphics.drawable.BitmapDrawable

class DocumentThumbnailFetcher(
    private val file: File,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val ext = file.extension.lowercase()
        val bitmap = when (ext) {
            "pdf" -> generatePdfThumbnail()
            "docx", "pptx", "xlsx" -> generateDocxThumbnail()
            "apk" -> generateApkThumbnail()
            "txt", "log", "md", "json", "xml", "kt", "java", "py", "html", "css", "js" -> generateTextThumbnail()
            else -> null
        }
        
        return if (bitmap != null) {
            DrawableResult(
                drawable = BitmapDrawable(options.context.resources, bitmap),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } else {
            null
        }
    }

    private fun generatePdfThumbnail(): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                page = renderer.openPage(0)
                val width = 400
                val height = (width.toFloat() / page.width * page.height).toInt().coerceAtMost(600)
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try { page?.close() } catch (e: Exception) {}
            try { renderer?.close() } catch (e: Exception) {}
            try { pfd?.close() } catch (e: Exception) {}
        }
    }

    private fun generateTextThumbnail(): Bitmap? {
        return try {
            val width = 400
            val height = 500
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                isAntiAlias = true
            }
            
            val reader = file.bufferedReader()
            var y = 40f
            var linesCount = 0
            while (y < height - 40f && linesCount < 20) {
                val line = reader.readLine() ?: break
                canvas.drawText(line.take(50), 20f, y, paint)
                y += paint.textSize + 10f
                linesCount++
            }
            reader.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun generateDocxThumbnail(): Bitmap? {
        return try {
            var bitmap: Bitmap? = null
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("docProps/thumbnail.jpeg")
                    ?: zip.getEntry("docProps/thumbnail.png")
                    ?: zip.getEntry("docProps/thumbnail.wmf")
                if (entry != null) {
                    zip.getInputStream(entry).use { input ->
                        bitmap = BitmapFactory.decodeStream(input)
                    }
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun generateApkThumbnail(): Bitmap? {
        return try {
            val pm = options.context.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return null
            val appInfo = info.applicationInfo ?: return null
            appInfo.sourceDir = file.absolutePath
            appInfo.publicSourceDir = file.absolutePath
            val drawable = appInfo.loadIcon(pm) ?: return null
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        val SUPPORTED_EXTENSIONS = setOf(
            "pdf", "docx", "pptx", "xlsx", "apk",
            "txt", "log", "md", "json", "xml", "kt", "java", "py", "html", "css", "js"
        )
    }

    class Factory : Fetcher.Factory<File> {
        override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher? {
            val ext = data.extension.lowercase()
            if (ext in SUPPORTED_EXTENSIONS) {
                return DocumentThumbnailFetcher(data, options)
            }
            return null
        }
    }

    class UriFactory : Fetcher.Factory<android.net.Uri> {
        override fun create(data: android.net.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == "file" && data.path != null) {
                val file = File(data.path!!)
                val ext = file.extension.lowercase()
                if (ext in SUPPORTED_EXTENSIONS) {
                    return DocumentThumbnailFetcher(file, options)
                }
            }
            return null
        }
    }
}
