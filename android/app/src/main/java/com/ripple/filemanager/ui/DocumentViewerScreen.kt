package com.ripple.filemanager.ui

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.FileItem
import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    fileItem: FileItem,
    onClose: () -> Unit,
    onAction: (AppAction) -> Unit,
    cornerRoundness: Float = 0.5f
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize renderer
    var renderer by remember { mutableStateOf<DocumentRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    
    DisposableEffect(fileItem) {
        val file = File(fileItem.path)
        val source = when {
            file.name.endsWith(".pdf", true) -> DocumentSource.Pdf(file)
            file.name.endsWith(".docx", true) -> DocumentSource.Docx(file)
            file.name.endsWith(".md", true) -> DocumentSource.Markdown(file)
            else -> DocumentSource.Txt(file)
        }
        
        val newRenderer = when (source) {
            is DocumentSource.Pdf -> NativePdfRenderer(file)
            else -> SyntheticDocumentRenderer(context, file, source)
        }
        
        renderer = newRenderer
        pageCount = newRenderer.pageCount
        
        onDispose {
            newRenderer.close()
        }
    }

    // Thumbnail Cache (LRU)
    // 20 thumbnails in memory
    val thumbnailCache = remember {
        object : LruCache<Int, Bitmap>(20) {}
    }

    // High Res Page Cache (LRU)
    // 3 high-res pages in memory
    val pageCache = remember {
        object : LruCache<Int, Bitmap>(3) {}
    }

    val pagerState = rememberPagerState(pageCount = { pageCount })
    val listState = rememberLazyListState()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var bookmarkedPages by remember { mutableStateOf(setOf<Int>()) }

    androidx.activity.compose.BackHandler(onBack = onClose)

    // Sync scroll and reset zoom on page change
    LaunchedEffect(pagerState.currentPage) {
        if (pageCount > 0) {
            listState.animateScrollToItem(pagerState.currentPage)
            scale = 1f
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileItem.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp
                        )
                        if (pageCount > 0) {
                            Text(
                                text = "Page ${pagerState.currentPage + 1} of $pageCount",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            File(fileItem.path)
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = if (fileItem.name.endsWith(".pdf", true)) "application/pdf" else "*/*"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Document"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            // Left Rail
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(pageCount) { index ->
                    ThumbnailItem(
                        index = index,
                        renderer = renderer,
                        cache = thumbnailCache,
                        isSelected = index == pagerState.currentPage,
                        isBookmarked = bookmarkedPages.contains(index),
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
            
            // Main Content
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (pageCount > 0) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = scale == 1f
                    ) { page ->
                        MainPageItem(
                            index = page,
                            renderer = renderer,
                            cache = pageCache,
                            scale = scale,
                            offset = offset,
                            onScaleChange = { scale = it },
                            onOffsetChange = { offset = it }
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                
                // Floating Controls
                FloatingPill(
                    isBookmarked = bookmarkedPages.contains(pagerState.currentPage),
                    onToggleBookmark = {
                        val page = pagerState.currentPage
                        bookmarkedPages = if (bookmarkedPages.contains(page)) {
                            bookmarkedPages - page
                        } else {
                            bookmarkedPages + page
                        }
                    },
                    onZoomIn = { scale = (scale * 1.5f).coerceAtMost(5f) },
                    onZoomOut = { scale = (scale / 1.5f).coerceAtLeast(1f); if (scale == 1f) offset = androidx.compose.ui.geometry.Offset.Zero },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ThumbnailItem(
    index: Int,
    renderer: DocumentRenderer?,
    cache: LruCache<Int, Bitmap>,
    isSelected: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(cache.get(index)) }
    
    LaunchedEffect(index, renderer) {
        if (bitmap == null && renderer != null) {
            withContext(Dispatchers.IO) {
                val newBitmap = renderer.renderPage(index, 200) // 200px width thumbnail
                if (newBitmap != null) {
                    cache.put(index, newBitmap)
                    bitmap = newBitmap
                }
            }
        }
    }

    val borderColor = if (isSelected) Color(0xFFE0AC70) else Color(0xFF3A2F24)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(width = 48.dp, height = 62.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.page_number, index),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
        
        if (isBookmarked) {
            // Fold marker
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .background(
                        if (isSelected) Color(0xFFE0AC70) else Color(0xFF8A6435),
                        RoundedCornerShape(bottomStart = 8.dp)
                    )
            )
        }
    }
}

@Composable
fun MainPageItem(
    index: Int,
    renderer: DocumentRenderer?,
    cache: LruCache<Int, Bitmap>,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (androidx.compose.ui.geometry.Offset) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(cache.get(index)) }
    
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)
    
    LaunchedEffect(index, renderer) {
        if (bitmap == null && renderer != null) {
            withContext(Dispatchers.IO) {
                // High res render
                val newBitmap = renderer.renderPage(index, 1200) 
                if (newBitmap != null) {
                    cache.put(index, newBitmap)
                    bitmap = newBitmap
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (currentScale * zoom).coerceIn(1f, 5f)
                    onScaleChange(newScale)
                    if (newScale == 1f) {
                        onOffsetChange(androidx.compose.ui.geometry.Offset.Zero)
                    } else {
                        val maxPanX = (newScale - 1) * size.width.toFloat() / 2
                        val maxPanY = (newScale - 1) * size.height.toFloat() / 2
                        onOffsetChange(androidx.compose.ui.geometry.Offset(
                            (currentOffset.x + pan.x * newScale).coerceIn(-maxPanX, maxPanX),
                            (currentOffset.y + pan.y * newScale).coerceIn(-maxPanY, maxPanY)
                        ))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .shadow(8.dp)
                    .background(Color(0xFFF4ECDF))
            ) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.page_number, index),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun FloatingPill(
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onZoomIn, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.zoom_in), modifier = Modifier.size(20.dp))
            }
            HorizontalDivider(modifier = Modifier.width(20.dp).padding(vertical = 4.dp))
            IconButton(onClick = onZoomOut, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.zoom_out), modifier = Modifier.size(20.dp))
            }
            HorizontalDivider(modifier = Modifier.width(20.dp).padding(vertical = 4.dp))
            IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = stringResource(R.string.bookmark),
                    modifier = Modifier.size(20.dp),
                    tint = if (isBookmarked) Color(0xFFE0AC70) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
