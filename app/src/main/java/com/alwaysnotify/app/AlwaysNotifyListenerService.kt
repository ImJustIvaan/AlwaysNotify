package com.alwaysnotify.app

import android.app.Notification
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap

/**
 * Watches every notification posted on the device. For apps the user picked in
 * "Choose apps", it reposts a boosted copy on a high-importance channel so it
 * shows as a heads-up banner and is fully visible on the lock screen, similar
 * to how a normal high-priority notification behaves.
 */
class AlwaysNotifyListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (sbn.packageName == packageName) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (!PrefsManager.isSelected(this, sbn.packageName)) return

        repost(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        if (sbn.packageName == packageName) return
        NotificationManagerCompat.from(this).cancel(sbn.packageName, repostId(sbn))
    }

    private fun repost(sbn: StatusBarNotification) {
        val source = sbn.notification
        val extras = source.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE) ?: appLabelFor(sbn.packageName)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: text

        val builder = NotificationCompat.Builder(this, AlwaysNotifyApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setWhen(sbn.postTime)
            .setShowWhen(true)
            .setGroup(GROUP_KEY)

        source.contentIntent?.let { builder.setContentIntent(it) }
        extractLargeIcon(sbn)?.let { builder.setLargeIcon(it) }

        try {
            NotificationManagerCompat.from(this).notify(sbn.packageName, repostId(sbn), builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was denied; nothing to boost until the user grants it.
        }
    }

    private fun extractLargeIcon(sbn: StatusBarNotification): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val icon = sbn.notification.getLargeIcon() ?: return null
                icon.loadDrawable(this)?.toBitmap()
            } else {
                @Suppress("DEPRECATION")
                sbn.notification.extras.getParcelable(Notification.EXTRA_LARGE_ICON)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun appLabelFor(packageName: String): CharSequence {
        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0))
        } catch (_: Exception) {
            packageName
        }
    }

    private fun repostId(sbn: StatusBarNotification): Int = (sbn.packageName + ":" + sbn.id).hashCode()

    companion object {
        private const val GROUP_KEY = "com.alwaysnotify.app.BOOSTED"
    }
}
