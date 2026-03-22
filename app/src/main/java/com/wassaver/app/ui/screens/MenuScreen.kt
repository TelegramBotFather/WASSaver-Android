package com.wassaver.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wassaver.app.data.UpdateChecker
import com.wassaver.app.ui.theme.WhatsAppDarkGreen
import com.wassaver.app.ui.theme.WhatsAppGreen

enum class MenuDestination {
    STATUS_VIEWER,
    MEDIA_BROWSER,
    SAVED_STATUSES,
    DELETED_MESSAGES,
    STATUS_SPLITTER,
    DIRECT_CHAT,
    UPDATE,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onNavigate: (MenuDestination) -> Unit
) {
    val context = LocalContext.current
    val currentVersion = remember { UpdateChecker.getCurrentVersion(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "WASSaver",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "WhatsApp Status Saver & Tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Menu items
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Section: Status Tools
            Text(
                text = "STATUS TOOLS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            MenuItemCard(
                icon = Icons.Default.Home,
                title = "Status Viewer",
                description = "View and save WhatsApp status updates from contacts",
                gradientColors = listOf(WhatsAppGreen, WhatsAppDarkGreen),
                onClick = { onNavigate(MenuDestination.STATUS_VIEWER) }
            )

            MenuItemCard(
                icon = Icons.Default.PermMedia,
                title = "Media Browser",
                description = "Browse WhatsApp private media — view once photos & videos",
                gradientColors = listOf(Color(0xFF6C63FF), Color(0xFF4834DF)),
                onClick = { onNavigate(MenuDestination.MEDIA_BROWSER) }
            )

            MenuItemCard(
                icon = Icons.Default.Bookmark,
                title = "Saved Statuses",
                description = "View and manage all your previously saved statuses",
                gradientColors = listOf(Color(0xFFFF9800), Color(0xFFF57C00)),
                onClick = { onNavigate(MenuDestination.SAVED_STATUSES) }
            )

            MenuItemCard(
                icon = Icons.Default.DeleteSweep,
                title = "Deleted Messages",
                description = "Recover deleted WhatsApp messages via notification capture",
                gradientColors = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2)),
                onClick = { onNavigate(MenuDestination.DELETED_MESSAGES) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section: Sharing Tools
            Text(
                text = "SHARING TOOLS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            MenuItemCard(
                icon = Icons.Default.ContentCut,
                title = "Status Splitter",
                description = "Split long videos into 90s parts for WhatsApp Status",
                gradientColors = listOf(Color(0xFFE91E63), Color(0xFFC2185B)),
                onClick = { onNavigate(MenuDestination.STATUS_SPLITTER) }
            )

            MenuItemCard(
                icon = Icons.AutoMirrored.Filled.Send,
                title = "Direct Message",
                description = "Send messages to unsaved numbers on WhatsApp",
                gradientColors = listOf(Color(0xFF00BCD4), Color(0xFF00838F)),
                onClick = { onNavigate(MenuDestination.DIRECT_CHAT) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section: App
            Text(
                text = "APP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            MenuItemCard(
                icon = Icons.Default.SystemUpdate,
                title = "Check for Updates",
                description = "Current version: v$currentVersion — Check GitHub for new releases",
                gradientColors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)),
                onClick = { onNavigate(MenuDestination.UPDATE) }
            )

            MenuItemCard(
                icon = Icons.Default.Info,
                title = "About",
                description = "App info, credits & version details",
                gradientColors = listOf(Color(0xFF607D8B), Color(0xFF455A64)),
                onClick = { onNavigate(MenuDestination.ABOUT) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Text(
                text = "Built with ❤\uFE0F by Parveen Bhadoo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "v$currentVersion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(colors = gradientColors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title and description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
