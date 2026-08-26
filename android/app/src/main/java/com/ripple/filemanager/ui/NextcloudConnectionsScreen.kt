package com.ripple.filemanager.ui

import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.theme.JetBrainsMonoFamily
import com.ripple.filemanager.ui.theme.SkylineColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.ConnectionStatus
import com.ripple.filemanager.WebDavState
import com.ripple.filemanager.data.webdav.WebDavConnection
import com.ripple.filemanager.data.webdav.buildNextcloudUrl
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextcloudConnectionsDialog(
    state: WebDavState,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var showAddForm by remember { mutableStateOf(false) }

    // Filter to only show Nextcloud connections
    val nextcloudConnections = state.savedConnections.filter { it.isNextcloud }

    if (showAddForm) {
        NextcloudFormDialog(
            onSave = { connection, password ->
                onAction(AppAction.WebDavAction.AddConnection(connection, password))
                showAddForm = false
            },
            onDismiss = { showAddForm = false }
        )
    } else {
        com.ripple.filemanager.ui.GradientAlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Nextcloud Connections", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (nextcloudConnections.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_saved_connections),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
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
                    }
                }
            },
            confirmButton = {
                Button(onClick = { haptics.tap(); showAddForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_content_desc))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.new_connection))
                }
            },
            dismissButton = {
                TextButton(onClick = { haptics.tap(); onDismiss() }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
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
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.displayName, style = MaterialTheme.typography.titleMedium)
                Text(connection.serverUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (isActive) {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = status.name,
                    tint = when(status) {
                        ConnectionStatus.Connected -> SkylineColors.Amber
                        ConnectionStatus.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(12.dp)
                )
            }

            Box {
                IconButton(onClick = { haptics.tap(); showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (isActive) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stop_connection), color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                onStop()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.connect_action), color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_label), color = MaterialTheme.colorScheme.error) },
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
    var domain by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    com.ripple.filemanager.ui.GradientAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Nextcloud") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Server domain") },
                    placeholder = { Text("cloud.example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / App password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Tip: For accounts with 2FA, use an app password.\nGenerate one at Settings \u2192 Security \u2192 Devices & sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.tap()
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
                }
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = { haptics.tap(); onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
