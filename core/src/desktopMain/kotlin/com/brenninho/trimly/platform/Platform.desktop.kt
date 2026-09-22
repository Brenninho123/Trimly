package com.brenninho.trimly.platform

actual object Platform {
    actual val kind: PlatformKind = detectKind()
    actual val displayName: String = System.getProperty("os.name") ?: "Desktop"

    private fun detectKind(): PlatformKind {
        val name = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            name.contains("win") -> PlatformKind.WINDOWS
            name.contains("mac") || name.contains("darwin") -> PlatformKind.MACOS
            else -> PlatformKind.LINUX
        }
    }
}
