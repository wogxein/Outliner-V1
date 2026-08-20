package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val currentUri: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val error: String? = null
)

class AudioPlayerController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    fun play(uriString: String) {
        try {
            if (_playbackState.value.currentUri == uriString && mediaPlayer != null) {
                mediaPlayer?.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true, error = null)
                startProgressTracker()
                return
            }

            stop()

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(uriString))
                prepare()
                start()
            }

            player.setOnCompletionListener {
                _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0)
                stopProgressTracker()
            }

            player.setOnErrorListener { _, _, _ ->
                _playbackState.value = _playbackState.value.copy(isPlaying = false, error = "Unable to play audio")
                stopProgressTracker()
                true
            }

            mediaPlayer = player
            _playbackState.value = AudioPlaybackState(
                currentUri = uriString,
                isPlaying = true,
                currentPositionMs = 0,
                durationMs = player.duration,
                error = null
            )
            startProgressTracker()
        } catch (e: Exception) {
            _playbackState.value = AudioPlaybackState(
                currentUri = uriString,
                isPlaying = false,
                error = e.localizedMessage ?: "Playback failed"
            )
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        stopProgressTracker()
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun stop() {
        stopProgressTracker()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _playbackState.value = AudioPlaybackState()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = player.currentPosition,
                            durationMs = player.duration
                        )
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
    }
}
