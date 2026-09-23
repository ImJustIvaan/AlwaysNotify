package com.alwaysnotify.app

import android.content.Context

/**
 * Stores the set of app package names the user wants boosted into large,
 * lock-screen-visible notifications.
 */
object PrefsManager {
    private const val PREFS_NAME = "always_notify_prefs"
    private const val KEY_SELECTED_APPS = "selected_apps"

    fun getSelectedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return HashSet(prefs.getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet())
    }

    fun isSelected(context: Context, packageName: String): Boolean {
        return getSelectedPackages(context).contains(packageName)
    }

    fun setSelected(context: Context, packageName: String, selected: Boolean) {
        val current = getSelectedPackages(context).toMutableSet()
        if (selected) current.add(packageName) else current.remove(packageName)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SELECTED_APPS, current).apply()
    }
}
