package com.ripple.filemanager.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun GradientDialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(0.dp)) // match Skyline Ledger 0dp radius
            .appGradientBackground()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(0.dp)),
        content = content
    )
}


@Composable
fun GradientAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties(),
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp),
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
        confirmButton = confirmButton,
        modifier = modifier
            .clip(shape)
            .appGradientBackground()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), shape),
        dismissButton = dismissButton,
        title = title,
        text = text,
        icon = icon,
        shape = shape,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
