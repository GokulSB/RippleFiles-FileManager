package com.ripple.filemanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.ConnectionStatus
import com.ripple.filemanager.SmbState
import com.ripple.filemanager.data.smb.SmbConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbConnectionsDialog(
    state: SmbState,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }

    if (showAddForm) {
        SmbFormDialog(
            onSave = { connection, password ->
                onAction(AppAction.SmbAction.AddConnection(connection, password))
                showAddForm = false
            },
            onDismiss = { showAddForm = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("SMB Connections", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.savedConnections.isEmpty()) {
                        Text(
                            "No saved connections",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(state.savedConnections) { conn ->
                                SmbConnectionItem(
                                    connection = conn,
                                    isActive = conn.id == state.activeConnectionId,
                                    status = state.connectionStatus,
                                    onClick = {
                                        onAction(AppAction.SmbAction.Connect(conn.id))
                                        onDismiss()
                                    },
                                    onDelete = { onAction(AppAction.SmbAction.DeleteConnection(conn.id)) },
                                    onStop = { onAction(AppAction.SmbAction.Disconnect(conn.id)) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAddForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Connection")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SmbConnectionItem(
    connection: SmbConnection,
    isActive: Boolean,
    status: ConnectionStatus,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onStop: () -> Unit
) {
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
            Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.displayName, style = MaterialTheme.typography.titleMedium)
                Text("${connection.host}:${connection.port} / ${connection.shareName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isActive) {
                Icon(
                    Icons.Default.Circle, 
                    contentDescription = status.name, 
                    tint = when(status) {
                        ConnectionStatus.Connected -> com.ripple.filemanager.ui.theme.SkylineColors.Amber
                        ConnectionStatus.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(12.dp)
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (isActive) {
                        DropdownMenuItem(
                            text = { Text("Stop", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                onStop()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Connect", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
