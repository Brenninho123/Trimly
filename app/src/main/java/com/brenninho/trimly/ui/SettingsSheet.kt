@file:OptIn(ExperimentalMaterial3Api::class)

package com.brenninho.trimly.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.data.RemoteImages
import com.brenninho.trimly.i18n.AppLanguage
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.settings.DiscordProfile
import com.brenninho.trimly.settings.LoginError
import com.brenninho.trimly.settings.LoginStatus
import com.brenninho.trimly.settings.SettingsActions
import com.brenninho.trimly.settings.SettingsState

private val Blurple = Color(0xFF5865F2)

@Composable
fun SettingsSheet(
    settings: SettingsState,
    actions: SettingsActions,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = s.settingsTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            SectionTitle(s.accountTitle)
            DiscordCard(settings = settings, actions = actions)

            SectionTitle(s.languageTitle)
            LanguageSection(current = settings.language, onSelect = actions.onLanguage)

            SectionTitle(s.homeSection)
            SwitchRow(
                title = s.tipsTitle,
                hint = s.tipsHint,
                checked = settings.tipsEnabled,
                onCheckedChange = actions.onTips
            )

            SectionTitle(s.aboutTitle)
            Text(
                text = s.aboutBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.versionLabel(appVersion(context)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun DiscordCard(
    settings: SettingsState,
    actions: SettingsActions
) {
    val s = LocalStrings.current
    val profile = settings.profile
    val login = settings.login

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                profile != null -> SignedIn(profile = profile, onLogout = actions.onLogout)
                login is LoginStatus.Waiting -> Progress(text = s.discordWaiting, onCancel = actions.onCancelLogin)
                login is LoginStatus.Loading -> Progress(text = s.discordLoading, onCancel = null)
                else -> SignedOut(login = login, onLogin = actions.onLogin)
            }
        }
    }
}

@Composable
private fun SignedIn(
    profile: DiscordProfile,
    onLogout: () -> Unit
) {
    val s = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        AvatarImage(url = profile.avatarUrl, name = profile.displayName, size = 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = s.discordSignedInAs("@${profile.username}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        TextButton(onClick = onLogout) {
            Text(s.discordLogout)
        }
    }
}

@Composable
private fun Progress(
    text: String,
    onCancel: (() -> Unit)?
) {
    val s = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (onCancel != null) {
            TextButton(onClick = onCancel) {
                Text(s.cancel)
            }
        }
    }
}

@Composable
private fun SignedOut(
    login: LoginStatus,
    onLogin: () -> Unit
) {
    val s = LocalStrings.current
    Column {
        Text(
            text = s.discordHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onLogin,
            colors = ButtonDefaults.buttonColors(containerColor = Blurple, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.discordLogin)
        }
        if (login is LoginStatus.Failed) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorText(login.error, s),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun LanguageSection(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "languageChevron"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.languageTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = languageLabel(current, s),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = chevron }
                )
            }
            Expand(visible = expanded) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    AppLanguage.entries.forEach { language ->
                        LanguageRow(
                            label = languageLabel(language, s),
                            selected = current == language,
                            onClick = { onSelect(language) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun Expand(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(220)) + fadeIn(tween(180)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(120))
    ) {
        content()
    }
}

@Composable
internal fun AvatarImage(
    url: String,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        bitmap = RemoteImages.load(url)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun languageLabel(language: AppLanguage, s: AppStrings): String =
    if (language == AppLanguage.SYSTEM) s.languageSystem else language.nativeName

private fun errorText(error: LoginError, s: AppStrings): String = when (error) {
    LoginError.CANCELLED -> s.loginErrorCancelled
    LoginError.FAILED -> s.loginErrorFailed
    LoginError.NETWORK -> s.loginErrorNetwork
    LoginError.SECURITY -> s.loginErrorSecurity
    LoginError.NO_BROWSER -> s.loginErrorNoBrowser
}

private fun appVersion(context: Context): String =
    try {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName.orEmpty()
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    }
