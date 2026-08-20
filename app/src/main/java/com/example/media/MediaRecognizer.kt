package com.example.media

import java.net.URI

enum class RecognizedMediaType {
    NONE,
    URL,
    YOUTUBE_VIDEO,
    YOUTUBE_PLAYLIST,
    AUDIO_FILE
}

data class MediaInfo(
    val type: RecognizedMediaType,
    val rawUrl: String,
    val title: String,
    val youtubeId: String? = null,
    val playlistId: String? = null
)

object MediaRecognizer {

    private val YOUTUBE_VIDEO_REGEX = Regex("""(?:youtu\.be/|youtube\.com/(?:embed/|v/|watch\?v=|watch\?.+&v=))([\w-]{11})""")
    private val YOUTUBE_PLAYLIST_REGEX = Regex("""(?:youtube\.com/playlist\?list=|[\?&]list=)([\w-]+)""")
    private val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".wav", ".aac", ".ogg", ".flac")

    fun recognize(input: String?): MediaInfo {
        if (input.isNullOrBlank()) {
            return MediaInfo(RecognizedMediaType.NONE, "", "")
        }

        val text = input.trim()

        // Check for YouTube Playlist
        val playlistMatch = YOUTUBE_PLAYLIST_REGEX.find(text)
        val videoMatch = YOUTUBE_VIDEO_REGEX.find(text)

        if (text.contains("playlist") && playlistMatch != null) {
            val listId = playlistMatch.groupValues[1]
            return MediaInfo(
                type = RecognizedMediaType.YOUTUBE_PLAYLIST,
                rawUrl = text,
                title = "YouTube Playlist ($listId)",
                playlistId = listId
            )
        }

        // Check for YouTube Video
        if (videoMatch != null) {
            val videoId = videoMatch.groupValues[1]
            return MediaInfo(
                type = RecognizedMediaType.YOUTUBE_VIDEO,
                rawUrl = text,
                title = "YouTube Video ($videoId)",
                youtubeId = videoId,
                playlistId = playlistMatch?.groupValues?.get(1)
            )
        }

        // Check for Audio File
        val lower = text.lowercase()
        if (AUDIO_EXTENSIONS.any { lower.endsWith(it) } || text.startsWith("content://media/external/audio")) {
            val name = text.substringAfterLast("/").substringAfterLast("\\").ifEmpty { "Audio Track" }
            return MediaInfo(
                type = RecognizedMediaType.AUDIO_FILE,
                rawUrl = text,
                title = name
            )
        }

        // General Web URL
        if (text.startsWith("http://") || text.startsWith("https://")) {
            val host = try {
                URI(text).host ?: text
            } catch (e: Exception) {
                text
            }
            return MediaInfo(
                type = RecognizedMediaType.URL,
                rawUrl = text,
                title = host
            )
        }

        return MediaInfo(RecognizedMediaType.NONE, text, text)
    }
}
