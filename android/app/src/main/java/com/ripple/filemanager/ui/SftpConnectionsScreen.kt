package com.ripple.filemanager.ui

import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R
import com.ripple.filemanager.ui.theme.JetBrainsMonoFamily
import com.ripple.filemanager.ui.theme.SkylineColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.ConnectionStatus
import com.ripple.filemanager.SftpState
import com.ripple.filemanager.data.sftp.SftpConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpConnectionsDialog(
    state: SftpState,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var showAddForm by remember { mutableStateOf(false) }

    if (showAddForm) {
        SftpFormDialog(
            onSave = { connection, password ->
                onAction(AppAction.SftpAction.AddConnection(connection, password))
                showAddForm = false
            },
            onDismiss = { showAddForm = false }
        )
    } else {
        com.ripple.filemanager.ui.GradientAlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(R.string.sftp_connections_title), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.savedConnections.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_saved_connections),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(state.savedConnections) { conn ->
                                SftpConnectionItem(
                                    connection = conn,
                                    isActive = conn.id == state.activeConnectionId,
                                    status = state.connectionStatus,
                                    onClick = {
                                        onAction(AppAction.SftpAction.Connect(conn.id))
                                        onDismiss()
                                    },
                                    onDelete = { onAction(AppAction.SftpAction.DeleteConnection(conn.id)) },
                                    onStop = { onAction(AppAction.SftpAction.Disconnect(conn.id)) }
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
fun SftpConnectionItem(
    connection: SftpConnection,
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
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.displayName, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.sftp_connection_details, connection.host, connection.port), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
