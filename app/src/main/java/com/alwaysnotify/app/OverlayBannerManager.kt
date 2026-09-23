package com.alwaysnotify.app

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView

/**
 * Draws a heads-up-style banner on top of whatever app is currently on
 * screen, independent of the system's own notification UI entirely - this
 * is the only alert AlwaysNotify shows; it never posts an actual
 * Notification, so there's no duplicate entry in the shade. It only shows
 * while the device is unlocked and in use (a normal app can't draw over the
 * lock screen without posting a real notification).
 *
 * Requires the "display over other apps" (SYSTEM_ALERT_WINDOW) permission.
 */
object OverlayBannerManager {

    private const val AUTO_DISMISS_MS = 6000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var bannerView: View? = null
    private var dismissRunnable: Runnable? = null
    private var currentKey: String? = null

    fun canDrawOverlays(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    fun show(
        context: Context,
        key: String,
        appLabel: CharSequence,
        title: CharSequence,
        text: CharSequence,
        icon: Bitmap?,
        bigPicture: Bitmap?,
        contentIntent: PendingIntent?
    ) {
        if (!canDrawOverlays(context)) return
        val appContext = context.applicationContext

        mainHandler.post {
            val wm = windowManager
                ?: (appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager).also { windowManager = it }

            removeCurrent()
            currentKey = key

            val view = LayoutInflater.from(appContext).inflate(R.layout.overlay_banner, null)
            view.findViewById<TextView>(R.id.bannerAppLabel).text = appLabel
            view.findViewById<TextView>(R.id.bannerTitle).text = title
            view.findViewById<TextView>(R.id.bannerText).text = text

            val iconView = view.findViewById<ImageView>(R.id.bannerIcon)
            if (icon != null) iconView.setImageBitmap(icon) else iconView.setImageResource(R.drawable.ic_notification)

            val imageView = view.findViewById<ImageView>(R.id.bannerImage)
            if (bigPicture != null) {
                imageView.visibility = View.VISIBLE
                imageView.setImageBitmap(bigPicture)
            } else {
                imageView.visibility = View.GONE
            }

            view.setOnClickListener {
                try {
                    contentIntent?.send()
                } catch (_: PendingIntent.CanceledException) {
                    // The source app's pending intent is no longer valid; nothing to open.
                }
                removeCurrent()
            }
            view.findViewById<ImageView>(R.id.bannerClose).setOnClickListener { removeCurrent() }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                y = (16 * appContext.resources.displayMetrics.density).toInt()
            }

            try {
                wm.addView(view, params)
                bannerView = view
                view.translationY = -400f
                view.animate().translationY(0f).setDuration(220).start()

                val runnable = Runnable { removeCurrent() }
                dismissRunnable = runnable
                mainHandler.postDelayed(runnable, AUTO_DISMISS_MS)
            } catch (_: Exception) {
                // Overlay permission may have been revoked between the check and now.
            }
        }
    }

    /** Dismisses the banner only if it's still showing the given source notification. */
    fun dismissIfKey(key: String) {
        mainHandler.post {
            if (currentKey == key) removeCurrent()
        }
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun removeCurrent() {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissRunnable = null
        currentKey = null
        val view = bannerView ?: return
        bannerView = null
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
            // View was already detached.
        }
    }
}
