package com.ripple.filemanager.ui

import androidx.compose.ui.res.stringResource
import com.ripple.filemanager.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.data.ftp.FtpConnection
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FtpFormDialog(
    onSave: (FtpConnection, String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = com.ripple.filemanager.haptics.LocalHaptics.current
    var displayName by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("21") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var useFtps by remember { mutableStateOf(false) }

    var error by remember { mutableStateOf<String?>(null) }

    com.ripple.filemanager.ui.GradientAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_ftp_connection_title)) },
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
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.display_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.host_ip_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text(stringResource(R.string.port_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = useFtps, onCheckedChange = { useFtps = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.use_ftps_label), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (host.isBlank() || username.isBlank()) {
                        error = "Host and Username are required."
                        return@Button
                    }
                    val portNum = port.toIntOrNull() ?: 21

                    val conn = FtpConnection(
                        id = UUID.randomUUID().toString(),
                        displayName = displayName.ifBlank { host },
                        host = host,
                        port = portNum,
                        username = username,
                        useFtps = useFtps,
                        savedAt = System.currentTimeMillis()
                    )
                    onSave(conn, password)
                }
            ) {
                Text(stringResource(R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = { haptics.tap(); onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
