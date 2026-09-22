package com.brenninho.trimly.platform

enum class PlatformKind {
    ANDROID,
    WINDOWS,
    MACOS,
    LINUX,
    IOS
}

expect object Platform {
    val kind: PlatformKind
    val displayName: String
}

val Platform.isDesktop: Boolean
    get() = kind == PlatformKind.WINDOWS || kind == PlatformKind.MACOS || kind == PlatformKind.LINUX

val Platform.isMobile: Boolean
    get() = kind == PlatformKind.ANDROID || kind == PlatformKind.IOS
