package com.brenninho.trimly.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class RecentStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("trimly_recent", Context.MODE_PRIVATE)

    fun load(): List<RecentVideo> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                RecentVideo(
                    uri = item.getString("uri"),
                    name = item.getString("name"),
                    durationMs = item.getLong("duration"),
                    openedAt = item.getLong("openedAt")
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun save(items: List<RecentVideo>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("uri", it.uri)
                    .put("name", it.name)
                    .put("duration", it.durationMs)
                    .put("openedAt", it.openedAt)
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    fun loadGrid(): Boolean = prefs.getBoolean(KEY_GRID, false)

    fun saveGrid(value: Boolean) {
        prefs.edit().putBoolean(KEY_GRID, value).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_GRID = "grid"
    }
}
