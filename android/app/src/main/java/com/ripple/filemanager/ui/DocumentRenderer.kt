package com.ripple.filemanager.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

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

class NativePdfRenderer(
    private val file: File,
    private val context: Context? = null
) : DocumentRenderer {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    init {
        try {
            fileDescriptor = try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (e: Exception) {
                context?.let { ctx ->
                    if (file.path.startsWith("content://")) {
                        ctx.contentResolver.openFileDescriptor(Uri.parse(file.path), "r")
                    } else {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            ctx,
                            "${ctx.packageName}.fileprovider",
                            file
                        )
                        ctx.contentResolver.openFileDescriptor(uri, "r")
                    }
                } ?: throw e
            }
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor!!)
            }
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
                val height = (width.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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
        try { pdfRenderer?.close() } catch (_: Exception) {}
        try { fileDescriptor?.close() } catch (_: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
    }
}

class SyntheticDocumentRenderer(
    private val context: Context,
    private val file: File,
    private val type: DocumentSource
) : DocumentRenderer {

    // A4 Aspect ratio ~ 1.414
    private val PAGE_ASPECT_RATIO = 1.414f

    // Default synthetic canvas dimensions
    private val RENDER_WIDTH = 1200
    private val RENDER_HEIGHT = (RENDER_WIDTH * PAGE_ASPECT_RATIO).toInt()

    private val margin = 84
    private val contentWidth = RENDER_WIDTH - (margin * 2)
    private val contentHeight = RENDER_HEIGHT - (margin * 2)

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 38f
        color = Color.rgb(28, 27, 31)
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }

    private var pages: List<CharSequence> = listOf("Loading document…")

    var fullTextContent: String = ""
        private set

    override val pageCount: Int
        get() = pages.size

    suspend fun loadAndPaginateAsync() = withContext(Dispatchers.Default) {
        val rawText = try {
            when (type) {
                is DocumentSource.Docx -> parseDocx()
                else -> parseText()
            }
        } catch (e: Exception) {
            "Error reading document: ${e.message ?: "Unable to read content"}"
        }

        fullTextContent = rawText

        val fullText = if (rawText.isBlank()) {
            "(Empty document)"
        } else {
            rawText
        }

        val newPages = mutableListOf<CharSequence>()
        var currentIndex = 0
        val textLength = fullText.length

        while (currentIndex < textLength) {
            val remainingLength = textLength - currentIndex
            val chunkLength = minOf(remainingLength, 8000)
            val chunkEnd = currentIndex + chunkLength
            val chunk = fullText.subSequence(currentIndex, chunkEnd)

            val staticLayout = StaticLayout.Builder.obtain(chunk, 0, chunk.length, textPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.25f)
                .setIncludePad(false)
                .build()

            var linesToFit = 0
            for (i in 0 until staticLayout.lineCount) {
                if (staticLayout.getLineBottom(i) > contentHeight) {
                    break
                }
                linesToFit = i + 1
            }

            if (linesToFit == 0) {
                linesToFit = 1
            }

            val lineEndInChunk = staticLayout.getLineEnd(linesToFit - 1)
            val advance = maxOf(1, lineEndInChunk)
            val pageEnd = minOf(textLength, currentIndex + advance)

            newPages.add(fullText.subSequence(currentIndex, pageEnd))
            currentIndex = pageEnd

            // Safety limit: cap synthetic pages at 250 pages to prevent memory spikes
            if (newPages.size >= 250 && currentIndex < textLength) {
                newPages.add("\n\n... [Content truncated. Displaying first 250 pages.]")
                break
            }
        }

        if (newPages.isEmpty()) {
            newPages.add("(Empty document)")
        }

        pages = newPages
    }

    private fun parseText(): String {
        val stream = openFileStream(context, file)
        return stream.use { readTextFromStream(it) }
    }

    private fun parseDocx(): String {
        val stream = openFileStream(context, file)
        return stream.use { readDocxFromStream(it) }
    }

    override suspend fun renderPage(pageIndex: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pages.size) return@withContext null

        val text = pages[pageIndex]
        val height = (width * PAGE_ASPECT_RATIO).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val scale = width.toFloat() / RENDER_WIDTH.toFloat()
        canvas.scale(scale, scale)

        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.25f)
            .setIncludePad(false)
            .build()

        canvas.translate(margin.toFloat(), margin.toFloat())
        staticLayout.draw(canvas)

        return@withContext bitmap
    }

    override fun close() {
        // Clean up resources if necessary
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stream & decoding helpers
// ─────────────────────────────────────────────────────────────────────────────

fun openFileStream(context: Context?, file: File): InputStream {
    val path = file.path
    if (path.startsWith("content://")) {
        if (context == null) throw FileNotFoundException("Context required to read content URI: $path")
        return context.contentResolver.openInputStream(Uri.parse(path))
            ?: throw FileNotFoundException("Unable to open stream for $path")
    }

    try {
        if (file.exists() && file.canRead()) {
            return FileInputStream(file)
        }
    } catch (_: Exception) {}

    // Fallback via ContentResolver / MediaStore / FileProvider
    context?.let { ctx ->
        try {
            val projection = arrayOf(android.provider.MediaStore.Files.FileColumns._ID)
            val cursor = ctx.contentResolver.query(
                android.provider.MediaStore.Files.getContentUri("external"),
                projection,
                "${android.provider.MediaStore.Files.FileColumns.DATA} = ?",
                arrayOf(file.absolutePath),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns._ID))
                    val uri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Files.getContentUri("external"),
                        id
                    )
                    val s = ctx.contentResolver.openInputStream(uri)
                    if (s != null) return s
                }
            }
        } catch (_: Exception) {}

        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            val s = ctx.contentResolver.openInputStream(uri)
            if (s != null) return s
        } catch (_: Exception) {}
    }

    // Direct attempt if previous checks didn't return
    return FileInputStream(file)
}

fun readTextFromStream(inputStream: InputStream, maxBytes: Int = 4 * 1024 * 1024): String {
    val buffer = ByteArray(65536)
    val out = ByteArrayOutputStream()
    var total = 0
    var n: Int
    var truncated = false
    while (inputStream.read(buffer).also { n = it } != -1) {
        if (total + n > maxBytes) {
            val rem = maxBytes - total
            if (rem > 0) out.write(buffer, 0, rem)
            truncated = true
            break
        }
        out.write(buffer, 0, n)
        total += n
    }
    val bytes = out.toByteArray()
    if (bytes.isEmpty()) return "(Empty file)"
    val text = decodeTextBytes(bytes)
    return if (truncated) "$text\n\n... [Content truncated at 4 MB]" else text
}

fun decodeTextBytes(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""
    // Check UTF-8 BOM: EF BB BF
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
        return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
    }
    // Check UTF-16LE BOM: FF FE
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
    }
    // Check UTF-16BE BOM: FE FF
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
    }
    // Strict UTF-8 decoding
    return try {
        val decoder = Charsets.UTF_8.newDecoder()
        decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
        decoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    } catch (_: Exception) {
        // Fallback to ISO-8859-1 which maps every byte 0x00-0xFF to a valid character
        try {
            String(bytes, Charsets.ISO_8859_1)
        } catch (_: Exception) {
            String(bytes)
        }
    }
}

fun readDocxFromStream(inputStream: InputStream): String {
    return try {
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        var documentXml: String? = null
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var len: Int
                while (zip.read(buf).also { len = it } != -1) {
                    out.write(buf, 0, len)
                }
                documentXml = String(out.toByteArray(), Charsets.UTF_8)
                break
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        if (documentXml.isNullOrBlank()) {
            return "(This Word document appears to be empty or has no readable text)"
        }
        parseDocxXmlText(documentXml)
    } catch (e: Exception) {
        "Error reading DOCX file: ${e.message ?: "Unable to read document"}\n\nThe file may be corrupt or password-protected."
    }
}

fun parseDocxXmlText(xml: String): String {
    return try {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        val sb = StringBuilder()
        var eventType = parser.eventType
        var inTableCell = false
        val cellSb = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.substringAfterLast(':') ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "t" -> {
                            val text = parser.nextText()
                            if (inTableCell) cellSb.append(text) else sb.append(text)
                        }
                        "tab" -> {
                            if (inTableCell) cellSb.append("\t") else sb.append("\t")
                        }
                        "br", "cr" -> {
                            if (inTableCell) cellSb.append("\n") else sb.append("\n")
                        }
                        "tc" -> {
                            inTableCell = true
                            cellSb.setLength(0)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tag) {
                        "p" -> {
                            if (!inTableCell) {
                                sb.append("\n\n")
                            }
                        }
                        "tc" -> {
                            inTableCell = false
                            val cell = cellSb.toString().trim()
                            if (cell.isNotEmpty()) {
                                sb.append(cell).append("  |  ")
                            }
                        }
                        "tr" -> {
                            sb.append("\n")
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        val res = sb.toString().trim()
        if (res.isBlank()) "(This Word document appears to be empty)" else res
    } catch (e: Exception) {
        // Fallback regex cleaner
        val cleaned = xml.replace(Regex("<w:p[^>]*>"), "\n\n")
            .replace(Regex("<w:tc[^>]*>"), "  |  ")
            .replace(Regex("<w:tr[^>]*>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .trim()
        if (cleaned.isNotBlank()) cleaned else "Error reading document: ${e.message}"
    }
}

suspend fun readTextContent(file: File, context: Context? = null): String = withContext(Dispatchers.IO) {
    try {
        val stream = openFileStream(context, file)
        stream.use { readTextFromStream(it) }
    } catch (e: Exception) {
        "Error reading file: ${e.message ?: "Unable to read content"}"
    }
}

suspend fun readDocxContent(file: File, context: Context? = null): String = withContext(Dispatchers.IO) {
    try {
        val stream = openFileStream(context, file)
        stream.use { readDocxFromStream(it) }
    } catch (e: Exception) {
        "Error reading DOCX file: ${e.message ?: "Unable to read document"}"
    }
}
