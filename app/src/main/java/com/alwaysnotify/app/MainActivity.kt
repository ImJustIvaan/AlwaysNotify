package com.alwaysnotify.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var listenerStatusText: TextView
    private lateinit var overlayStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listenerStatusText = findViewById(R.id.listenerStatusText)
        overlayStatusText = findViewById(R.id.overlayStatusText)

        findViewById<Button>(R.id.grantListenerButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.grantOverlayButton).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }

        findViewById<Button>(R.id.chooseAppsButton).setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateListenerStatus()
        updateOverlayStatus()
    }

    private fun updateListenerStatus() {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        listenerStatusText.setText(
            if (enabled) R.string.listener_status_enabled else R.string.listener_status_disabled
        )
    }

    private fun updateOverlayStatus() {
        val granted = OverlayBannerManager.canDrawOverlays(this)
        overlayStatusText.setText(
            if (granted) R.string.overlay_status_enabled else R.string.overlay_status_disabled
        )
    }
}
