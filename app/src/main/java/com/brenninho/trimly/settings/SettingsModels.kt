package com.brenninho.trimly.settings

import com.brenninho.trimly.i18n.AppLanguage

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
    val profile: DiscordProfile? = null,
    val login: LoginStatus = LoginStatus.Idle
)

class SettingsActions(
    val onLanguage: (AppLanguage) -> Unit,
    val onTips: (Boolean) -> Unit,
    val onLogin: () -> Unit,
    val onCancelLogin: () -> Unit,
    val onLogout: () -> Unit
)
