package com.alwaysnotify.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lets the user pick which installed apps should have their notifications
 * boosted into large, lock-screen-visible alerts.
 */
class AppSelectionActivity : AppCompatActivity() {

    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.appRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AppListAdapter { app, selected ->
            PrefsManager.setSelected(this, app.packageName, selected)
        }
        recyclerView.adapter = adapter

        findViewById<EditText>(R.id.searchInput).addTextChangedListener { text ->
            adapter.filter(text?.toString().orEmpty())
        }

        loadApps()
    }

    private fun loadApps() {
        CoroutineScope(Dispatchers.Main).launch {
            val apps = withContext(Dispatchers.IO) { queryInstalledApps() }
            adapter.submitList(apps)
        }
    }

    private fun queryInstalledApps(): List<AppInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != packageName }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = pm.getApplicationIcon(app)
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
