package com.brenninho.trimly.settings

import com.brenninho.trimly.i18n.AppLanguage

enum class ThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    BLACK("black");

    companion object {
        fun fromCode(code: String?): ThemeMode = entries.firstOrNull { it.code == code } ?: DARK
    }
}

enum class AccentColor(val code: String, val seed: Long) {
    TRIMLY("trimly", 0xFF7C9CFF),
    VIOLET("violet", 0xFFB388FF),
    MINT("mint", 0xFF5FD4A8),
    SUNSET("sunset", 0xFFFF9A5C),
    ROSE("rose", 0xFFFF7AA8),
    DYNAMIC("dynamic", 0xFF7C9CFF);

    companion object {
        fun fromCode(code: String?): AccentColor = entries.firstOrNull { it.code == code } ?: TRIMLY
    }
}

data class DiscordProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String
)

enum class LoginError {
    CANCELLED,
    FAILED,
    NETWORK,
    SECURITY,
    NO_BROWSER
}

sealed interface LoginStatus {
    data object Idle : LoginStatus
    data object Waiting : LoginStatus
    data object Loading : LoginStatus
    data class Failed(val error: LoginError) : LoginStatus
}

data class SettingsState(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val tipsEnabled: Boolean = true,
    val theme: ThemeMode = ThemeMode.DARK,
    val accent: AccentColor = AccentColor.TRIMLY,
    val profile: DiscordProfile? = null,
    val login: LoginStatus = LoginStatus.Idle
)

class SettingsActions(
    val onLanguage: (AppLanguage) -> Unit,
    val onTips: (Boolean) -> Unit,
    val onTheme: (ThemeMode) -> Unit,
    val onAccent: (AccentColor) -> Unit,
    val onLogin: () -> Unit,
    val onCancelLogin: () -> Unit,
    val onLogout: () -> Unit
)
