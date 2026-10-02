package com.nobodymusic.tyxypoor.source

data class SourceCapability(
    val source: String,
    val actions: List<String>,
    val qualitys: List<String>
)

data class SourceInfo(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val homeUrl: String,
    val capabilities: List<SourceCapability>
) {
    fun supports(source: String, action: String): Boolean =
        capabilities.any { it.source == source && it.actions.contains(action) }
}

data class SourceRequest(
    val source: String,
    val action: String,
    val musicInfo: Map<String, Any?> = emptyMap(),
    val quality: String? = null
)

data class ResolvedUrl(
    val url: String,
    val quality: String
)

sealed class SourceResult<out T> {
    data class Ok<T>(val value: T) : SourceResult<T>()
    data class Err(val message: String) : SourceResult<Nothing>()
}

object SourceConstants {
    val ALL_SOURCES = listOf("kw", "kg", "tx", "wy", "mg", "local")
    val DEFAULT_QUALITY = "320k"
    val QUALITY_ORDER = listOf("128k", "320k", "flac", "flac24bit")
    val SOURCE_NAMES = mapOf(
        "kw" to "酷我", "kg" to "酷狗", "tx" to "QQ音乐",
        "wy" to "网易云", "mg" to "咪咕", "local" to "本地"
    )
}