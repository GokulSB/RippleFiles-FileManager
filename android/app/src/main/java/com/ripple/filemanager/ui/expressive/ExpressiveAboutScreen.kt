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
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ExpressiveTheme.colors
    val context = LocalContext.current

    RippleBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                            text = "Ripple Files",
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
                                text = "Version 1.0.12",
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
                        text = "Ripple Files is a modern, expressive file manager built for Android. It combines intuitive file navigation, smart categorization, built-in media viewers, cloud storage integration, and customizable themes.",
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
                        subtitle = "Third-party libraries & tools",
                        icon = Icons.Outlined.Code,
                        category = "archives",
                        onClick = {
                            openUrl(context, "https://github.com/GokulSB/RippleFiles-FileManager/blob/main/LICENSE")
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

            Spacer(modifier = Modifier.height(130.dp))
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
