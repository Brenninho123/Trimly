package com.brenninho.trimly.ios

import androidx.compose.runtime.mutableStateOf

object VideoPickerBridge {
    var present: (() -> Unit)? = null
    var pickedPath = mutableStateOf<String?>(null)
    var shareErrorMessage = mutableStateOf<String?>(null)
}
