package com.ripple.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.ConnectionStatus
import com.ripple.filemanager.R
import com.ripple.filemanager.WebDavState
import com.ripple.filemanager.data.webdav.WebDavConnection
import com.ripple.filemanager.data.webdav.buildNextcloudUrl
import com.ripple.filemanager.ui.expressive.CookieIcon
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens
import com.ripple.filemanager.ui.theme.LocalAppFont
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextcloudConnectionsDialog(
    state: WebDavState,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val colors = ExpressiveTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val nextcloudConnections = remember(state.savedConnections) {
        state.savedConnections.filter { it.isNextcloud }
    }
    var showAddForm by remember { mutableStateOf(nextcloudConnections.isEmpty()) }

    if (showAddForm) {
        NextcloudFormDialog(
            onSave = { connection, password ->
                onAction(AppAction.WebDavAction.AddConnection(connection, password))
                showAddForm = false
                onDismiss()
            },
            onDismiss = {
                if (nextcloudConnections.isEmpty()) {
                    onDismiss()
                } else {
                    showAddForm = false
                }
            }
        )
    } else {
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
                    .background(colors.container)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Grab handle centered at top
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 16.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.muted.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )

                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pastel = ExpressiveTokens.categoryPastel("downloads")
                    CookieIcon(
                        icon = Icons.Outlined.CloudSync,
                        bgColor = pastel,
                        size = 46.dp,
                        lobes = 8
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nextcloud Connections",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalAppFont.current,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${nextcloudConnections.size} saved connections",
                            fontSize = 13.sp,
                            fontFamily = LocalAppFont.current,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Saved Connections List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nextcloudConnections) { conn ->
                        NextcloudConnectionItem(
                            connection = conn,
                            isActive = conn.id == state.activeConnectionId,
                            status = state.connectionStatus,
                            onClick = {
                                onAction(AppAction.WebDavAction.Connect(conn.id))
                                onDismiss()
                            },
                            onDelete = { onAction(AppAction.WebDavAction.DeleteConnection(conn.id)) },
                            onStop = { onAction(AppAction.WebDavAction.Disconnect(conn.id)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            haptics.tap()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.card,
                            contentColor = colors.text
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.close),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalAppFont.current
                        )
                    }

                    Button(
                        onClick = {
                            haptics.tap()
                            showAddForm = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.ink
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.new_connection),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalAppFont.current
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NextcloudConnectionItem(
    connection: WebDavConnection,
    isActive: Boolean,
    status: ConnectionStatus,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onStop: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val colors = ExpressiveTheme.colors
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CloudSync,
                contentDescription = null,
                tint = if (isActive) colors.accent else colors.muted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = connection.serverUrl,
                    fontSize = 12.sp,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                ConnectionStatus.Connected -> colors.accent
                                ConnectionStatus.Error -> MaterialTheme.colorScheme.error
                                else -> colors.muted
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box {
                IconButton(onClick = { haptics.tap(); showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = colors.muted
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (isActive) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stop_connection), fontFamily = LocalAppFont.current) },
                            onClick = {
                                showMenu = false
                                onStop()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.connect_action), fontFamily = LocalAppFont.current) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_label), color = MaterialTheme.colorScheme.error, fontFamily = LocalAppFont.current) },
                        onClick = {
                            haptics.delete()
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextcloudFormDialog(
    onSave: (WebDavConnection, String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val colors = ExpressiveTheme.colors
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var domain by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                .background(colors.container)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Grab handle centered at top
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.muted.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Row: Cookie Icon + Title & Subtitle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pastel = ExpressiveTokens.categoryPastel("downloads")
                CookieIcon(
                    icon = Icons.Outlined.CloudSync,
                    bgColor = pastel,
                    size = 46.dp,
                    lobes = 8
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add Nextcloud",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalAppFont.current,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Connect to your Nextcloud instance",
                        fontSize = 13.sp,
                        fontFamily = LocalAppFont.current,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scrollable Form Fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontFamily = LocalAppFont.current
                    )
                }

                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Server domain", fontFamily = LocalAppFont.current) },
                    placeholder = { Text("cloud.example.com", fontFamily = LocalAppFont.current, color = colors.muted.copy(alpha = 0.6f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.card,
                        unfocusedContainerColor = colors.card,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedLabelColor = colors.accent,
                        unfocusedLabelColor = colors.muted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username_label), fontFamily = LocalAppFont.current) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.card,
                        unfocusedContainerColor = colors.card,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedLabelColor = colors.accent,
                        unfocusedLabelColor = colors.muted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / App password", fontFamily = LocalAppFont.current) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = colors.muted
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.card,
                        unfocusedContainerColor = colors.card,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.line.copy(alpha = colors.lineAlpha),
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedLabelColor = colors.accent,
                        unfocusedLabelColor = colors.muted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.card)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Tip: For accounts with 2FA, use an app password.\nGenerate one at Settings \u2192 Security \u2192 Devices & sessions.",
                        fontSize = 12.sp,
                        fontFamily = LocalAppFont.current,
                        color = colors.muted,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two side-by-side pill buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        haptics.tap()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.card,
                        contentColor = colors.text
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalAppFont.current
                    )
                }

                Button(
                    onClick = {
                        haptics.tap()
                        focusManager.clearFocus()
                        if (domain.isBlank() || username.isBlank() || password.isBlank()) {
                            error = "Please fill in all fields"
                            return@Button
                        }
                        val url = buildNextcloudUrl(domain, username)
                        val connection = WebDavConnection(
                            id = UUID.randomUUID().toString(),
                            displayName = domain.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                            serverUrl = url,
                            username = username,
                            isNextcloud = true,
                            allowSelfSignedCert = false
                        )
                        onSave(connection, password)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.ink
                    )
                ) {
                    Text(
                        text = "Connect",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalAppFont.current
                    )
                }
            }
        }
    }
}
