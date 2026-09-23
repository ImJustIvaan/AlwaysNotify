package com.alwaysnotify.app

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.BundleCompat

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

        val largeIcon = extractLargeIcon(sbn)
        val bigPicture = extractBigPicture(sbn)

        val builder = NotificationCompat.Builder(this, AlwaysNotifyApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setWhen(sbn.postTime)
            .setShowWhen(true)
            .setGroup(GROUP_KEY)

        if (bigPicture != null) {
            // Mirrors how the source notification would expand: full image, with the
            // small/contact icon collapsed away once expanded (bigLargeIcon(null)).
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(bigText)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }

        largeIcon?.let { builder.setLargeIcon(it) }
        source.contentIntent?.let { builder.setContentIntent(it) }

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
                BundleCompat.getParcelable(sbn.notification.extras, Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Pulls the big-picture image (a photo, album art, etc.) off the source
     * notification, if it set one, so the boosted repost shows it too.
     */
    private fun extractBigPicture(sbn: StatusBarNotification): Bitmap? {
        val extras = sbn.notification.extras
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val icon = BundleCompat.getParcelable(extras, Notification.EXTRA_PICTURE_ICON, Icon::class.java)
                icon?.loadDrawable(this)?.toBitmap() ?: legacyBigPicture(extras)
            } else {
                legacyBigPicture(extras)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun legacyBigPicture(extras: Bundle): Bitmap? {
        return try {
            BundleCompat.getParcelable(extras, Notification.EXTRA_PICTURE, Bitmap::class.java)
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
