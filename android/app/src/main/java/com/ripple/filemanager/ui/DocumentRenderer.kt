package com.ripple.filemanager.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

sealed class DocumentSource {
    data class Pdf(val file: File) : DocumentSource()
    data class Docx(val file: File) : DocumentSource()
    data class Txt(val file: File) : DocumentSource()
    data class Markdown(val file: File) : DocumentSource()
}

interface DocumentRenderer : AutoCloseable {
    val pageCount: Int
    suspend fun renderPage(pageIndex: Int, width: Int): Bitmap?
}

class NativePdfRenderer(private val file: File) : DocumentRenderer {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    
    init {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    override suspend fun renderPage(pageIndex: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        synchronized(renderer) {
            try {
                val page = renderer.openPage(pageIndex)
                // Calculate height to maintain aspect ratio
                val height = (width.toFloat() / page.width * page.height).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                // Fill with white background
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                return@withContext bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    override fun close() {
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}

/**
 * Reads text content from a file on the IO dispatcher.
 * Handles plain text, markdown, json, xml, html, csv, log, code files.
 * Returns the full text or an error message.
 */
suspend fun readTextContent(file: File): String = withContext(Dispatchers.IO) {
    try {
        if (!file.exists()) return@withContext "File not found: ${file.path}"
        if (!file.canRead()) return@withContext "Cannot read file (permission denied): ${file.name}"
        val maxBytes = 4 * 1024 * 1024 // 4 MB cap
        val bytes = file.inputStream().use { it.readBytes() }
        val text = if (bytes.size > maxBytes) {
            String(bytes, 0, maxBytes, Charsets.UTF_8) + "\n\n... [File truncated at 4 MB]"
        } else {
            // Try UTF-8 first, fall back to system default
            try {
                String(bytes, Charsets.UTF_8)
            } catch (e: Exception) {
                String(bytes)
            }
        }
        text
    } catch (e: Exception) {
        "Error reading file: ${e.message}"
    }
}

/**
 * Reads text content from a .docx file using Apache POI on the IO dispatcher.
 * Falls back to a clear error message if POI is unavailable or the file is corrupt.
 */
suspend fun readDocxContent(file: File): String = withContext(Dispatchers.IO) {
    try {
        if (!file.exists()) return@withContext "File not found: ${file.path}"
        if (!file.canRead()) return@withContext "Cannot read file (permission denied): ${file.name}"
        FileInputStream(file).use { fis ->
            // Dynamically load XWPFDocument so a missing class (ProGuard stripped)
            // throws a clear ClassNotFoundException rather than a cryptic crash.
            val docClass = Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument")
            val doc = docClass.getConstructor(java.io.InputStream::class.java).newInstance(fis)
            val getParagraphs = docClass.getMethod("getParagraphs")
            @Suppress("UNCHECKED_CAST")
            val paragraphs = getParagraphs.invoke(doc) as List<*>
            val sb = StringBuilder()
            for (para in paragraphs) {
                val getText = para!!.javaClass.getMethod("getText")
                sb.append(getText.invoke(para) as? String ?: "").append("\n")
            }
            // Close the document
            try { (doc as? AutoCloseable)?.close() } catch (_: Exception) {}
            val result = sb.toString().trimEnd()
            if (result.isBlank()) "(This document appears to be empty or contains only non-text content)" else result
        }
    } catch (e: ClassNotFoundException) {
        "DOCX reading is unavailable in this build.\n\n" +
        "Technical detail: Apache POI library was not included. " +
        "Use an external app (e.g. WPS Office, Google Docs) to open this file."
    } catch (e: Exception) {
        "Error reading DOCX file: ${e.message}\n\n" +
        "The file may be corrupt, password-protected, or in an unsupported format."
    }
}

// ── Legacy SyntheticDocumentRenderer kept for any remaining callers ──────────
// NOTE: This class is no longer used by DocumentViewerScreen (which now uses
// the direct text/docx readers above). It is kept to avoid compilation errors
// if referenced elsewhere, but marked as deprecated.
@Deprecated("Use readTextContent() or readDocxContent() directly instead.")
class SyntheticDocumentRenderer(
    private val context: Context,
    private val file: File,
    private val type: DocumentSource
) : DocumentRenderer {
    
    // A4 Aspect ratio ~ 1.414
    private val PAGE_ASPECT_RATIO = 1.414f
    
    private var pages: List<CharSequence> = emptyList()
    
    // Default synthetic width for rendering (will scale down for thumbnails)
    private val RENDER_WIDTH = 1200
    private val RENDER_HEIGHT = (RENDER_WIDTH * PAGE_ASPECT_RATIO).toInt()
    
    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 40f
        color = Color.BLACK
        typeface = Typeface.DEFAULT
    }

    private val margin = 80
    private val contentWidth = RENDER_WIDTH - (margin * 2)
    private val contentHeight = RENDER_HEIGHT - (margin * 2)

    // No longer calls loadAndPaginate() synchronously in init.
    // Callers must call loadAsync() before using pageCount or renderPage().
    init { /* intentionally empty */ }

    suspend fun loadAsync() = withContext(Dispatchers.IO) {
        loadAndPaginate()
    }

    private fun loadAndPaginate() {
        val fullText = when (type) {
            is DocumentSource.Docx -> "(Use readDocxContent() instead)"
            else -> try { file.readText() } catch (e: Exception) { "Error reading file: ${e.message}" }
        }
        
        val newPages = mutableListOf<CharSequence>()
        var currentIndex = 0
        
        while (currentIndex < fullText.length) {
            val staticLayout = StaticLayout.Builder.obtain(fullText, currentIndex, fullText.length, textPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()
                
            var linesToFit = 0
            
            for (i in 0 until staticLayout.lineCount) {
                val currentHeight = staticLayout.getLineBottom(i)
                if (currentHeight > contentHeight) break
                linesToFit = i + 1
            }
            
            if (linesToFit == 0) linesToFit = 1
            
            val endOffset = staticLayout.getLineEnd(linesToFit - 1)
            newPages.add(fullText.subSequence(currentIndex, endOffset))
            currentIndex = endOffset
        }
        
        pages = newPages
    }

    override val pageCount: Int get() = pages.size

    override suspend fun renderPage(pageIndex: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pages.size) return@withContext null
        val text = pages[pageIndex]
        val height = (width * PAGE_ASPECT_RATIO).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val scale = width.toFloat() / RENDER_WIDTH.toFloat()
        canvas.scale(scale, scale)
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()
        canvas.translate(margin.toFloat(), margin.toFloat())
        staticLayout.draw(canvas)
        return@withContext bitmap
    }

    override fun close() { /* Nothing to close */ }
}
