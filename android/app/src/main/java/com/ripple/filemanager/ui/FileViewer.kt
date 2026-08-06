package com.ripple.filemanager.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import java.io.File
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(fileItem: FileItem, onClose: () -> Unit) {
    BackHandler {
        onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileItem.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val file = File(fileItem.path)
            if (fileItem.name.endsWith(".zip", ignoreCase = true)) {
                ZipViewer(file)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No viewer available for this file type.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PdfViewer(file: File) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var needsPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var isDecrypting by remember { mutableStateOf(false) }
    var decryptedFile by remember { mutableStateOf<File?>(null) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(file, decryptedFile) {
        val targetFile = decryptedFile ?: file
        withContext(Dispatchers.IO) {
            try {
                val fileDescriptor = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fileDescriptor)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                needsPassword = false
                errorMsg = null
            } catch (e: SecurityException) {
                needsPassword = true
            } catch (e: Exception) {
                if (e.message?.contains("password") == true || e.message?.contains("cannot create document") == true) {
                    needsPassword = true
                } else {
                    errorMsg = e.message
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
            decryptedFile?.delete()
        }
    }

    if (needsPassword) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* Cannot dismiss */ },
            title = { Text("Password Required") },
            text = {
                Column {
                    Text("This PDF is password protected.")
                    if (errorMsg != null) {
                        Text(
                            text = errorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true
                    )
                    if (isDecrypting) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        isDecrypting = true
                        errorMsg = null
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(file, password)
                                doc.setAllSecurityToBeRemoved(true)
                                val tempFile = File(context.cacheDir, "decrypted_${System.currentTimeMillis()}.pdf")
                                doc.save(tempFile)
                                doc.close()
                                decryptedFile = tempFile
                            } catch (e: Exception) {
                                errorMsg = "Invalid password or error: ${e.message}"
                            } finally {
                                isDecrypting = false
                            }
                        }
                    },
                    enabled = !isDecrypting && password.isNotEmpty()
                ) {
                    Text("Unlock")
                }
            }
        )
    }

    if (errorMsg != null && !needsPassword) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error opening PDF: $errorMsg", color = MaterialTheme.colorScheme.error)
        }
    } else if (pageCount > 0 && !needsPassword) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pageCount) { index ->
                PdfPageImage(pdfRenderer, index)
            }
        }
    } else if (!needsPassword) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun PdfPageImage(pdfRenderer: PdfRenderer?, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(pdfRenderer, pageIndex) {
        if (pdfRenderer != null) {
            withContext(Dispatchers.IO) {
                synchronized(pdfRenderer) {
                    try {
                        val page = pdfRenderer.openPage(pageIndex)
                        // Scale up for better resolution (assuming standard roughly 72dpi size, multiplying by 2.5 gives readable text)
                        val w = (page.width * 2.5).toInt()
                        val h = (page.height * 2.5).toInt()
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        bitmap = bmp
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    } else {
        Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun TextViewer(file: File) {
    var textContent by remember { mutableStateOf("Loading...") }
    
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = file.inputStream()
                val buffer = ByteArray(2 * 1024 * 1024) // up to 2MB
                val bytesRead = inputStream.read(buffer)
                val text = if (bytesRead > 0) String(buffer, 0, bytesRead) else ""
                textContent = if (file.length() > 2 * 1024 * 1024) "$text\n\n... [File truncated at 2MB]" else text
                inputStream.close()
            } catch (e: Exception) {
                textContent = "Error reading file: ${e.message}"
            }
        }
    }
    
    SelectionContainer {
        Text(
            text = textContent,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ZipViewer(file: File) {
    var entries by remember { mutableStateOf<List<java.util.zip.ZipEntry>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val zipFile = java.util.zip.ZipFile(file)
                val entryList = zipFile.entries().toList().filter { !it.isDirectory }
                entries = entryList.sortedBy { it.name }
                zipFile.close()
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (errorMsg != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error opening Zip: $errorMsg", color = MaterialTheme.colorScheme.error)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries) { entry ->
                ListItem(
                    headlineContent = { Text(entry.name, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                    supportingContent = { 
                        val sizeStr = if (entry.size > 0) "Size: $entry.size bytes" else "Unknown size"
                        Text(sizeStr) 
                    },
                    leadingContent = {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
