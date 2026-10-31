package com.ripple.filemanager.ui

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.R
import com.ripple.filemanager.haptics.LocalHaptics
import com.ripple.filemanager.ui.expressive.CookieIcon
import com.ripple.filemanager.ui.expressive.CookieIconButton
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens
import com.ripple.filemanager.ui.expressive.pressScale
import com.ripple.filemanager.ui.theme.LocalAppFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    fileItem: FileItem,
    onClose: () -> Unit,
    onAction: (AppAction) -> Unit,
    cornerRoundness: Float = 0.5f
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var renderer by remember { mutableStateOf<DocumentRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var passwordPromptError by remember { mutableStateOf<String?>(null) }
    var isDecryptingPdf by remember { mutableStateOf(false) }
    var decryptedTempPdfFile by remember { mutableStateOf<File?>(null) }

    val file = remember(fileItem.path) { File(fileItem.path) }
    val ext = remember(fileItem.name) { fileItem.name.substringAfterLast('.', "").lowercase() }
    val isPdf = ext == "pdf"
    val isDocx = ext == "docx" || ext == "doc"
    val isMd = ext == "md"

    DisposableEffect(fileItem.path) {
        onDispose {
            renderer?.close()
            try { decryptedTempPdfFile?.delete() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(fileItem.path) {
        isLoading = true
        errorMessage = null
        showPasswordPrompt = false
        passwordPromptError = null
        withContext(Dispatchers.IO) {
            try {
                val newRenderer: DocumentRenderer = if (isPdf) {
                    val pdf = NativePdfRenderer(file, context)
                    if (pdf.pageCount == 0) {
                        throw Exception("Unable to open PDF or file is empty.")
                    }
                    pdf
                } else {
                    val source = when {
                        isDocx -> DocumentSource.Docx(file)
                        isMd -> DocumentSource.Markdown(file)
                        else -> DocumentSource.Txt(file)
                    }
                    val synth = SyntheticDocumentRenderer(context, file, source)
                    synth.loadAndPaginateAsync()
                    synth
                }

                withContext(Dispatchers.Main) {
                    renderer?.close()
                    renderer = newRenderer
                    pageCount = newRenderer.pageCount
                    isLoading = false
                }
            } catch (e: PasswordRequiredException) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    showPasswordPrompt = true
                    errorMessage = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (e.message?.contains("password", ignoreCase = true) == true) {
                        isLoading = false
                        showPasswordPrompt = true
                        errorMessage = null
                    } else {
                        errorMessage = e.message ?: "Failed to read document"
                        isLoading = false
                    }
                }
            }
        }
    }

    // Thumbnail Cache (LRU)
    val thumbnailCache = remember { object : LruCache<Int, Bitmap>(25) {} }
    // High Res Page Cache (LRU)
    val pageCache = remember { object : LruCache<Int, Bitmap>(5) {} }

    val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })
    val listState = rememberLazyListState()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var bookmarkedPages by remember { mutableStateOf(setOf<Int>()) }
    var isBottomExpanded by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
        if (isBottomExpanded) isBottomExpanded = false else onClose()
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pageCount > 0) {
            listState.animateScrollToItem(pagerState.currentPage)
            scale = 1f
            offset = Offset.Zero
        }
    }

    Column(modifier = Modifier.fillMaxSize().appGradientBackground()) {
        ExpressiveDocTopBar(
            fileItem = fileItem,
            currentPage = pagerState.currentPage,
            pageCount = pageCount,
            onBack = onClose
        )

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.accent)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isDocx) "Reading document…" else "Loading…",
                            fontSize = 14.sp,
                            color = colors.muted,
                            fontFamily = LocalAppFont.current
                        )
                    }
                }
            }
            showPasswordPrompt || (errorMessage == null && pageCount == 0 && isPdf) -> {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CookieIcon(
                            icon = Icons.Outlined.Lock,
                            bgColor = colors.card,
                            iconTint = colors.accent,
                            size = 56.dp,
                            lobes = 8
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "PDF is password protected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalAppFont.current,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This document requires a password to open.",
                            fontSize = 13.sp,
                            fontFamily = LocalAppFont.current,
                            color = colors.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showPasswordPrompt = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.ink
                            )
                        ) {
                            Text("Enter Password", fontWeight = FontWeight.SemiBold, fontFamily = LocalAppFont.current)
                        }
                    }
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontFamily = LocalAppFont.current,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                openFileInExternalApp(context, fileItem, "*/*")
                            }
                        ) {
                            Text("Open in external app")
                        }
                    }
                }
            }
            pageCount > 0 -> {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    ExpressiveDocThumbnailRail(
                        pageCount = pageCount,
                        renderer = renderer,
                        cache = thumbnailCache,
                        currentPage = pagerState.currentPage,
                        bookmarkedPages = bookmarkedPages,
                        listState = listState,
                        onPageClick = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                    )
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = scale == 1f
                        ) { page ->
                            ExpressiveMainPageItem(
                                index = page,
                                renderer = renderer,
                                cache = pageCache,
                                scale = scale,
                                offset = offset,
                                onScaleChange = { scale = it },
                                onOffsetChange = { offset = it }
                            )
                        }
                        ExpressiveFloatingPill(
                            isBookmarked = bookmarkedPages.contains(pagerState.currentPage),
                            onToggleBookmark = {
                                val page = pagerState.currentPage
                                bookmarkedPages = if (bookmarkedPages.contains(page)) bookmarkedPages - page else bookmarkedPages + page
                            },
                            onZoomIn = { scale = (scale * 1.5f).coerceAtMost(5f) },
                            onZoomOut = {
                                scale = (scale / 1.5f).coerceAtLeast(1f)
                                if (scale == 1f) offset = Offset.Zero
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        )
                    }
                }

                ExpressiveDocBottomSheet(
                    fileItem = fileItem,
                    currentPage = pagerState.currentPage,
                    pageCount = pageCount,
                    isExpanded = isBottomExpanded,
                    onToggleExpand = { isBottomExpanded = !isBottomExpanded },
                    onAction = onAction,
                    context = context,
                    fullText = (renderer as? SyntheticDocumentRenderer)?.fullTextContent
                )
            }
        }
    }

    if (showPasswordPrompt) {
        var pdfPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isDecryptingPdf) {
                    showPasswordPrompt = false
                    if (pageCount == 0) {
                        onClose()
                    }
                }
            },
            containerColor = colors.container,
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CookieIcon(
                        icon = Icons.Outlined.Lock,
                        bgColor = colors.card,
                        iconTint = colors.accent,
                        size = 38.dp,
                        lobes = 8
                    )
                    Text(
                        text = "Password Protected PDF",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalAppFont.current,
                        color = colors.text
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This PDF file is encrypted. Enter the document password to unlock and read it.",
                        fontSize = 13.sp,
                        fontFamily = LocalAppFont.current,
                        color = colors.muted
                    )

                    OutlinedTextField(
                        value = pdfPassword,
                        onValueChange = {
                            pdfPassword = it
                            passwordPromptError = null
                        },
                        label = { Text("Password", fontFamily = LocalAppFont.current) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.card,
                            unfocusedContainerColor = colors.card,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = colors.muted
                                )
                            }
                        }
                    )

                    if (passwordPromptError != null) {
                        Text(
                            text = passwordPromptError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontFamily = LocalAppFont.current
                        )
                    }

                    if (isDecryptingPdf) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Decrypting PDF...",
                                fontSize = 12.sp,
                                fontFamily = LocalAppFont.current,
                                color = colors.muted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pdfPassword.isNotEmpty() && !isDecryptingPdf) {
                            isDecryptingPdf = true
                            passwordPromptError = null
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(file, pdfPassword)
                                    doc.setAllSecurityToBeRemoved(true)
                                    val tempFile = File(context.cacheDir, "pdf_decrypted_${System.currentTimeMillis()}.pdf")
                                    doc.save(tempFile)
                                    doc.close()

                                    val decryptedRenderer = NativePdfRenderer(tempFile, context)
                                    if (decryptedRenderer.pageCount == 0) {
                                        throw Exception("Decrypted document could not be opened.")
                                    }

                                    withContext(Dispatchers.Main) {
                                        try { decryptedTempPdfFile?.delete() } catch (_: Exception) {}
                                        decryptedTempPdfFile = tempFile
                                        renderer?.close()
                                        renderer = decryptedRenderer
                                        pageCount = decryptedRenderer.pageCount
                                        isDecryptingPdf = false
                                        showPasswordPrompt = false
                                        passwordPromptError = null
                                    }
                                } catch (pe: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
                                    withContext(Dispatchers.Main) {
                                        isDecryptingPdf = false
                                        passwordPromptError = "Incorrect password. Please try again."
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isDecryptingPdf = false
                                        val msg = e.message ?: ""
                                        if (msg.contains("password", ignoreCase = true)) {
                                            passwordPromptError = "Incorrect password. Please try again."
                                        } else {
                                            passwordPromptError = "Error decrypting PDF: ${e.localizedMessage ?: "Unknown error"}"
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = pdfPassword.isNotEmpty() && !isDecryptingPdf,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink,
                        disabledContainerColor = colors.accent.copy(alpha = 0.4f),
                        disabledContentColor = colors.ink.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Unlock", fontWeight = FontWeight.SemiBold, fontFamily = LocalAppFont.current)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isDecryptingPdf) {
                            showPasswordPrompt = false
                            if (pageCount == 0) {
                                onClose()
                            }
                        }
                    }
                ) {
                    Text("Cancel", color = colors.muted, fontFamily = LocalAppFont.current)
                }
            }
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Top Bar
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ExpressiveDocTopBar(
    fileItem: FileItem,
    currentPage: Int,
    pageCount: Int,
    onBack: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            CookieIconButton(
                onClick = {
                    haptics.tap()
                    onBack()
                },
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                bgColor = colors.card,
                iconTint = colors.text,
                description = "Back",
                size = 42.dp,
                iconSize = 20.dp,
                lobes = 8
            )

            // File title + page counter
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = fileItem.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (pageCount > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.page_of_total, currentPage + 1, pageCount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = LocalAppFont.current,
                        color = colors.muted
                    )
                }
            }

            // Document type icon badge
            val ext = fileItem.name.substringAfterLast('.', "").lowercase()
            val docIcon = when (ext) {
                "pdf" -> Icons.Outlined.PictureAsPdf
                "docx", "doc" -> Icons.Outlined.Description
                "md" -> Icons.Outlined.Code
                "json" -> Icons.Outlined.DataObject
                else -> Icons.Outlined.TextSnippet
            }
            CookieIcon(
                icon = docIcon,
                bgColor = ExpressiveTokens.PastelSky,
                iconTint = ExpressiveTokens.OnPastel,
                size = 42.dp,
                lobes = 8
            )
        }

        // Slim progress rule showing page position
        val progress = if (pageCount > 1) {
            (currentPage.toFloat() / (pageCount - 1).coerceAtLeast(1))
        } else 1f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(colors.container)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(colors.accent)
            )
        }

        // Warm gradient divider strip — adds depth between header and content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.glow.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Thumbnail Rail
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ExpressiveDocThumbnailRail(
    pageCount: Int,
    renderer: DocumentRenderer?,
    cache: LruCache<Int, Bitmap>,
    currentPage: Int,
    bookmarkedPages: Set<Int>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPageClick: (Int) -> Unit
) {
    val colors = ExpressiveTheme.colors

    Column(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                ambientColor = Color.Transparent,
                spotColor = colors.shadow.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
            .background(colors.container.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = colors.line.copy(alpha = colors.lineAlpha),
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
            )
    ) {
        // Rail header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pages",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = LocalAppFont.current,
                color = colors.muted,
                letterSpacing = 0.8.sp
            )
        }

        // Thin accent divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(1.dp)
                .background(colors.accent.copy(alpha = 0.25f))
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            state = listState
        ) {
            items(pageCount) { index ->
                ExpressiveThumbnailItem(
                    index = index,
                    renderer = renderer,
                    cache = cache,
                    isSelected = index == currentPage,
                    isBookmarked = bookmarkedPages.contains(index),
                    onClick = { onPageClick(index) }
                )
            }
        }
    }
}

@Composable
private fun ExpressiveThumbnailItem(
    index: Int,
    renderer: DocumentRenderer?,
    cache: LruCache<Int, Bitmap>,
    isSelected: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current
    var bitmap by remember { mutableStateOf<Bitmap?>(cache.get(index)) }

    LaunchedEffect(index, renderer) {
        if (bitmap == null && renderer != null) {
            withContext(Dispatchers.IO) {
                val newBitmap = renderer.renderPage(index, 200)
                if (newBitmap != null) {
                    cache.put(index, newBitmap)
                    bitmap = newBitmap
                }
            }
        }
    }

    val borderColor = if (isSelected) colors.accent else colors.line.copy(alpha = colors.lineAlpha)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 70.dp)
                .then(
                    if (isSelected) {
                        Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(10.dp),
                                ambientColor = Color.Transparent,
                                spotColor = colors.accent.copy(alpha = 0.4f)
                            )
                    } else Modifier
                )
                .pressScale(pressedScale = 0.94f, interactionSource = interactionSource)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card)
                .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    haptics.tap()
                    onClick()
                }
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.page_number, index),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent
                    )
                }
            }

            // Bookmark fold indicator
            if (isBookmarked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .background(
                            colors.accent,
                            RoundedCornerShape(bottomStart = 8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        // Page number label
        Text(
            text = "${index + 1}",
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = LocalAppFont.current,
            color = if (isSelected) colors.accent else colors.muted,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Main Page Item
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveMainPageItem(
    index: Int,
    renderer: DocumentRenderer?,
    cache: LruCache<Int, Bitmap>,
    scale: Float,
    offset: Offset,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit
) {
    val colors = ExpressiveTheme.colors
    var bitmap by remember { mutableStateOf<Bitmap?>(cache.get(index)) }

    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)

    LaunchedEffect(index, renderer) {
        if (bitmap == null && renderer != null) {
            withContext(Dispatchers.IO) {
                val newBitmap = renderer.renderPage(index, 1200)
                if (newBitmap != null) {
                    cache.put(index, newBitmap)
                    bitmap = newBitmap
                }
            }
        }
    }

    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        val newScale = (currentScale * zoomChange).coerceIn(1f, 5f)
        onScaleChange(newScale)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .let { modifier ->
                if (scale > 1f) {
                    modifier.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                            onScaleChange(newScale)
                            if (newScale <= 1f) {
                                onScaleChange(1f)
                                onOffsetChange(Offset.Zero)
                            } else {
                                val maxPanX = (newScale - 1) * size.width.toFloat() / 2
                                val maxPanY = (newScale - 1) * size.height.toFloat() / 2
                                onOffsetChange(
                                    Offset(
                                        (currentOffset.x + pan.x * newScale).coerceIn(-maxPanX, maxPanX),
                                        (currentOffset.y + pan.y * newScale).coerceIn(-maxPanY, maxPanY)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    modifier.transformable(transformableState)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            // Soft container card behind the page (matching image viewer pattern)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.90f)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.container.copy(alpha = 0.35f))
                    .border(
                        1.dp,
                        colors.line.copy(alpha = colors.lineAlpha),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner page with shadow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Transparent,
                            spotColor = colors.shadow
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card)
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.page_number, index),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Loading state with themed spinner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Loading page…",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Floating Zoom/Bookmark Pill
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ExpressiveFloatingPill(
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    Box(modifier = modifier) {
        // Subtle glow halo behind the pill
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 0.5f }
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        colors = listOf(
                            colors.glow.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.maxDimension * 0.8f
                    )
                    onDrawBehind {
                        drawRect(brush = brush)
                    }
                }
        )

        Column(
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Transparent,
                    spotColor = colors.shadow.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(colors.container)
                .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(22.dp))
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Zoom In
            CookieIconButton(
                onClick = { haptics.tap(); onZoomIn() },
                icon = Icons.Filled.Add,
                bgColor = colors.card,
                iconTint = colors.text,
                description = stringResource(R.string.zoom_in),
                size = 38.dp,
                iconSize = 18.dp,
                lobes = 8
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(1.dp)
                    .background(colors.line.copy(alpha = colors.lineAlpha))
            )

            // Zoom Out
            CookieIconButton(
                onClick = { haptics.tap(); onZoomOut() },
                icon = Icons.Filled.Remove,
                bgColor = colors.card,
                iconTint = colors.text,
                description = stringResource(R.string.zoom_out),
                size = 38.dp,
                iconSize = 18.dp,
                lobes = 8
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(1.dp)
                    .background(colors.line.copy(alpha = colors.lineAlpha))
            )

            // Bookmark
            CookieIconButton(
                onClick = { haptics.tap(); onToggleBookmark() },
                icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                bgColor = if (isBookmarked) colors.accent else colors.card,
                iconTint = if (isBookmarked) colors.ink else colors.text,
                description = stringResource(R.string.bookmark),
                size = 38.dp,
                iconSize = 18.dp,
                lobes = 8
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Bottom Action Sheet
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ExpressiveDocBottomSheet(
    fileItem: FileItem,
    currentPage: Int,
    pageCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAction: (AppAction) -> Unit,
    context: android.content.Context,
    fullText: String? = null
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "doc_sheet_chevron"
    )

    val ext = fileItem.name.substringAfterLast('.', "").lowercase()
    val sizeFormatted = formatSize(File(fileItem.path).length())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                ambientColor = Color.Transparent,
                spotColor = colors.shadow.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .background(colors.container)
            .border(
                1.dp,
                colors.line.copy(alpha = colors.lineAlpha),
                RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
            )
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = if (isExpanded) 16.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Grab handle ──
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.muted.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.tap()
                    onToggleExpand()
                }
        )

        // ── Header row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Doc type cookie icon
            val docIcon = when (ext) {
                "pdf" -> Icons.Outlined.PictureAsPdf
                "docx", "doc" -> Icons.Outlined.Description
                "md" -> Icons.Outlined.Code
                "json" -> Icons.Outlined.DataObject
                else -> Icons.Outlined.TextSnippet
            }
            CookieIcon(
                icon = docIcon,
                bgColor = ExpressiveTokens.PastelSky,
                iconTint = ExpressiveTokens.OnPastel,
                size = 46.dp,
                lobes = 8
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.tap()
                        onToggleExpand()
                    }
            ) {
                Text(
                    text = fileItem.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$sizeFormatted · ${ext.uppercase()} · $pageCount pages",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chevron toggle
            CookieIconButton(
                onClick = {
                    haptics.tap()
                    onToggleExpand()
                },
                icon = Icons.Default.KeyboardArrowUp,
                bgColor = colors.card,
                iconTint = colors.text,
                description = if (isExpanded) "Collapse" else "Expand",
                size = 36.dp,
                iconSize = 20.dp,
                modifier = Modifier.rotate(chevronRotation),
                lobes = 8
            )
        }

        // ── Action cards row ──
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share
            DocActionCard(
                icon = Icons.Outlined.Share,
                label = "Share",
                onClick = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(fileItem.path)
                    )
                    val mimeType = when (ext) {
                        "pdf" -> "application/pdf"
                        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        "json" -> "application/json"
                        "md", "txt" -> "text/plain"
                        else -> "*/*"
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Document"))
                }
            )

            // Copy text
            if (!fullText.isNullOrBlank()) {
                DocActionCard(
                    icon = Icons.Outlined.ContentCopy,
                    label = "Copy",
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(fileItem.name, fullText)
                        clipboard?.setPrimaryClip(clip)
                        onAction(AppAction.ShowToast("Text copied to clipboard"))
                    }
                )
            }

            // Open with external app
            DocActionCard(
                icon = Icons.Outlined.OpenInNew,
                label = "Open",
                onClick = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(fileItem.path)
                    )
                    val mimeType = when (ext) {
                        "pdf" -> "application/pdf"
                        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        "json" -> "application/json"
                        "md", "txt" -> "text/plain"
                        else -> "*/*"
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(android.content.Intent.createChooser(intent, "Open with"))
                    } catch (_: Exception) {}
                }
            )

            // Favourite
            val isFav = fileItem.isPinned
            DocActionCard(
                icon = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                label = if (isFav) "Unfav" else "Fav",
                isHighlight = isFav,
                onClick = { onAction(AppAction.TogglePin(fileItem.path)) }
            )

            // Print (for PDFs)
            if (ext == "pdf") {
                DocActionCard(
                    icon = Icons.Outlined.Print,
                    label = "Print",
                    onClick = {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            File(fileItem.path)
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }

            // Copy path
            DocActionCard(
                icon = Icons.Outlined.ContentCopy,
                label = "Copy",
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("File Path", fileItem.path))
                    onAction(AppAction.ShowToast("Path copied"))
                }
            )
        }

        // ── Expandable details panel ──
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExpressiveDocDetailsCard(fileItem = fileItem, pageCount = pageCount)

                Spacer(modifier = Modifier.height(14.dp))

                // "Open in External App" pill button (matching image viewer)
                val pillInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .pressScale(pressedScale = 0.96f, interactionSource = pillInteractionSource)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.accent)
                        .clickable(
                            interactionSource = pillInteractionSource,
                            indication = null
                        ) {
                            haptics.tap()
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                File(fileItem.path)
                            )
                            val mimeType = when (ext) {
                                "pdf" -> "application/pdf"
                                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                "json" -> "application/json"
                                "md", "txt" -> "text/plain"
                                else -> "*/*"
                            }
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(android.content.Intent.createChooser(intent, "Open with"))
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Open in External App",
                            fontFamily = LocalAppFont.current,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Document Action Card
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DocActionCard(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val haptics = LocalHaptics.current

    val containerBg = when {
        isDestructive -> colors.accent.copy(alpha = 0.12f)
        isHighlight -> colors.accent
        else -> colors.card
    }
    val contentColor = when {
        isDestructive -> colors.accent
        isHighlight -> colors.ink
        else -> colors.text
    }
    val borderColor = when {
        isDestructive -> colors.accent.copy(alpha = 0.35f)
        isHighlight -> colors.accent
        else -> colors.line.copy(alpha = colors.lineAlpha * 2f)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(68.dp)
            .pressScale(pressedScale = 0.94f, interactionSource = interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptics.tap()
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Document Details Card
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ExpressiveDocDetailsCard(fileItem: FileItem, pageCount: Int) {
    val colors = ExpressiveTheme.colors
    val ext = fileItem.name.substringAfterLast('.', "—").uppercase()
    val sizeFormatted = formatSize(File(fileItem.path).length())
    val modifiedFormatted = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
        .format(Date(fileItem.lastModified))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Row 1: Modified + Format
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DocDetailItem(
                label = "Modified",
                value = modifiedFormatted,
                icon = Icons.Outlined.CalendarToday,
                modifier = Modifier.weight(1f)
            )
            DocDetailItem(
                label = "Format",
                value = ext,
                icon = Icons.Outlined.Description,
                modifier = Modifier.weight(0.6f)
            )
        }

        // Row 2: Pages + Size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DocDetailItem(
                label = "Pages",
                value = "$pageCount",
                icon = Icons.Outlined.MenuBook,
                modifier = Modifier.weight(1f)
            )
            DocDetailItem(
                label = "Size",
                value = sizeFormatted,
                icon = Icons.Outlined.Storage,
                modifier = Modifier.weight(0.6f)
            )
        }

        // File path
        Text(
            text = fileItem.path,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = LocalAppFont.current,
            color = colors.muted.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DocDetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = LocalAppFont.current,
                color = colors.muted,
                letterSpacing = 0.6.sp
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = LocalAppFont.current,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}