package com.ripple.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.ConnectionStatus
import com.ripple.filemanager.R
import com.ripple.filemanager.SmbState
import com.ripple.filemanager.data.smb.SmbConnection
import com.ripple.filemanager.ui.expressive.CookieIcon
import com.ripple.filemanager.ui.expressive.ExpressiveTheme
import com.ripple.filemanager.ui.expressive.ExpressiveTokens
import com.ripple.filemanager.ui.theme.LocalAppFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbConnectionsDialog(
    state: SmbState,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    val colors = ExpressiveTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddForm by remember { mutableStateOf(state.savedConnections.isEmpty()) }

    if (showAddForm) {
        SmbFormDialog(
            onSave = { connection, password ->
                onAction(AppAction.SmbAction.AddConnection(connection, password))
                showAddForm = false
                onDismiss()
            },
            onDismiss = {
                if (state.savedConnections.isEmpty()) {
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
                    val pastel = ExpressiveTokens.categoryPastel("large")
                    CookieIcon(
                        icon = Icons.Outlined.Dns,
                        bgColor = pastel,
                        size = 46.dp,
                        lobes = 8
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.smb_connections_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalAppFont.current,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${state.savedConnections.size} saved connections",
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
fun SmbConnectionItem(
    connection: SmbConnection,
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
                Icons.Outlined.Dns,
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
                    text = stringResource(R.string.smb_connection_details, connection.host, connection.port, connection.shareName),
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
