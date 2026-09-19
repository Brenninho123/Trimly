package com.brenninho.trimly

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.DragAndDropPermissions
import android.view.DragEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.brenninho.trimly.auth.DiscordAuth
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.i18n.stringsFor
import com.brenninho.trimly.settings.LoginError
import com.brenninho.trimly.settings.SettingsActions
import com.brenninho.trimly.ui.TrimlyApp
import com.brenninho.trimly.ui.theme.TrimlyTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private sealed interface LaunchAction {
    data class Auth(val uri: Uri) : LaunchAction
    data class OpenVideo(val uri: Uri, val hasMore: Boolean) : LaunchAction
    data object PickVideo : LaunchAction
    data object RecordVideo : LaunchAction
    data object ContinueLast : LaunchAction
    data object None : LaunchAction
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var dropPermissions: DragAndDropPermissions? = null

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(::openUri) }

    private val recordVideo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) result.data?.data?.let(::openUri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        window.setBackgroundDrawable(ColorDrawable(WINDOW_BACKGROUND))
        allowCutoutContent()
        installDropTarget()
        keepShortcutsFresh()

        if (savedInstanceState == null && !launchedFromHistory(intent)) {
            handleIntent(intent)
        }

        setContent {
            TrimlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val recents by viewModel.recents.collectAsStateWithLifecycle()
                    val gridMode by viewModel.gridMode.collectAsStateWithLifecycle()
                    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

                    val configuration = LocalConfiguration.current
                    val strings = remember(settings.language, configuration) { stringsFor(settings.language) }
                    val actions = remember {
                        SettingsActions(
                            onLanguage = viewModel::setLanguage,
                            onTips = viewModel::setTipsEnabled,
                            onLogin = ::startDiscordLogin,
                            onCancelLogin = viewModel::cancelLogin,
                            onLogout = viewModel::logout
                        )
                    }

                    KeepScreenOn(enabled = state is MainState.Ready)

                    CompositionLocalProvider(LocalStrings provides strings) {
                        TrimlyApp(
                            state = state,
                            recents = recents,
                            gridMode = gridMode,
                            settings = settings,
                            actions = actions,
                            onPick = viewModel::open,
                            onOpenRecent = viewModel::openRecent,
                            onRemoveRecent = viewModel::removeRecent,
                            onClearRecents = viewModel::clearRecents,
                            onToggleGrid = viewModel::toggleGrid,
                            onDismissError = viewModel::dismissError,
                            onClose = viewModel::close
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        dropPermissions?.release()
        dropPermissions = null
        super.onDestroy()
    }

    private fun strings(): AppStrings = stringsFor(viewModel.settingsState.value.language)

    private fun launchedFromHistory(intent: Intent?): Boolean =
        intent != null && (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0

    private fun allowCutoutContent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val attributes = window.attributes
            attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attributes
        }
    }

    private fun parse(intent: Intent): LaunchAction {
        val data = intent.data
        return when {
            intent.action == Intent.ACTION_VIEW && data != null && DiscordAuth.isRedirect(data) ->
                LaunchAction.Auth(data)

            intent.action == ACTION_PICK_VIDEO -> LaunchAction.PickVideo
            intent.action == ACTION_RECORD_VIDEO -> LaunchAction.RecordVideo
            intent.action == ACTION_CONTINUE_LAST -> LaunchAction.ContinueLast

            else -> {
                val uris = intent.videoUris()
                val first = uris.firstOrNull()
                if (first == null) LaunchAction.None else LaunchAction.OpenVideo(first, uris.size > 1)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (val action = parse(intent)) {
            is LaunchAction.Auth -> viewModel.handleAuthRedirect(action.uri)
            is LaunchAction.OpenVideo -> {
                if (action.hasMore) toast(strings().openingFirstOnly)
                openUri(action.uri)
            }
            LaunchAction.PickVideo -> window.decorView.post { launchPicker() }
            LaunchAction.RecordVideo -> window.decorView.post { launchRecorder() }
            LaunchAction.ContinueLast -> viewModel.recents.value.firstOrNull()?.let(viewModel::openRecent)
            LaunchAction.None -> Unit
        }
    }

    private fun startDiscordLogin() {
        val url = viewModel.beginDiscordLogin()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            viewModel.reportLoginError(LoginError.NO_BROWSER)
        }
    }

    private fun openUri(uri: Uri) {
        if (isVideo(uri)) viewModel.open(uri) else toast(strings().notAVideo)
    }

    private fun isVideo(uri: Uri): Boolean {
        val type = try {
            contentResolver.getType(uri)
        } catch (e: SecurityException) {
            null
        }
        return type == null || type.startsWith("video/") || type == "application/octet-stream"
    }

    private fun launchPicker() {
        pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    private fun launchRecorder() {
        try {
            recordVideo.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
        } catch (e: ActivityNotFoundException) {
            toast(strings().noCameraApp)
        }
    }

    private fun installDropTarget() {
        window.decorView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> event.clipDescription?.hasMimeType("video/*") == true

                DragEvent.ACTION_DROP -> {
                    val clip = event.clipData
                    val uri = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                    if (uri != null) {
                        dropPermissions?.release()
                        dropPermissions = requestDragAndDropPermissions(event)
                        if ((clip?.itemCount ?: 0) > 1) toast(strings().openingFirstOnly)
                        openUri(uri)
                        true
                    } else {
                        false
                    }
                }

                else -> true
            }
        }
    }

    private fun keepShortcutsFresh() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.recents.map { it.isNotEmpty() }.distinctUntilChanged(),
                    viewModel.settingsState.map { it.language }.distinctUntilChanged()
                ) { hasRecents, language -> hasRecents to stringsFor(language) }
                    .collect { (hasRecents, strings) -> publishShortcuts(hasRecents, strings) }
            }
        }
    }

    private fun publishShortcuts(hasRecents: Boolean, strings: AppStrings) {
        try {
            if (!hasRecents) {
                ShortcutManagerCompat.removeDynamicShortcuts(this, listOf(SHORTCUT_CONTINUE))
                return
            }
            val target = Intent(this, MainActivity::class.java).setAction(ACTION_CONTINUE_LAST)
            val shortcut = ShortcutInfoCompat.Builder(this, SHORTCUT_CONTINUE)
                .setShortLabel(strings.continueEditing)
                .setLongLabel(strings.continueEditing)
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(target)
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
        } catch (e: Exception) {
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun Intent.videoUris(): List<Uri> {
        val direct: List<Uri> = when (action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> listOfNotNull(data)
            Intent.ACTION_SEND -> listOfNotNull(streamUri())
            Intent.ACTION_SEND_MULTIPLE -> streamUris()
            else -> return emptyList()
        }
        if (direct.isNotEmpty()) return direct
        val clip = clipData ?: return emptyList()
        return (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
    }

    private fun Intent.streamUri(): Uri? =
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM)
        }

    private fun Intent.streamUris(): List<Uri> =
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }

    companion object {
        const val ACTION_PICK_VIDEO = "com.brenninho.trimly.action.PICK_VIDEO"
        const val ACTION_RECORD_VIDEO = "com.brenninho.trimly.action.RECORD_VIDEO"
        const val ACTION_CONTINUE_LAST = "com.brenninho.trimly.action.CONTINUE_LAST"
        private const val SHORTCUT_CONTINUE = "continue_last"
        private val WINDOW_BACKGROUND = 0xFF101318.toInt()
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
