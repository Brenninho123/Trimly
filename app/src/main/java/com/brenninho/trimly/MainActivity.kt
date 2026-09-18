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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brenninho.trimly.ui.TrimlyApp
import com.brenninho.trimly.ui.theme.TrimlyTheme

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
        installDropTarget()
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            TrimlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val recents by viewModel.recents.collectAsStateWithLifecycle()
                    val gridMode by viewModel.gridMode.collectAsStateWithLifecycle()

                    KeepScreenOn(enabled = state is MainState.Ready)

                    TrimlyApp(
                        state = state,
                        recents = recents,
                        gridMode = gridMode,
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

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            ACTION_PICK_VIDEO -> window.decorView.post { launchPicker() }
            ACTION_RECORD_VIDEO -> window.decorView.post { launchRecorder() }
            else -> {
                if (intent.action == Intent.ACTION_SEND_MULTIPLE && intent.streamUris().size > 1) {
                    toast("Opening the first video only")
                }
                intent.videoUri()?.let(::openUri)
            }
        }
    }

    private fun openUri(uri: Uri) {
        if (isVideo(uri)) viewModel.open(uri) else toast("That file is not a video")
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
            toast("No camera app found")
        }
    }

    private fun installDropTarget() {
        window.decorView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> event.clipDescription?.hasMimeType("video/*") == true

                DragEvent.ACTION_DROP -> {
                    val uri = event.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                    if (uri != null) {
                        dropPermissions = requestDragAndDropPermissions(event)
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

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun Intent.videoUri(): Uri? {
        val direct = when (action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> data
            Intent.ACTION_SEND -> streamUri()
            Intent.ACTION_SEND_MULTIPLE -> streamUris().firstOrNull()
            else -> null
        }
        return direct ?: clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
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
