package com.brenninho.trimly.settings

import android.content.Context
import com.brenninho.trimly.i18n.AppLanguage

class AppSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("trimly_settings", Context.MODE_PRIVATE)

    var language: AppLanguage
        get() = AppLanguage.fromCode(prefs.getString("language", null))
        set(value) {
            prefs.edit().putString("language", value.code).apply()
        }

    var tipsEnabled: Boolean
        get() = prefs.getBoolean("tips_enabled", true)
        set(value) {
            prefs.edit().putBoolean("tips_enabled", value).apply()
        }

    var pendingState: String?
        get() = prefs.getString("pending_state", null)
        set(value) {
            prefs.edit().putString("pending_state", value).apply()
        }

    var profile: DiscordProfile?
        get() {
            val id = prefs.getString("discord_id", null) ?: return null
            return DiscordProfile(
                id = id,
                username = prefs.getString("discord_username", "").orEmpty(),
                displayName = prefs.getString("discord_display", "").orEmpty(),
                avatarUrl = prefs.getString("discord_avatar", "").orEmpty()
            )
        }
        set(value) {
            val editor = prefs.edit()
            if (value == null) {
                editor.remove("discord_id")
                editor.remove("discord_username")
                editor.remove("discord_display")
                editor.remove("discord_avatar")
            } else {
                editor.putString("discord_id", value.id)
                editor.putString("discord_username", value.username)
                editor.putString("discord_display", value.displayName)
                editor.putString("discord_avatar", value.avatarUrl)
            }
            editor.apply()
        }
}
