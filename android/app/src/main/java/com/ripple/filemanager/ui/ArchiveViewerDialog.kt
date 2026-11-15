package com.ripple.filemanager.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.core.view.WindowCompat
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.R
import com.ripple.filemanager.archive.ArchiveEntryInfo
import com.ripple.filemanager.archive.ArchiveHandlerFactory
import com.ripple.filemanager.archive.ArchiveProgress
import com.ripple.filemanager.archive.SUPPORTED_ZIP_CHARSETS
import com.ripple.filemanager.ui.expressive.CookieIcon
import com.ripple.filemanager.ui.expressive.CookieIconButton
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens
import com.ripple.filemanager.ui.expressive.PageHeader
import com.ripple.filemanager.ui.expressive.RippleBackground
import com.ripple.filemanager.ui.expressive.SectionContainer
import com.ripple.filemanager.ui.expressive.rememberIconShape
import com.ripple.filemanager.ui.theme.LocalAppFont
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

data class ArchiveItemDisplay(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val isEncrypted: Boolean,
    val entryIndex: Int = -1,
    val itemsCount: Int = 0
)

@Composable
fun ArchiveViewerDialog(
    archiveFile: FileItem,
    onDismiss: () -> Unit,
    onExtractRequest: (password: String?, selectedIndices: List<Int>?) -> Unit
) {
    val view = LocalView.current
    val colors = ExpressiveTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { win ->
                win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                win.statusBarColor = android.graphics.Color.TRANSPARENT
                win.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.setDecorFitsSystemWindows(win, false)
                val insets = WindowCompat.getInsetsController(win, view)
                val isDark = colors.bg.luminance() < 0.5f
                insets.isAppearanceLightStatusBars = !isDark
                insets.isAppearanceLightNavigationBars = !isDark
            }
        }
        ArchiveBrowserScreen(
            archiveFile = archiveFile,
            onClose = onDismiss,
            onExtractRequest = onExtractRequest
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveBrowserScreen(
    archiveFile: FileItem,
    onClose: () -> Unit,
    onExtractRequest: (password: String?, selectedIndices: List<Int>?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val coroutineScope = rememberCoroutineScope()
    val colors = ExpressiveTheme.colors

    val sourceUri = remember(archiveFile) {
        if (archiveFile.path.startsWith("content://")) {
            Uri.parse(archiveFile.path)
        } else {
            Uri.fromFile(File(archiveFile.path))
        }
    }

    var entries by remember { mutableStateOf<List<ArchiveEntryInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isEncrypted by remember { mutableStateOf(false) }
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    // Charset selection
    var selectedCharset by remember { mutableStateOf("UTF-8") }
    var showCharsetMenu by remember { mutableStateOf(false) }

    // Folder navigation state (breadcrumbs)
    var currentPath by remember { mutableStateOf("") } // "" = root
    val selectedIndices = remember { mutableStateMapOf<Int, Boolean>() }

    // Bottom Sheets
    var showPasswordSheet by remember { mutableStateOf(false) }
    var showExtractSheet by remember { mutableStateOf(false) }

    // Progress State (Docked Progress Card)
    var isRunningJob by remember { mutableStateOf(false) }
    var jobProgressPercent by remember { mutableStateOf(0) }
    var jobStatusText by remember { mutableStateOf("") }
    var jobCanCancel by remember { mutableStateOf(true) }
    var isJobCancelled by remember { mutableStateOf(false) }

    // Archive integrity test state
    var testIntegrityResult by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(true) }

    fun loadArchive(pwd: String? = null, charset: String = selectedCharset) {
        isLoading = true
        generalError = null
        passwordError = null
        coroutineScope.launch {
            try {
                val handler = ArchiveHandlerFactory.create(context, sourceUri)
                val enc = handler.isEncrypted(sourceUri)
                if (enc && pwd == null) {
                    isEncrypted = true
                    isUnlocked = false
                    isLoading = false
                    showPasswordSheet = true
                    return@launch
                }

                val list = handler.listEntries(sourceUri, pwd, charset)
                val hasEncryptedEntry = list.any { it.isEncrypted }
                if (hasEncryptedEntry && pwd == null) {
                    isEncrypted = true
                    isUnlocked = false
                    showPasswordSheet = true
                } else {
                    isEncrypted = enc || hasEncryptedEntry
                    isUnlocked = true
                    entries = list
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to read archive"
                if (msg.contains("password", ignoreCase = true) || msg.contains("encrypted", ignoreCase = true)) {
                    isEncrypted = true
                    isUnlocked = false
                    showPasswordSheet = true
                } else {
                    generalError = msg
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(archiveFile, selectedCharset) {
        loadArchive(enteredPassword.ifEmpty { null }, selectedCharset)
    }

    fun submitUnlock(pwd: String) {
        coroutineScope.launch {
            try {
                val handler = ArchiveHandlerFactory.create(context, sourceUri)
                val isValid = handler.testPassword(sourceUri, pwd)
                if (isValid) {
                    enteredPassword = pwd
                    isUnlocked = true
                    isEncrypted = true
                    showPasswordSheet = false
                    loadArchive(pwd, selectedCharset)
                } else {
                    passwordError = "Incorrect password. Please try again."
                }
            } catch (e: Exception) {
                passwordError = e.message ?: "Password verification failed"
            }
        }
    }

    // Run archive test / integrity check
    fun runIntegrityTest() {
        isRunningJob = true
        jobCanCancel = false
        jobProgressPercent = 0
        jobStatusText = "Testing archive integrity..."
        testIntegrityResult = null

        coroutineScope.launch {
            try {
                val handler = ArchiveHandlerFactory.create(context, sourceUri)
                val pass = handler.testArchive(sourceUri, enteredPassword.ifEmpty { null })
                if (pass) {
                    testIntegrityResult = "Archive integrity verified OK"
                    isTestSuccess = true
                    jobStatusText = "Integrity test passed"
                } else {
                    testIntegrityResult = "Integrity check failed: Archive corrupted"
                    isTestSuccess = false
                    jobStatusText = "Integrity test failed"
                }
            } catch (e: Exception) {
                testIntegrityResult = "Integrity error: ${e.message}"
                isTestSuccess = false
                jobStatusText = "Test error"
            } finally {
                isRunningJob = false
            }
        }
    }

    // Back handling: pop breadcrumb if deep, else close
    BackHandler {
        if (currentPath.isNotEmpty()) {
            val slashIdx = currentPath.lastIndexOf('/')
            currentPath = if (slashIdx != -1) currentPath.substring(0, slashIdx) else ""
        } else {
            onClose()
        }
    }

    // Compute display items in current folder level
    val currentDisplayItems = remember(entries, currentPath) {
        val dirMap = mutableMapOf<String, MutableList<ArchiveEntryInfo>>()
        val fileList = mutableListOf<ArchiveEntryInfo>()
        val prefix = if (currentPath.isEmpty()) "" else "$currentPath/"

        for (entry in entries) {
            val normalized = entry.name.replace('\\', '/').trimStart('/')
            if (normalized.isEmpty()) continue

            if (prefix.isNotEmpty() && !normalized.startsWith(prefix)) {
                continue
            }

            val relative = if (prefix.isEmpty()) normalized else normalized.substring(prefix.length)
            if (relative.isEmpty()) continue

            val slashIdx = relative.indexOf('/')
            if (slashIdx != -1) {
                val dirName = relative.substring(0, slashIdx)
                dirMap.getOrPut(dirName) { mutableListOf() }.add(entry)
            } else if (entry.isDirectory) {
                dirMap.getOrPut(relative) { mutableListOf() }.add(entry)
            } else {
                fileList.add(entry)
            }
        }

        val results = mutableListOf<ArchiveItemDisplay>()

        // Subdirectories first
        dirMap.keys.sorted().forEach { dirName ->
            val childEntries = dirMap[dirName] ?: emptyList()
            val dirFullPath = if (currentPath.isEmpty()) dirName else "$currentPath/$dirName"
            results.add(
                ArchiveItemDisplay(
                    name = dirName,
                    fullPath = dirFullPath,
                    isDirectory = true,
                    sizeBytes = childEntries.sumOf { it.sizeBytes },
                    isEncrypted = childEntries.any { it.isEncrypted },
                    itemsCount = childEntries.size
                )
            )
        }

        // Files next
        fileList.sortedBy { it.name.substringAfterLast('/') }.forEach { entry ->
            val fileName = entry.name.substringAfterLast('/')
            results.add(
                ArchiveItemDisplay(
                    name = fileName,
                    fullPath = entry.name,
                    isDirectory = false,
                    sizeBytes = entry.sizeBytes,
                    isEncrypted = entry.isEncrypted,
                    entryIndex = entry.index
                )
            )
        }

        results
    }

    val totalSize = remember(entries) { entries.sumOf { it.sizeBytes } }
    val cookieShape = rememberIconShape(lobes = 10, amplitude = 0.08f)

    BackHandler(enabled = true) {
        if (selectedIndices.isNotEmpty()) {
            selectedIndices.clear()
        } else if (currentPath.isNotEmpty()) {
            val slashIdx = currentPath.lastIndexOf('/')
            currentPath = if (slashIdx != -1) currentPath.substring(0, slashIdx) else ""
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        RippleBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // ====================================================================
            // 1. COOKIE-SHAPE HEADER (Expressive style matching rest of app)
            // ====================================================================
            PageHeader(
                title = archiveFile.name,
                subtitle = "${entries.size} items · ${formatSize(totalSize)} · ${formatSize(archiveFile.sizeBytes)} packed",
                onBack = {
                    haptics.tap()
                    if (selectedIndices.isNotEmpty()) {
                        selectedIndices.clear()
                    } else if (currentPath.isNotEmpty()) {
                        val slashIdx = currentPath.lastIndexOf('/')
                        currentPath = if (slashIdx != -1) currentPath.substring(0, slashIdx) else ""
                    } else {
                        onClose()
                    }
                },
                actions = {
                    // Charset Selector Button
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.card)
                                .clickable { showCharsetMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedCharset,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.text
                            )
                        }

                        DropdownMenu(
                            expanded = showCharsetMenu,
                            onDismissRequest = { showCharsetMenu = false }
                        ) {
                            SUPPORTED_ZIP_CHARSETS.forEach { cs ->
                                DropdownMenuItem(
                                    text = { Text(cs) },
                                    onClick = {
                                        selectedCharset = cs
                                        showCharsetMenu = false
                                        loadArchive(charset = cs)
                                    }
                                )
                            }
                        }
                    }

                    // Integrity Test Action Button
                    CookieIconButton(
                        onClick = {
                            haptics.tap()
                            runIntegrityTest()
                        },
                        icon = Icons.AutoMirrored.Outlined.Rule,
                        description = "Test archive",
                        bgColor = colors.card,
                        iconTint = colors.text,
                        size = 40.dp,
                        lobes = 8
                    )
                }
            )

            // ====================================================================
            // 2. INTERACTIVE BREADCRUMBS BAR
            // ====================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Root chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentPath.isEmpty()) colors.accent else colors.card)
                        .clickable {
                            haptics.tap()
                            currentPath = ""
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderZip,
                            contentDescription = null,
                            tint = if (currentPath.isEmpty()) colors.ink else colors.text,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = archiveFile.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (currentPath.isEmpty()) colors.ink else colors.text
                        )
                    }
                }

                if (currentPath.isNotEmpty()) {
                    val segments = currentPath.split("/").filter { it.isNotEmpty() }
                    var accumulated = ""
                    segments.forEachIndexed { idx, segment ->
                        accumulated = if (accumulated.isEmpty()) segment else "$accumulated/$segment"
                        val targetPath = accumulated
                        val isLast = idx == segments.lastIndex

                        Text(
                            text = "/",
                            fontSize = 12.sp,
                            color = colors.muted
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isLast) colors.accent else colors.card)
                                .clickable {
                                    haptics.tap()
                                    currentPath = targetPath
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = segment,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isLast) colors.ink else colors.text
                            )
                        }
                    }
                }
            }

            // Test Integrity Banner if tested
            AnimatedVisibility(
                visible = testIntegrityResult != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isTestSuccess) colors.accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isTestSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = if (isTestSuccess) colors.accent else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = testIntegrityResult ?: "",
                            fontSize = 12.sp,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { testIntegrityResult = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = colors.muted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ====================================================================
            // 3. SEPARATE CONTAINER FOR LIST OF ITEMS (SectionContainer matching app style)
            // ====================================================================
            SectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                } else if (generalError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = generalError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { loadArchive() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.card)
                            ) {
                                Text("Retry", color = colors.text)
                            }
                        }
                    }
                } else if (currentDisplayItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Folder is empty",
                            fontSize = 14.sp,
                            color = colors.muted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentDisplayItems, key = { it.fullPath }) { item ->
                            val childIndices = remember(item.fullPath, entries) {
                                if (item.isDirectory) {
                                    val prefix = item.fullPath.trimEnd('/') + "/"
                                    entries.filter { 
                                        !it.isDirectory && it.name.replace('\\', '/').trimStart('/').startsWith(prefix)
                                    }.map { it.index }
                                } else {
                                    if (item.entryIndex != -1) listOf(item.entryIndex) else emptyList()
                                }
                            }
                            val isSelected = remember(childIndices, selectedIndices.size) {
                                childIndices.isNotEmpty() && childIndices.all { selectedIndices.containsKey(it) }
                            }

                            fun toggleThisItem() {
                                if (childIndices.isEmpty()) return
                                val currentlyAllSelected = childIndices.all { selectedIndices.containsKey(it) }
                                if (currentlyAllSelected) {
                                    childIndices.forEach { selectedIndices.remove(it) }
                                } else {
                                    childIndices.forEach { selectedIndices[it] = true }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.card)
                                    .combinedClickable(
                                        onClick = {
                                            haptics.tap()
                                            if (selectedIndices.isNotEmpty()) {
                                                toggleThisItem()
                                            } else {
                                                if (item.isDirectory) {
                                                    currentPath = item.fullPath
                                                } else {
                                                    toggleThisItem()
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            haptics.tap()
                                            toggleThisItem()
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Cookie Item Icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(cookieShape)
                                        .background(
                                            if (item.isDirectory) colors.accent.copy(alpha = 0.2f)
                                            else colors.card
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (item.isDirectory) colors.accent else colors.text,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Name & Size
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (item.isDirectory) "${item.itemsCount} items · ${formatSize(item.sizeBytes)}"
                                        else formatSize(item.sizeBytes),
                                        fontSize = 12.sp,
                                        color = colors.muted
                                    )
                                }

                                // Encrypted lock indicator
                                if (item.isEncrypted) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = "Encrypted",
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Checkbox for selection (clickable on its own too)
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        haptics.tap()
                                        toggleThisItem()
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.accent,
                                        checkmarkColor = colors.ink
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Extract Action Button if no selection
            AnimatedVisibility(
                visible = selectedIndices.isEmpty() && !isRunningJob,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = {
                            haptics.tap()
                            showExtractSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Unarchive, contentDescription = null, tint = colors.ink)
                            Text(
                                text = "Extract All Files",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ink
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 4. DOCKED PROGRESS CARD (Shows active extract/test job)
        // ====================================================================
        AnimatedVisibility(
            visible = isRunningJob,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.container)
                    .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = jobStatusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                        if (jobCanCancel) {
                            TextButton(
                                onClick = { isJobCancelled = true }
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { jobProgressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = colors.accent,
                        trackColor = colors.card
                    )
                }
            }
        }

        // ====================================================================
        // 5. SELECTION BOTTOM SHEET (Slides up on long press or checkbox selection)
        // ====================================================================
        AnimatedVisibility(
            visible = selectedIndices.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut()
        ) {
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
                    .navigationBarsPadding()
                    .padding(top = 10.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered grab handle
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.muted.copy(alpha = 0.4f))
                )

                // Header Row: CookieIcon + title/subtitle + Select/Deselect All + Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pastel = ExpressiveTokens.categoryPastel("docs")
                    CookieIcon(
                        icon = Icons.Outlined.Checklist,
                        bgColor = pastel,
                        iconTint = ExpressiveTokens.OnPastel,
                        size = 46.dp,
                        lobes = 8
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedIndices.size} ${if (selectedIndices.size == 1) "file" else "files"} selected",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalAppFont.current,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val selectedSize = remember(selectedIndices.size) {
                            entries.filter { selectedIndices.containsKey(it.index) }.sumOf { it.sizeBytes }
                        }
                        Text(
                            text = formatSize(selectedSize),
                            fontSize = 12.sp,
                            color = colors.muted
                        )
                    }

                    val totalFiles = entries.count { !it.isDirectory }
                    val isAllSelected = selectedIndices.size >= totalFiles && totalFiles > 0
                    TextButton(
                        onClick = {
                            haptics.tap()
                            if (isAllSelected) {
                                selectedIndices.clear()
                            } else {
                                entries.filter { !it.isDirectory }.forEach { selectedIndices[it.index] = true }
                            }
                        }
                    ) {
                        Text(
                            text = if (isAllSelected) "Deselect" else "Select All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }

                    CookieIconButton(
                        onClick = {
                            haptics.tap()
                            selectedIndices.clear()
                        },
                        icon = Icons.Default.Close,
                        bgColor = colors.card,
                        iconTint = colors.text,
                        description = "Clear selection",
                        size = 36.dp,
                        iconSize = 18.dp,
                        lobes = 8
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions Row: Primary "Extract Selected" button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptics.tap()
                            showExtractSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.ink
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Unarchive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Extract Selected (${selectedIndices.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // ====================================================================
        // 6. BOTTOM SHEETS (Extract Sheet & Password Sheet)
        // ====================================================================
        if (showExtractSheet) {
            ArchiveExtractBottomSheet(
                archiveName = archiveFile.name,
                defaultCharset = selectedCharset,
                isEncrypted = isEncrypted,
                enteredPassword = enteredPassword,
                onDismiss = { showExtractSheet = false },
                onConfirmExtract = { pwd, chosenCharset ->
                    showExtractSheet = false
                    val indicesToExtract = if (selectedIndices.isNotEmpty()) selectedIndices.keys.toList() else null
                    onExtractRequest(pwd ?: enteredPassword.ifEmpty { null }, indicesToExtract)
                }
            )
        }

        if (showPasswordSheet) {
            ArchivePasswordBottomSheet(
                error = passwordError,
                onDismiss = { showPasswordSheet = false },
                onSubmit = { pwd -> submitUnlock(pwd) }
            )
        }
    }
}

/**
 * Extract Bottom Sheet matching mockup:
 * Offers Destination (Here, New Folder, Choose Folder),
 * Character Encoding, and Overwrite Policy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveExtractBottomSheet(
    archiveName: String,
    defaultCharset: String,
    isEncrypted: Boolean,
    enteredPassword: String,
    onDismiss: () -> Unit,
    onConfirmExtract: (password: String?, charset: String) -> Unit
) {
    val colors = ExpressiveTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var destinationMode by remember { mutableStateOf(ExtractDestinationMode.NEW_FOLDER) }
    var charset by remember { mutableStateOf(defaultCharset) }
    var password by remember { mutableStateOf(enteredPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.muted.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Unarchive,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Extract Archive",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = archiveName,
                        fontSize = 12.sp,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Destination options
            Text(
                text = "Destination",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ExtractDestinationMode.NEW_FOLDER to "New Folder",
                    ExtractDestinationMode.HERE to "Here",
                    ExtractDestinationMode.CHOOSE_FOLDER to "Choose..."
                ).forEach { (mode, label) ->
                    val isSelected = destinationMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.accent else colors.card)
                            .clickable { destinationMode = mode }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) colors.ink else colors.text
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Character Encoding Chips
            Text(
                text = "Character Encoding",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SUPPORTED_ZIP_CHARSETS.forEach { cs ->
                    val isSelected = charset == cs
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.accent else colors.card)
                            .clickable { charset = cs }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cs,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) colors.ink else colors.text
                        )
                    }
                }
            }

            // Password Field if encrypted
            if (isEncrypted) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Archive Password",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = colors.muted
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Extract Confirm Button
            Button(
                onClick = { onConfirmExtract(password.ifEmpty { null }, charset) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text(
                    text = "Extract Now",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                )
            }
        }
    }
}

/**
 * Dedicated Password Bottom Sheet for encrypted archives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivePasswordBottomSheet(
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (password: String) -> Unit
) {
    val colors = ExpressiveTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = colors.container,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.muted.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Password Protected",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = "Enter password to view and extract contents",
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (password.isNotEmpty()) onSubmit(password) }),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = colors.muted
                        )
                    }
                },
                isError = error != null,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = { if (password.isNotEmpty()) onSubmit(password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text(
                    text = "Unlock Archive",
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                )
            }
        }
    }
}
