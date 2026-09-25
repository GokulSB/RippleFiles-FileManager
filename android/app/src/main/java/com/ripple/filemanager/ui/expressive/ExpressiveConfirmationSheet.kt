package com.ripple.filemanager.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.ui.theme.LocalAppFont

/**
 * Expressive bottom sheet for destructive actions (e.g. Delete, Empty bin).
 * Matches the cookie-shape/pill/sheet design system (Create, Sort, Info sheets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveConfirmationSheet(
    title: String,
    subtitle: String? = null,
    confirmLabel: String = "Delete",
    cancelLabel: String = "Cancel",
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ExpressiveTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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

            // Title: Outfit 500 20sp, left-aligned, normal case, colors.accent
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = LocalAppFont.current,
                color = colors.accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            // Subtitle: muted colour, normal weight, below title
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Two side-by-side pill buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel: card-colour fill, text-colour label (secondary style)
                Button(
                    onClick = onDismissRequest,
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
                        text = cancelLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalAppFont.current
                    )
                }

                // Delete / Confirm: accent-colour fill, ink-colour label, 600 weight (primary style)
                Button(
                    onClick = onConfirm,
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
                        text = confirmLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalAppFont.current
                    )
                }
            }
        }
    }
}

/**
 * Expressive centered dialog option matching the expressive design system.
 */
@Composable
fun ExpressiveConfirmationDialog(
    title: String,
    subtitle: String? = null,
    confirmLabel: String = "Delete",
    cancelLabel: String = "Cancel",
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val roundness = LocalCornerRoundness.current
    val dialogShape = RoundedCornerShape((28f * (roundness * 2).coerceIn(0.5f, 1.5f)).dp)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = dialogShape,
        containerColor = colors.container,
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = LocalAppFont.current,
                color = colors.accent
            )
        },
        text = if (!subtitle.isNullOrBlank()) {
            {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = LocalAppFont.current,
                    color = colors.muted
                )
            }
        } else null,
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.ink
                )
            ) {
                Text(
                    text = confirmLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalAppFont.current
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.card,
                    contentColor = colors.text
                )
            ) {
                Text(
                    text = cancelLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalAppFont.current
                )
            }
        }
    )
}
