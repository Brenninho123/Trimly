package com.brenninho.trimly.ios

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

fun MainViewController(): UIViewController {
    val viewController = ComposeUIViewController { App() }
    VideoPickerBridge.present = { presentVideoPicker(viewController) }
    return viewController
}

private fun presentVideoPicker(from: UIViewController) {
    val picker = UIDocumentPickerViewController(
        documentTypes = listOf("public.movie"),
        inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
    )
    picker.allowsMultipleSelection = false
    picker.delegate = VideoPickerDelegate
    from.presentViewController(picker, animated = true, completion = null)
}

private object VideoPickerDelegate : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val accessing = url.startAccessingSecurityScopedResource()
        val path = url.path
        if (path != null) {
            VideoPickerBridge.pickedPath.value = path
        } else {
            VideoPickerBridge.shareErrorMessage.value = "Could not read the picked file."
        }
        if (accessing) url.stopAccessingSecurityScopedResource()
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = Unit
}
