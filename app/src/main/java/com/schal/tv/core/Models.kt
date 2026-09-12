package com.schal.tv.core

enum class StreamStatus {
    UNKNOWN, CHECKING, ONLINE, OFFLINE, INVALID, UNSUPPORTED;

    companion object {
        fun fromString(value: String?): StreamStatus = when (value?.lowercase()) {
            "checking" -> CHECKING
            "online" -> ONLINE
            "offline" -> OFFLINE
            "invalid" -> INVALID
            "unsupported" -> UNSUPPORTED
            else -> UNKNOWN
        }
    }
}

enum class StreamType {
    HLS, MP4, DASH, LOCAL, UNKNOWN;

    companion object {
        fun fromString(value: String?): StreamType = when (value?.lowercase()) {
            "hls" -> HLS
            "mp4" -> MP4
            "dash" -> DASH
            "local" -> LOCAL
            else -> UNKNOWN
        }
    }
}

data class TvChannel(
    val id: String,
    val name: String,
    val description: String = "",
    val country: String = "",
    val language: String = "",
    val logo: String = "",
    val category: String = "",
    val genre: String = "",
    val streamUrl: String = "",
    val alternateStreams: List<String> = emptyList(),
    val streamType: StreamType = StreamType.UNKNOWN,
    val quality: String = "",
    val isLive: Boolean = true,
    val isActive: Boolean = true,
    val offlineAvailable: Boolean = false,
    var favorite: Boolean = false,
    val updatedAt: String = "",
    var streamStatus: StreamStatus = StreamStatus.UNKNOWN
) {
    fun hasConfiguredStream(): Boolean = streamUrl.isNotBlank()

    fun searchableText(): String =
        "$name $country $language $category $genre".lowercase()
}

data class LocalVideo(
    val id: String,
    val title: String,
    val path: String,
    val mimeType: String = "video/mp4",
    val sizeBytes: Long = 0L,
    val durationSeconds: Long = 0L,
    val thumbnail: String = "",
    var favorite: Boolean = false,
    var lastPositionSeconds: Long = 0L
)

data class Favorites(
    val tv: MutableSet<String> = mutableSetOf(),
    val radio: MutableSet<String> = mutableSetOf(),
    val localVideo: MutableSet<String> = mutableSetOf()
)

data class CacheItem(
    val id: String,
    val cachedAt: String,
    val expiresAt: String,
    val status: String
)

data class CacheManifest(
    val schemaVersion: String = "1.0",
    val items: MutableList<CacheItem> = mutableListOf()
)

sealed class CatalogResult {
    data class Success(val channels: List<TvChannel>) : CatalogResult()
    data class Empty(val reason: String) : CatalogResult()
    data class Error(val message: String, val cause: Throwable? = null) : CatalogResult()
}
