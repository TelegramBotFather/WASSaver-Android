package com.wassaver.app.data

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.UUID

/**
 * Listens to WhatsApp notifications and captures message content.
 * This allows recovering messages even if the sender deletes them.
 *
 * The user must explicitly:
 * 1. Enable the feature in the app settings
 * 2. Grant notification access permission in Android settings
 */
class WhatsAppNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "WANotifListener"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        private val WHATSAPP_PACKAGES = setOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Only process WhatsApp notifications
        if (sbn.packageName !in WHATSAPP_PACKAGES) return

        // Check if feature is enabled by user
        if (!MessageStore.isListenerEnabled(applicationContext)) return

        try {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            // Get message text
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            if (text.isNullOrBlank()) return

            // Get sender/title
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            if (title.isNullOrBlank()) return

            // Skip system/summary notifications
            if (title.contains("messages") && title.contains("WhatsApp")) return
            if (text.contains("new messages") || text.contains("new message")) return
            if (title == "WhatsApp" || title == "WhatsApp Business") return

            // Detect group messages (format: "Sender @ Group" or "Group: Sender")
            var sender = title
            var isGroup = false
            var groupName: String? = null

            // Check for conversation title (group name)
            val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            if (conversationTitle != null) {
                isGroup = true
                groupName = conversationTitle
                // sender remains the individual sender from EXTRA_TITLE
            }

            // Alternative group detection: "Person @ Group"
            if (!isGroup && title.contains(" @ ")) {
                val parts = title.split(" @ ", limit = 2)
                if (parts.size == 2) {
                    sender = parts[0].trim()
                    groupName = parts[1].trim()
                    isGroup = true
                }
            }

            // Skip media-only messages (photos, videos, etc.) - they say things like "📷 Photo"
            // We still capture them but note they're media
            val messageText = text

            // Generate unique ID
            val messageId = UUID.randomUUID().toString()

            val capturedMessage = MessageStore.CapturedMessage(
                id = messageId,
                sender = sender,
                text = messageText,
                timestamp = sbn.postTime,
                packageName = sbn.packageName,
                isGroup = isGroup,
                groupName = groupName
            )

            MessageStore.saveMessage(applicationContext, capturedMessage)

            Log.d(TAG, "Captured: $sender → $messageText (group=$isGroup)")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // We don't need to do anything when notifications are removed
        // The messages are already saved
    }
}
