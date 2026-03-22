package com.wassaver.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wassaver.app.data.MessageStore
import com.wassaver.app.ui.theme.WhatsAppDarkGreen
import com.wassaver.app.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedMessagesScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current

    var isFeatureEnabled by remember { mutableStateOf(MessageStore.isListenerEnabled(context)) }
    var hasNotificationAccess by remember { mutableStateOf(MessageStore.isNotificationAccessGranted(context)) }
    var allMessages by remember { mutableStateOf(MessageStore.getMessages(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }

    // Selected sender for chat view (null = show contact list)
    var selectedSender by remember { mutableStateOf<String?>(null) }
    var showClearSenderDialog by remember { mutableStateOf<String?>(null) }

    // Refresh when returning from settings
    LaunchedEffect(Unit) {
        hasNotificationAccess = MessageStore.isNotificationAccessGranted(context)
        isFeatureEnabled = MessageStore.isListenerEnabled(context)
    }

    // Auto-refresh messages every 3 seconds when listener is active
    LaunchedEffect(isFeatureEnabled, hasNotificationAccess) {
        if (isFeatureEnabled && hasNotificationAccess) {
            while (true) {
                delay(3000)
                val newMessages = MessageStore.getMessages(context)
                if (newMessages.size != allMessages.size ||
                    newMessages.firstOrNull()?.id != allMessages.firstOrNull()?.id) {
                    allMessages = newMessages
                }
            }
        }
    }

    fun refreshMessages() {
        allMessages = MessageStore.getMessages(context)
        hasNotificationAccess = MessageStore.isNotificationAccessGranted(context)
    }

    // Group messages by sender key (sender name + group)
    val groupedBySender = remember(allMessages, searchQuery) {
        val filtered = if (searchQuery.isBlank()) allMessages
        else MessageStore.searchMessages(context, searchQuery)

        // Group by a unique key: for groups use "groupName", for DMs use "sender"
        filtered.groupBy { msg ->
            if (msg.isGroup && msg.groupName != null) msg.groupName else msg.sender
        }
    }

    // Build contact list sorted by most recent message
    val contactList = remember(groupedBySender) {
        groupedBySender.map { (key, messages) ->
            val latest = messages.first() // already sorted by timestamp desc
            ContactSummary(
                key = key,
                displayName = if (latest.isGroup && latest.groupName != null) latest.groupName else latest.sender,
                lastMessage = latest.text,
                lastTime = latest.formattedTime,
                messageCount = messages.size,
                isGroup = latest.isGroup,
                appName = latest.appName
            )
        }.sortedByDescending { summary ->
            groupedBySender[summary.key]?.firstOrNull()?.timestamp ?: 0L
        }
    }

    // Messages for selected sender
    val senderMessages = remember(selectedSender, allMessages) {
        if (selectedSender == null) emptyList()
        else {
            allMessages.filter { msg ->
                val key = if (msg.isGroup && msg.groupName != null) msg.groupName else msg.sender
                key == selectedSender
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedSender != null) {
                            selectedSender = null
                        } else {
                            onBack?.invoke()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    if (selectedSender != null) {
                        // Chat view header
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedSender ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${senderMessages.size} message${if (senderMessages.size != 1) "s" else ""} captured",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        // Clear this chat button
                        IconButton(onClick = { showClearSenderDialog = selectedSender }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear chat", tint = Color.White)
                        }
                    } else {
                        // Main list header
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Deleted Messages",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isFeatureEnabled && hasNotificationAccess) {
                            IconButton(onClick = { showSearch = !showSearch }) {
                                Icon(
                                    if (showSearch) Icons.Default.SearchOff else Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { refreshMessages() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                        }
                    }
                }

                // Search bar (only in contact list view)
                AnimatedVisibility(visible = showSearch && selectedSender == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp),
                        placeholder = { Text("Search messages...", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Content
        if (!isFeatureEnabled) {
            FeatureSetupContent(
                hasNotificationAccess = hasNotificationAccess,
                onEnable = {
                    MessageStore.setListenerEnabled(context, true)
                    isFeatureEnabled = true
                    if (!hasNotificationAccess) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (_: Exception) { }
                    }
                },
                onOpenSettings = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (_: Exception) { }
                }
            )
        } else if (!hasNotificationAccess) {
            PermissionNeededContent(
                onOpenSettings = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (_: Exception) { }
                },
                onRefresh = {
                    hasNotificationAccess = MessageStore.isNotificationAccessGranted(context)
                }
            )
        } else if (selectedSender != null) {
            // Chat view for specific sender
            ChatView(
                messages = senderMessages,
                onDeleteMessage = { msgId ->
                    MessageStore.deleteMessage(context, msgId)
                    refreshMessages()
                    // If no more messages for this sender, go back
                    val remaining = allMessages.filter { msg ->
                        val key = if (msg.isGroup && msg.groupName != null) msg.groupName else msg.sender
                        key == selectedSender
                    }
                    if (remaining.isEmpty()) selectedSender = null
                }
            )
        } else if (contactList.isEmpty()) {
            EmptyMessagesContent(
                hasSearch = searchQuery.isNotBlank(),
                onDisable = { showDisableDialog = true }
            )
        } else {
            // Contact/sender list
            Column(modifier = Modifier.fillMaxSize()) {
                // Stats bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${contactList.size} chat${if (contactList.size != 1) "s" else ""} • ${allMessages.size} message${if (allMessages.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { showClearAllDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(
                                onClick = { showDisableDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disable", fontSize = 12.sp)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(contactList, key = { it.key }) { contact ->
                        ContactCard(
                            contact = contact,
                            onClick = { selectedSender = contact.key },
                            onClearChat = { showClearSenderDialog = contact.key }
                        )
                    }
                }
            }
        }
    }

    // Clear all dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Messages?") },
            text = { Text("This will permanently delete all ${allMessages.size} captured messages from all chats. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        MessageStore.clearAllMessages(context)
                        refreshMessages()
                        selectedSender = null
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear All") }
            },
            dismissButton = { TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") } }
        )
    }

    // Clear specific sender dialog
    if (showClearSenderDialog != null) {
        val senderName = showClearSenderDialog ?: ""
        val count = groupedBySender[senderName]?.size ?: 0
        AlertDialog(
            onDismissRequest = { showClearSenderDialog = null },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear chat with $senderName?") },
            text = { Text("This will permanently delete $count message${if (count != 1) "s" else ""} from this chat. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val senderKey = showClearSenderDialog
                        val toDelete = allMessages.filter { msg ->
                            val key = if (msg.isGroup && msg.groupName != null) msg.groupName else msg.sender
                            key == senderKey
                        }
                        toDelete.forEach { MessageStore.deleteMessage(context, it.id) }
                        refreshMessages()
                        if (selectedSender == senderKey) selectedSender = null
                        showClearSenderDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear Chat") }
            },
            dismissButton = { TextButton(onClick = { showClearSenderDialog = null }) { Text("Cancel") } }
        )
    }

    // Disable feature dialog
    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            icon = { Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Disable Message Capture?") },
            text = { Text("WASSaver will stop listening to WhatsApp notifications. Previously captured messages will be kept.") },
            confirmButton = {
                Button(
                    onClick = {
                        MessageStore.setListenerEnabled(context, false)
                        isFeatureEnabled = false
                        showDisableDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Disable") }
            },
            dismissButton = { TextButton(onClick = { showDisableDialog = false }) { Text("Cancel") } }
        )
    }
}

private data class ContactSummary(
    val key: String,
    val displayName: String,
    val lastMessage: String,
    val lastTime: String,
    val messageCount: Int,
    val isGroup: Boolean,
    val appName: String
)

@Composable
private fun ContactCard(
    contact: ContactSummary,
    onClick: () -> Unit,
    onClearChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (contact.isGroup)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (contact.isGroup) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = contact.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = contact.lastTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Message count badge
                    Surface(
                        shape = CircleShape,
                        color = WhatsAppGreen,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = if (contact.messageCount > 99) "99+" else "${contact.messageCount}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // App badge
                Text(
                    text = contact.appName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Clear chat button
            IconButton(
                onClick = onClearChat,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Clear chat",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ChatView(
    messages: List<MessageStore.CapturedMessage>,
    onDeleteMessage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        // Show oldest first (like a chat)
        reverseLayout = false
    ) {
        val sortedMessages = messages.sortedBy { it.timestamp }
        items(sortedMessages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                onDelete = { onDeleteMessage(message.id) }
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageStore.CapturedMessage,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Sender name for group chats
        if (message.isGroup) {
            Text(
                text = message.sender,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable { showDeleteDialog = true },
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this message?") },
            text = { Text("This will remove the captured message from your local storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

// ── Setup / Permission / Empty screens ──

@Composable
private fun FeatureSetupContent(
    hasNotificationAccess: Boolean,
    onEnable: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(WhatsAppGreen, WhatsAppDarkGreen))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Deleted Message Recovery", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Capture WhatsApp messages as they arrive. Even if the sender deletes them, you'll still have a copy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("How it works:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        StepItem("1", "Enable this feature")
                        StepItem("2", "Grant notification access permission")
                        StepItem("3", "Messages are captured automatically")
                        StepItem("4", "View them here grouped by sender")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Messages are stored locally on your device only. Nothing is sent to any server. You can disable this anytime.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onEnable,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Enable Message Capture", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun StepItem(number: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(WhatsAppGreen), contentAlignment = Alignment.Center) {
            Text(number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PermissionNeededContent(onOpenSettings: () -> Unit, onRefresh: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth().padding(32.dp), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Notification Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("To capture WhatsApp messages, WASSaver needs notification access permission. Find \"WASSaver\" in the list and enable it.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Notification Settings", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I've Enabled It — Refresh")
                }
            }
        }
    }
}

@Composable
private fun EmptyMessagesContent(hasSearch: Boolean, onDisable: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (hasSearch) Icons.Outlined.SearchOff else Icons.Outlined.ChatBubbleOutline,
                contentDescription = null, modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasSearch) "No Messages Found" else "No Messages Yet",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasSearch) "Try a different search query."
                else "Messages will appear here grouped by sender as WhatsApp notifications arrive. If someone sends and then deletes a message, you'll still see it here.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center, lineHeight = 22.sp
            )
            if (!hasSearch) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = WhatsAppGreen.copy(alpha = 0.1f)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Listener is active. Messages will be captured automatically.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDisable) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Disable Capture")
                }
            }
        }
    }
}
