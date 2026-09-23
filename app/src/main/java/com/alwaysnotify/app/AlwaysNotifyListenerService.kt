package com.alwaysnotify.app

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.BundleCompat

/**
 * Watches every notification posted on the device. For apps the user picked in
 * "Choose apps", it shows the boosted alert as an on-screen overlay banner via
 * [OverlayBannerManager] - it never posts an actual system notification, so
 * there's no duplicate entry alongside the source app's own notification.
 */
class AlwaysNotifyListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        Log.d(TAG, "onNotificationPosted from ${sbn.packageName} (selected=${PrefsManager.isSelected(this, sbn.packageName)})")

        if (sbn.packageName == packageName) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d(TAG, "Skipping group summary notification from ${sbn.packageName}")
            return
        }
        if (!PrefsManager.isSelected(this, sbn.packageName)) return

        showOverlay(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        OverlayBannerManager.dismissIfKey(sbn.key)
    }

    private fun showOverlay(sbn: StatusBarNotification) {
        val source = sbn.notification
        val extras = source.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE) ?: appLabelFor(sbn.packageName)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: text

        OverlayBannerManager.show(
            context = this,
            key = sbn.key,
            appLabel = appLabelFor(sbn.packageName),
            title = title,
            text = bigText,
            icon = extractLargeIcon(sbn),
            bigPicture = extractBigPicture(sbn),
            contentIntent = source.contentIntent
        )
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
     * notification, if it set one, so the overlay banner shows it too.
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

    companion object {
        private const val TAG = "AlwaysNotify"
    }
}
