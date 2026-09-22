package com.brenninho.trimly.ios

import platform.Foundation.NSUserDefaults

private const val RECENTS_KEY = "trimly.recents"
private const val MAX_RECENTS = 8

object Recents {

    private val defaults = NSUserDefaults.standardUserDefaults

    fun load(): List<String> =
        (defaults.arrayForKey(RECENTS_KEY) as? List<*>)?.filterIsInstance<String>().orEmpty()

    fun add(path: String): List<String> {
        val updated = (listOf(path) + load().filter { it != path }).take(MAX_RECENTS)
        defaults.setObject(updated, RECENTS_KEY)
        return updated
    }
}
