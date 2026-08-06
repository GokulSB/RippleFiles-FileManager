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
import org.apache.poi.xwpf.usermodel.XWPFDocument
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

    init {
        // We load synchronously here just to initialize pageCount
        // In a real large-scale app, we might paginate asynchronously
        loadAndPaginate()
    }

    private fun loadAndPaginate() {
        val fullText = when (type) {
            is DocumentSource.Docx -> parseDocx()
            else -> parseText()
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
            var currentHeight = 0
            
            for (i in 0 until staticLayout.lineCount) {
                currentHeight = staticLayout.getLineBottom(i)
                if (currentHeight > contentHeight) {
                    break
                }
                linesToFit = i + 1
            }
            
            if (linesToFit == 0) {
                // If even one line doesn't fit, force at least one line to avoid infinite loop
                linesToFit = 1
            }
            
            val endOffset = staticLayout.getLineEnd(linesToFit - 1)
            newPages.add(fullText.subSequence(currentIndex, endOffset))
            currentIndex = endOffset
        }
        
        pages = newPages
    }

    private fun parseText(): CharSequence {
        return try {
            file.readText()
        } catch (e: Exception) {
            "Error reading file"
        }
    }

    private fun parseDocx(): CharSequence {
        return try {
            FileInputStream(file).use { fis ->
                val document = XWPFDocument(fis)
                val sb = java.lang.StringBuilder()
                for (paragraph in document.paragraphs) {
                    sb.append(paragraph.text).append("\n")
                }
                sb.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error reading DOCX file: ${e.message}"
        }
    }

    override val pageCount: Int
        get() = pages.size

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

    override fun close() {
        // Nothing to close
    }
}
