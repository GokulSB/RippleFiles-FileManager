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
            "txt", "csv", "md" -> generateTextThumbnail()
            "docx", "pptx", "xlsx" -> generateDocxThumbnail()
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

    class Factory : Fetcher.Factory<File> {
        override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher? {
            val ext = data.extension.lowercase()
            if (ext in listOf("pdf", "txt", "csv", "md", "docx", "pptx", "xlsx")) {
                return DocumentThumbnailFetcher(data, options)
            }
            return null
        }
    }
}
