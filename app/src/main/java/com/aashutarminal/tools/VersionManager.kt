package com.aashutarminal.tools

import android.content.Context

/**
 * Tracks which tools/versions are installed using a simple SharedPreferences
 * store (name -> version). Swap for a real DB if the tool count grows large.
 */
class VersionManager(context: Context) {
    private val prefs = context.getSharedPreferences("installed_tools", Context.MODE_PRIVATE)

    fun isInstalled(name: String): Boolean = prefs.contains(name)

    fun installedVersion(name: String): String? = prefs.getString(name, null)

    fun markInstalled(name: String, version: String) {
        prefs.edit().putString(name, version).apply()
    }

    fun markRemoved(name: String) {
        prefs.edit().remove(name).apply()
    }

    fun allInstalled(): Map<String, String> =
        prefs.all.mapNotNull { (k, v) -> if (v is String) k to v else null }.toMap()
}
