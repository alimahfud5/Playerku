package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.AlunanDatabase
import com.example.data.MusicRepository
import com.example.data.PlayerPreferences
import com.example.data.PreferencesManager
import com.example.data.TrackEntity
import com.example.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QueueEntry(
  val index: Int,
  val trackId: String,
  val title: String,
  val artist: String,
  val artworkUri: String?,
)

data class PlayerUiState(
  val allTracks: List<TrackEntity> = emptyList(),
  val favoriteTracks: List<TrackEntity> = emptyList(),
  val queue: List<QueueEntry> = emptyList(),
  val currentTrack: TrackEntity? = null,
  val currentQueueIndex: Int = -1,
  val isPlaying: Boolean = false,
  val playbackState: Int = Player.STATE_IDLE,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val repeatMode: Int = Player.REPEAT_MODE_ALL,
  val shuffleEnabled: Boolean = false,
  val playbackSpeed: Float = 1.0f,
  val sleepTimerRemainingMs: Long = 0L,
  val searchQuery: String = "",
  val statusBannerMessage: String? = null,
  val isControllerConnected: Boolean = false,
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: MusicRepository
  private val preferencesManager: PreferencesManager
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null

  private val _playbackState = MutableStateFlow(InternalPlaybackState())
  private val _searchQuery = MutableStateFlow("")
  private val _statusBanner = MutableStateFlow<String?>(null)

  private data class InternalPlaybackState(
    val currentMediaId: String? = null,
    val currentQueueIndex: Int = -1,
    val queue: List<QueueEntry> = emptyList(),
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_ALL,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerEndElapsedMs: Long = 0L,
    val sleepTimerRemainingMs: Long = 0L,
    val isConnected: Boolean = false,
  )

  val uiState: StateFlow<PlayerUiState>

  init {
    val db = AlunanDatabase.getInstance(application)
    repository = MusicRepository(application, db.trackDao())
    preferencesManager = PreferencesManager(application)

    uiState =
      combine(
          repository.allTracks,
          repository.favoriteTracks,
          _playbackState,
          _searchQuery,
          _statusBanner,
        ) { tracks, favorites, playback, query, banner ->
          val filteredTracks =
            if (query.isBlank()) {
              tracks
            } else {
              tracks.filter {
                it.title.contains(query, ignoreCase = true) ||
                  it.artist.contains(query, ignoreCase = true) ||
                  it.album.contains(query, ignoreCase = true)
              }
            }
          val activeTrack = tracks.firstOrNull { it.id == playback.currentMediaId }
          PlayerUiState(
            allTracks = filteredTracks,
            favoriteTracks = favorites,
            queue = playback.queue,
            currentTrack = activeTrack,
            currentQueueIndex = playback.currentQueueIndex,
            isPlaying = playback.isPlaying,
            playbackState = playback.playbackState,
            currentPositionMs = playback.currentPositionMs,
            durationMs =
              playback.durationMs.takeIf { it > 0L } ?: (activeTrack?.durationMs ?: 0L),
            repeatMode = playback.repeatMode,
            shuffleEnabled = playback.shuffleEnabled,
            playbackSpeed = playback.playbackSpeed,
            sleepTimerRemainingMs = playback.sleepTimerRemainingMs,
            searchQuery = query,
            statusBannerMessage = banner,
            isControllerConnected = playback.isConnected,
          )
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000L),
          initialValue = PlayerUiState(),
        )

    viewModelScope.launch {
      repository.ensureBundledTracksSeeded()
    }

    connectToPlaybackService()
    startPositionTicker()
  }

  private fun connectToPlaybackService() {
    val context = getApplication<Application>()
    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future =
      MediaController.Builder(context, sessionToken)
        .setListener(
          object : MediaController.Listener {
            override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
              val endMs = extras.getLong(PlaybackService.EXTRA_SLEEP_TIMER_END_MS, 0L)
              updateSleepTimerEnd(endMs)
            }
          }
        )
        .buildAsync()
    controllerFuture = future

    future.addListener(
      {
        val controller =
          try {
            future.get()
          } catch (_: Exception) {
            return@addListener
          }
        mediaController = controller
        attachControllerListener(controller)
        viewModelScope.launch { restoreQueueIfEmpty(controller) }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  private suspend fun restoreQueueIfEmpty(controller: MediaController) {
    val prefs: PlayerPreferences = preferencesManager.preferencesFlow.first()
    if (controller.mediaItemCount == 0) {
      val tracks = repository.allTracks.first()
      if (tracks.isNotEmpty()) {
        val startIdx =
          prefs.lastTrackId?.let { id -> tracks.indexOfFirst { it.id == id }.coerceAtLeast(0) } ?: 0
        controller.setMediaItems(tracks.map { it.toMediaItem() }, startIdx, 0L)
        controller.repeatMode = prefs.repeatMode
        controller.shuffleModeEnabled = prefs.shuffleEnabled
        controller.setPlaybackSpeed(prefs.playbackSpeed)
        controller.prepare()
      }
    }
    syncFromController(controller)
  }

  private fun attachControllerListener(controller: MediaController) {
    val endMs = controller.sessionExtras.getLong(PlaybackService.EXTRA_SLEEP_TIMER_END_MS, 0L)
    updateSleepTimerEnd(endMs)

    controller.addListener(
      object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
          syncFromController(controller)
        }
      }
    )
    syncFromController(controller)
  }

  private fun syncFromController(controller: MediaController) {
    val currentItem = controller.currentMediaItem
    val queueEntries = buildList {
      for (i in 0 until controller.mediaItemCount) {
        val item = controller.getMediaItemAt(i)
        add(
          QueueEntry(
            index = i,
            trackId = item.mediaId,
            title = item.mediaMetadata.title?.toString() ?: "Lagu",
            artist = item.mediaMetadata.artist?.toString() ?: "Artis",
            artworkUri = item.mediaMetadata.artworkUri?.toString(),
          )
        )
      }
    }

    currentItem?.mediaId?.let { mediaId ->
      viewModelScope.launch { preferencesManager.saveLastTrackId(mediaId) }
    }

    _playbackState.update { state ->
      state.copy(
        currentMediaId = currentItem?.mediaId ?: state.currentMediaId,
        currentQueueIndex = controller.currentMediaItemIndex,
        queue = queueEntries,
        isPlaying = controller.isPlaying,
        playbackState = controller.playbackState,
        currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
        durationMs = controller.duration.takeIf { it > 0L } ?: 0L,
        repeatMode = controller.repeatMode,
        shuffleEnabled = controller.shuffleModeEnabled,
        playbackSpeed = controller.playbackParameters.speed,
        isConnected = true,
      )
    }
  }

  private fun startPositionTicker() {
    viewModelScope.launch {
      while (isActive) {
        val controller = mediaController
        if (controller != null) {
          val nowElapsed = SystemClock.elapsedRealtime()
          _playbackState.update { state ->
            val remaining =
              if (state.sleepTimerEndElapsedMs > nowElapsed) {
                state.sleepTimerEndElapsedMs - nowElapsed
              } else {
                0L
              }
            state.copy(
              currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
              durationMs = controller.duration.takeIf { it > 0L } ?: state.durationMs,
              sleepTimerRemainingMs = remaining,
            )
          }
        }
        delay(250L)
      }
    }
  }

  private fun updateSleepTimerEnd(endElapsedMs: Long) {
    val now = SystemClock.elapsedRealtime()
    val remaining = if (endElapsedMs > now) endElapsedMs - now else 0L
    _playbackState.update {
      it.copy(sleepTimerEndElapsedMs = endElapsedMs, sleepTimerRemainingMs = remaining)
    }
  }

  fun playTrackFromList(track: TrackEntity, list: List<TrackEntity>) {
    val controller = mediaController ?: return
    val startIndex = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    controller.setMediaItems(list.map { it.toMediaItem() }, startIndex, 0L)
    controller.prepare()
    controller.play()
  }

  fun playQueueIndex(index: Int) {
    val controller = mediaController ?: return
    if (index in 0 until controller.mediaItemCount) {
      controller.seekToDefaultPosition(index)
      controller.prepare()
      controller.play()
    }
  }

  fun togglePlayPause() {
    val controller = mediaController ?: return
    if (controller.isPlaying) {
      controller.pause()
    } else {
      if (controller.playbackState == Player.STATE_IDLE) {
        controller.prepare()
      }
      controller.play()
    }
  }

  fun skipToNext() {
    val controller = mediaController ?: return
    if (controller.hasNextMediaItem()) {
      controller.seekToNextMediaItem()
    } else if (controller.mediaItemCount > 0) {
      controller.seekToDefaultPosition(0)
    }
    controller.play()
  }

  fun skipToPrevious() {
    val controller = mediaController ?: return
    if (controller.currentPosition > 3_000L) {
      controller.seekTo(0L)
    } else if (controller.hasPreviousMediaItem()) {
      controller.seekToPreviousMediaItem()
    } else if (controller.mediaItemCount > 0) {
      controller.seekToDefaultPosition(controller.mediaItemCount - 1)
    }
    controller.play()
  }

  fun seekTo(positionMs: Long) {
    mediaController?.seekTo(positionMs.coerceAtLeast(0L))
    _playbackState.update { it.copy(currentPositionMs = positionMs.coerceAtLeast(0L)) }
  }

  fun cycleRepeatMode() {
    val controller = mediaController ?: return
    val nextMode =
      when (controller.repeatMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
      }
    controller.repeatMode = nextMode
    viewModelScope.launch { preferencesManager.saveRepeatMode(nextMode) }
  }

  fun toggleShuffle() {
    val controller = mediaController ?: return
    val nextShuffle = !controller.shuffleModeEnabled
    controller.shuffleModeEnabled = nextShuffle
    viewModelScope.launch { preferencesManager.saveShuffleEnabled(nextShuffle) }
  }

  fun setPlaybackSpeed(speed: Float) {
    val controller = mediaController ?: return
    controller.setPlaybackSpeed(speed)
    viewModelScope.launch { preferencesManager.savePlaybackSpeed(speed) }
  }

  fun setSleepTimerMinutes(minutes: Int) {
    val controller = mediaController ?: return
    val args = Bundle().apply { putInt(PlaybackService.ARG_SLEEP_MINUTES, minutes) }
    val resultFuture =
      controller.sendCustomCommand(
        SessionCommand(PlaybackService.COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY),
        args,
      )
    resultFuture.addListener(
      {
        try {
          val result = resultFuture.get()
          val endMs = result.extras.getLong(PlaybackService.EXTRA_SLEEP_TIMER_END_MS, 0L)
          updateSleepTimerEnd(endMs)
        } catch (_: Exception) {}
      },
      ContextCompat.getMainExecutor(getApplication()),
    )
  }

  fun toggleFavorite(track: TrackEntity) {
    viewModelScope.launch { repository.toggleFavorite(track.id, track.isFavorite) }
  }

  fun deleteLocalTrack(track: TrackEntity) {
    viewModelScope.launch {
      repository.deleteLocalTrack(track)
      val controller = mediaController ?: return@launch
      for (i in 0 until controller.mediaItemCount) {
        if (controller.getMediaItemAt(i).mediaId == track.id) {
          controller.removeMediaItem(i)
          break
        }
      }
    }
  }

  fun importLocalAudio(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      val added = repository.importAudioUris(uris)
      if (added > 0) {
        _statusBanner.value = "$added lagu berhasil ditambahkan"
        val updatedTracks = repository.allTracks.first()
        val controller = mediaController
        if (controller != null && !controller.isPlaying && controller.mediaItemCount <= 4) {
          controller.setMediaItems(updatedTracks.map { it.toMediaItem() })
          controller.prepare()
        }
      }
    }
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun clearStatusBanner() {
    _statusBanner.value = null
  }

  private fun TrackEntity.toMediaItem(): MediaItem {
    val metadataBuilder =
      MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
    artworkFileUri?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }

    return MediaItem.Builder()
      .setMediaId(id)
      .setUri(Uri.parse(uriString))
      .setMediaMetadata(metadataBuilder.build())
      .build()
  }

  override fun onCleared() {
    controllerFuture?.let { MediaController.releaseFuture(it) }
    mediaController = null
    super.onCleared()
  }
}
