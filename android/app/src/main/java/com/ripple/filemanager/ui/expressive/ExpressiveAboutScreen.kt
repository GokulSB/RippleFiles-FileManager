package com.ripple.filemanager.ui.expressive

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpressiveAboutScreen(
    appName: String = "Ripple Files",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current
    var showLicensesSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        PageHeader(
            title = "About",
            onBack = onClose
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Card
            SectionContainer {
                InnerCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CookieIcon(
                            icon = Icons.Outlined.Folder,
                            bgColor = colors.accent,
                            iconTint = colors.ink,
                            size = 72.dp,
                            lobes = 10
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = appName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = "Every file, in flow.",
                            fontSize = 14.sp,
                            color = colors.muted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.insetCard)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Version ${com.ripple.filemanager.BuildConfig.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.muted
                            )
                        }
                    }
                }
            }

            // Description Section
            SectionContainer {
                InnerCard {
                    Text(
                        text = "$appName is a modern, expressive file manager built for Android. It combines intuitive file navigation, smart categorization, built-in media viewers, cloud storage integration, and customizable themes.",
                        fontSize = 14.sp,
                        color = colors.text,
                        lineHeight = 22.sp
                    )
                }
            }

            // Links Section: Website, Privacy Policy, Licences, Feedback
            SectionTitle(title = "Links & Support")

            SectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutLinkRow(
                        title = "Website",
                        subtitle = "ripplefiles.app",
                        icon = Icons.Outlined.Language,
                        category = "docs",
                        onClick = {
                            openUrl(context, "https://github.com/GokulSB/RippleFiles-FileManager")
                        }
                    )

                    AboutLinkRow(
                        title = "Privacy policy",
                        subtitle = "How we protect your data",
                        icon = Icons.Outlined.Policy,
                        category = "apps",
                        onClick = {
                            openUrl(context, "https://github.com/GokulSB/RippleFiles-FileManager/blob/main/PRIVACY.md")
                        }
                    )

                    AboutLinkRow(
                        title = "Open source licenses",
                        subtitle = "libVLC, 7-Zip, zstd, lz4, lz5, lizard & others",
                        icon = Icons.Outlined.Code,
                        category = "archives",
                        onClick = {
                            showLicensesSheet = true
                        }
                    )

                    AboutLinkRow(
                        title = "Feedback & support",
                        subtitle = "gokulsb009@gmail.com",
                        icon = Icons.Outlined.Email,
                        category = "images",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:gokulsb009@gmail.com")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                    )
                }
            }

            // Footer
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Made with ❤ in India",
                    fontSize = 13.sp,
                    color = colors.muted
                )
                Text(
                    text = "© 2025 GokuCruz. All rights reserved.",
                    fontSize = 11.sp,
                    color = colors.muted.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(130.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }

        if (showLicensesSheet) {
            OpenSourceLicensesBottomSheet(
                onDismiss = { showLicensesSheet = false },
                onOpenUrl = { url -> openUrl(context, url) }
            )
        }
    }
}

private data class LibraryLicense(
    val name: String,
    val license: String,
    val description: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenSourceLicensesBottomSheet(
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val colors = ExpressiveTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val libraries = remember {
        listOf(
            LibraryLicense(
                name = "libVLC",
                license = "GNU LGPL v2.1+",
                description = "High-performance multimedia player framework & audio/video engine",
                url = "https://www.videolan.org/vlc/libvlc.html"
            ),
            LibraryLicense(
                name = "7-Zip / 7-Zip-zstd",
                license = "GNU LGPL v2.1+ (unRAR restriction)",
                description = "High-ratio multi-format archive engine (7z, ISO, WIM, TAR, etc.)",
                url = "https://github.com/mcmilk/7-Zip-zstd"
            ),
            LibraryLicense(
                name = "Zstandard (zstd)",
                license = "BSD 3-Clause / GPLv2",
                description = "Real-time compression algorithm developed by Meta",
                url = "https://github.com/facebook/zstd"
            ),
            LibraryLicense(
                name = "LZ4",
                license = "BSD 2-Clause",
                description = "Extremely fast lossless compression algorithm",
                url = "https://github.com/lz4/lz4"
            ),
            LibraryLicense(
                name = "LZ5",
                license = "BSD 2-Clause",
                description = "High throughput compression algorithm variant",
                url = "https://github.com/inikep/lz5"
            ),
            LibraryLicense(
                name = "Lizard (formerly LZ5)",
                license = "BSD 2-Clause",
                description = "Fast decompression and high compression ratio library",
                url = "https://github.com/inikep/lizard"
            ),
            LibraryLicense(
                name = "Apache Commons Compress",
                license = "Apache License 2.0",
                description = "Java API for working with compressed and archive formats",
                url = "https://commons.apache.org/proper/commons-compress/"
            ),
            LibraryLicense(
                name = "Zip4j",
                license = "Apache License 2.0",
                description = "Java library for zip files with AES-256 encryption support",
                url = "https://github.com/srikanth-lingala/zip4j"
            ),
            LibraryLicense(
                name = "Unrar5j",
                license = "GNU LGPL v3.0 (unRAR restriction)",
                description = "Pure Java RAR and RAR5 extraction library",
                url = "https://github.com/edmund-wiles/unrar5j"
            ),
            LibraryLicense(
                name = "SMBJ",
                license = "Apache License 2.0",
                description = "SMB2 and SMB3 client library for network storage",
                url = "https://github.com/hierynomus/smbj"
            ),
            LibraryLicense(
                name = "SSHJ",
                license = "Apache License 2.0",
                description = "SSHv2 and SFTP library for secure remote connections",
                url = "https://github.com/hierynomus/sshj"
            )
        )
    }

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
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.muted.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Open Source Licenses",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = "Third-party libraries & engines powering Ripple Files",
                        fontSize = 13.sp,
                        color = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                libraries.forEach { lib ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.card)
                            .clickable { onOpenUrl(lib.url) }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = lib.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.text
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.accent.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = lib.license,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.accent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lib.description,
                                    fontSize = 12.sp,
                                    color = colors.muted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lib.url,
                                    fontSize = 11.sp,
                                    color = colors.accent,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open link",
                                tint = colors.muted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    category: String,
    onClick: () -> Unit
) {
    val colors = ExpressiveTheme.colors
    val pastel = ExpressiveTokens.categoryPastel(category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CookieIcon(
            icon = icon,
            bgColor = pastel,
            size = 42.dp,
            lobes = cookieLobesForName(title)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
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
            tint = colors.muted,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {}
}
