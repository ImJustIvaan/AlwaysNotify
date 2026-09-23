package com.alwaysnotify.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var listenerStatusText: TextView
    private lateinit var postNotificationsStatusText: TextView
    private lateinit var overlayStatusText: TextView
    private lateinit var grantPostNotificationsButton: Button

    private val postNotificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            updatePostNotificationsStatus()
            if (!granted) {
                Toast.makeText(this, R.string.notifications_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listenerStatusText = findViewById(R.id.listenerStatusText)
        postNotificationsStatusText = findViewById(R.id.postNotificationsStatusText)
        overlayStatusText = findViewById(R.id.overlayStatusText)
        grantPostNotificationsButton = findViewById(R.id.grantPostNotificationsButton)

        findViewById<Button>(R.id.grantListenerButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        grantPostNotificationsButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        findViewById<Button>(R.id.grantOverlayButton).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }

        findViewById<Button>(R.id.chooseAppsButton).setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            grantPostNotificationsButton.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        updateListenerStatus()
        updatePostNotificationsStatus()
        updateOverlayStatus()
    }

    private fun updateListenerStatus() {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        listenerStatusText.setText(
            if (enabled) R.string.listener_status_enabled else R.string.listener_status_disabled
        )
    }

    private fun updatePostNotificationsStatus() {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        postNotificationsStatusText.setText(
            if (granted) R.string.post_notifications_status_enabled else R.string.post_notifications_status_disabled
        )
    }

    private fun updateOverlayStatus() {
        val granted = OverlayBannerManager.canDrawOverlays(this)
        overlayStatusText.setText(
            if (granted) R.string.overlay_status_enabled else R.string.overlay_status_disabled
        )
    }
}
