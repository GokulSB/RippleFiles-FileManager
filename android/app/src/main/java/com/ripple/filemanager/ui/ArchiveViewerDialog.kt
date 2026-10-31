package com.ripple.filemanager.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.FileItem
import com.ripple.filemanager.R
import com.ripple.filemanager.archive.ArchiveEntryInfo
import com.ripple.filemanager.archive.ArchiveHandlerFactory
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerDialog(
    archiveFile: FileItem,
    onDismiss: () -> Unit,
    onExtractRequest: (password: String?) -> Unit
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
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isTestingPassword by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    fun loadArchive(pwd: String? = null) {
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
                    return@launch
                }

                val list = handler.listEntries(sourceUri, pwd)
                val hasEncryptedEntry = list.any { it.isEncrypted }
                if (hasEncryptedEntry && pwd == null) {
                    isEncrypted = true
                    isUnlocked = false
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
                } else {
                    generalError = msg
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(archiveFile) {
        loadArchive()
    }

    fun submitUnlock() {
        if (enteredPassword.isEmpty()) {
            passwordError = "Please enter the password"
            return
        }
        isTestingPassword = true
        passwordError = null
        coroutineScope.launch {
            try {
                val handler = ArchiveHandlerFactory.create(context, sourceUri)
                val isValid = handler.testPassword(sourceUri, enteredPassword)
                if (isValid) {
                    try {
                        val list = handler.listEntries(sourceUri, enteredPassword)
                        entries = list
                        isUnlocked = true
                        isEncrypted = true
                    } catch (e: Exception) {
                        passwordError = "Failed to list contents: ${e.message}"
                    }
                } else {
                    passwordError = "Incorrect password. Please try again."
                }
            } catch (e: Exception) {
                passwordError = e.message ?: "Password verification failed"
            } finally {
                isTestingPassword = false
            }
        }
    }

    val borderColor = colors.line.copy(alpha = colors.lineAlpha)

    com.ripple.filemanager.ui.GradientAlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEncrypted && !isUnlocked) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Encrypted",
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = archiveFile.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else if (generalError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = generalError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (isEncrypted && !isUnlocked) {
                    // Password Prompt View
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "This archive is protected by a password. Enter password to view its contents.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted
                        )

                        OutlinedTextField(
                            value = enteredPassword,
                            onValueChange = {
                                enteredPassword = it
                                passwordError = null
                            },
                            label = { Text("Archive Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitUnlock() }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = colors.muted
                                    )
                                }
                            },
                            isError = passwordError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = borderColor,
                                focusedLabelColor = colors.accent,
                                cursorColor = colors.accent,
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (passwordError != null) {
                            Text(
                                text = passwordError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (isTestingPassword) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.accent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Verifying password...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }
                        }
                    }
                } else {
                    // Contents View
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contents (${entries.size} files):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                        if (isEncrypted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Unlocked",
                                    tint = colors.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Unlocked",
                                    fontSize = 11.sp,
                                    color = colors.accent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Archive is empty",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 320.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.card.copy(alpha = 0.5f))
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            items(entries) { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (entry.isDirectory) colors.accent else colors.muted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!entry.isDirectory && entry.sizeBytes > 0) {
                                        Text(
                                            text = formatSize(entry.sizeBytes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEncrypted && !isUnlocked) {
                Button(
                    onClick = {
                        haptics.tap()
                        submitUnlock()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink
                    ),
                    enabled = !isTestingPassword && enteredPassword.isNotEmpty()
                ) {
                    Text("Unlock & View")
                }
            } else {
                Button(
                    onClick = {
                        haptics.tap()
                        onExtractRequest(if (isEncrypted) enteredPassword else null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink
                    ),
                    enabled = !isLoading && generalError == null
                ) {
                    Text("Extract All")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    haptics.tap()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.close), color = colors.muted)
            }
        }
    )
}
