package com.ripple.filemanager.ui.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripple.filemanager.AppAction
import com.ripple.filemanager.AppState

/**
 * Expressive Drawer:
 * - 86% width (max 340dp), bg colour + glow
 * - Top-right ✕ cookie button
 * - Brand card (accent cookie folder icon, "Ripple Files", "Every file, in flow.")
 * - Connections card that expands to horizontally scrolling chips (GDrive, Mega, Dropbox, Nextcloud, WebDAV, SFTP, FTP, SMB)
 * - Cards: Storage cleaner, Settings, About (cookie icon + label + chevron)
 */
@Composable
fun ExpressiveDrawerContent(
    state: AppState,
    onAction: (AppAction) -> Unit,
    onCloseDrawer: () -> Unit,
    onShowAbout: () -> Unit,
    onConnectGoogleDrive: () -> Unit,
    onConnectMega: () -> Unit,
    onConnectDropbox: () -> Unit,
    onConnectNextcloud: () -> Unit,
    onConnectWebDav: () -> Unit,
    onConnectSftp: () -> Unit,
    onConnectFtp: () -> Unit,
    onConnectSmb: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    var connectionsExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = colors.bg,
        drawerContentColor = colors.text,
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 340.dp)
            .fillMaxWidth(0.86f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top-right ✕ cookie button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CookieIconButton(
                    onClick = onCloseDrawer,
                    icon = Icons.Default.Close,
                    bgColor = colors.card,
                    description = "Close drawer",
                    iconTint = colors.text,
                    size = 42.dp,
                    lobes = 8
                )
            }

            // Brand Card
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CookieIcon(
                        icon = Icons.Outlined.Folder,
                        bgColor = colors.accent,
                        iconTint = colors.ink,
                        size = 54.dp,
                        lobes = 10
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Ripple Files",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = "Every file, in flow.",
                            fontSize = 13.sp,
                            color = colors.muted
                        )
                    }
                }
            }

            // Connections Card (expandable to horizontally scrolling chips)
            val rotation by animateFloatAsState(
                targetValue = if (connectionsExpanded) 180f else 0f,
                animationSpec = tween(200),
                label = "chevron_rot"
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { connectionsExpanded = !connectionsExpanded }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CookieIcon(
                                icon = Icons.Outlined.CloudSync,
                                bgColor = ExpressiveTokens.PastelTeal,
                                size = 40.dp,
                                lobes = 8
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Connections",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.rotate(rotation)
                        )
                    }

                    AnimatedVisibility(visible = connectionsExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DrawerConnectionChip("GDrive", onConnectGoogleDrive)
                                DrawerConnectionChip("Mega", onConnectMega)
                                DrawerConnectionChip("Dropbox", onConnectDropbox)
                                DrawerConnectionChip("Nextcloud", onConnectNextcloud)
                                DrawerConnectionChip("WebDAV", onConnectWebDav)
                                DrawerConnectionChip("SFTP", onConnectSftp)
                                DrawerConnectionChip("FTP", onConnectFtp)
                                DrawerConnectionChip("SMB", onConnectSmb)
                            }
                        }
                    }
                }
            }

            // Cards: Storage cleaner, Settings, About
            DrawerActionCard(
                title = "Storage cleaner",
                subtitle = "Free up device space",
                icon = Icons.Outlined.CleaningServices,
                category = "docs",
                onClick = {
                    onAction(AppAction.SetCleanerScreenVisible(true))
                    onCloseDrawer()
                }
            )

            DrawerActionCard(
                title = "Settings",
                subtitle = "Theme, security & layout",
                icon = Icons.Outlined.Settings,
                category = "archives",
                onClick = {
                    onAction(AppAction.SetShowSettingsScreen(true))
                    onCloseDrawer()
                }
            )

            DrawerActionCard(
                title = "About",
                subtitle = "App info, license & links",
                icon = Icons.Outlined.Info,
                category = "large",
                onClick = {
                    onShowAbout()
                    onCloseDrawer()
                }
            )
        }
    }
}

@Composable
private fun DrawerConnectionChip(
    name: String,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.insetCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text
        )
    }
}

@Composable
private fun DrawerActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    category: String,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val pastel = ExpressiveTokens.categoryPastel(category)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CookieIcon(
                icon = icon,
                bgColor = pastel,
                size = 44.dp,
                lobes = cookieLobesForName(title)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.muted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted
            )
        }
    }
}
