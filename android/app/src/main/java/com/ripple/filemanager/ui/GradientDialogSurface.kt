package com.ripple.filemanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.ripple.filemanager.ui.expressive.ExpressiveTheme

@Composable
fun GradientDialogSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ExpressiveTheme.colors
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), shape),
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
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = Color.Unspecified
) {
    val colors = ExpressiveTheme.colors
    val effectiveBg = if (containerColor != Color.Unspecified) containerColor else colors.card
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
        confirmButton = confirmButton,
        modifier = modifier
            .clip(shape)
            .border(1.dp, colors.line.copy(alpha = colors.lineAlpha), shape),
        dismissButton = dismissButton,
        title = title,
        text = text,
        icon = icon,
        shape = shape,
        containerColor = effectiveBg,
        titleContentColor = colors.text,
        textContentColor = colors.muted
    )
}
