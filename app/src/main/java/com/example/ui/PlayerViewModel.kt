package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.AlunanDatabase
import com.example.data.MediaType
import com.example.data.MusicRepository
import com.example.data.PlayerPreferences
import com.example.data.PlaylistWithTracks
import com.example.data.PreferencesManager
import com.example.data.TrackEntity
import com.example.playback.PlaybackService
import com.example.ui.theme.AppThemeMode
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

enum class SortOrder(val label: String) {
  DEFAULT("Bawaan"),
  TITLE("Judul (A–Z)"),
  ARTIST("Artis (A–Z)"),
  DURATION("Durasi"),
}

data class QueueEntry(
  val index: Int,
  val trackId: String,
  val title: String,
  val artist: String,
  val artworkUri: String?,
)

data class EqBandUi(
  val bandIndex: Short,
  val centerFreqHz: Int,
  val minLevelMb: Short,
  val maxLevelMb: Short,
  val currentLevelMb: Short,
)

data class PlayerUiState(
  val audioTracks: List<TrackEntity> = emptyList(),
  val videoTracks: List<TrackEntity> = emptyList(),
  val audioPlaylists: List<PlaylistWithTracks> = emptyList(),
  val videoPlaylists: List<PlaylistWithTracks> = emptyList(),
  val queue: List<QueueEntry> = emptyList(),
  val currentTrack: TrackEntity? = null,
  val activeVideoTrack: TrackEntity? = null,
  val currentQueueIndex: Int = -1,
  val isPlaying: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val repeatMode: Int = Player.REPEAT_MODE_ALL,
  val shuffleEnabled: Boolean = false,
  val playbackSpeed: Float = 1.0f,
  val sleepTimerRemainingMs: Long = 0L,
  val audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET,
  val eqBands: List<EqBandUi> = emptyList(),
  val eqPresets: List<String> = emptyList(),
  val preferences: PlayerPreferences = PlayerPreferences(),
  val sortOrder: SortOrder = SortOrder.DEFAULT,
  val searchQuery: String = "",
  val statusBannerMessage: String? = null,
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: MusicRepository
  private val preferencesManager: PreferencesManager
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null
  private var hardwareEqualizer: Equalizer? = null

  private val _playbackState = MutableStateFlow(InternalPlaybackState())
  private val _searchQuery = MutableStateFlow("")
  private val _sortOrder = MutableStateFlow(SortOrder.DEFAULT)
  private val _activeVideoTrack = MutableStateFlow<TrackEntity?>(null)
  private val _statusBanner = MutableStateFlow<String?>(null)
  private val _eqState = MutableStateFlow<Pair<List<EqBandUi>, List<String>>>(emptyList<EqBandUi>() to emptyList())

  private data class InternalPlaybackState(
    val currentMediaId: String? = null,
    val currentQueueIndex: Int = -1,
    val queue: List<QueueEntry> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_ALL,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerEndElapsedMs: Long = 0L,
    val sleepTimerRemainingMs: Long = 0L,
    val audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET,
  )

  val uiState: StateFlow<PlayerUiState>

  init {
    val db = AlunanDatabase.getInstance(application)
    repository = MusicRepository(application, db.trackDao(), db.playlistDao())
    preferencesManager = PreferencesManager(application)

    val libraryFlow =
      combine(
        repository.audioTracks,
        repository.videoTracks,
        repository.audioPlaylists,
        repository.videoPlaylists,
        _sortOrder,
      ) { audio, video, audioPl, videoPl, sort ->
        val sortedAudio =
          when (sort) {
            SortOrder.DEFAULT -> audio
            SortOrder.TITLE -> audio.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> audio.sortedBy { it.artist.lowercase() }
            SortOrder.DURATION -> audio.sortedByDescending { it.durationMs }
          }
        LibrarySnapshot(sortedAudio, video, audioPl, videoPl, sort)
      }

    val controlsFlow =
      combine(
        _playbackState,
        preferencesManager.preferencesFlow,
        _searchQuery,
        _activeVideoTrack,
        _eqState,
      ) { playback, prefs, query, activeVideo, eqPair ->
        ControlsSnapshot(playback, prefs, query, activeVideo, eqPair.first, eqPair.second)
      }

    uiState =
      combine(libraryFlow, controlsFlow, _statusBanner) { lib, ctrl, banner ->
        val activeTrack = lib.audio.firstOrNull { it.id == ctrl.playback.currentMediaId }
        PlayerUiState(
          audioTracks = lib.audio,
          videoTracks = lib.video,
          audioPlaylists = lib.audioPl,
          videoPlaylists = lib.videoPl,
          queue = ctrl.playback.queue,
          currentTrack = activeTrack,
          activeVideoTrack = ctrl.activeVideo,
          currentQueueIndex = ctrl.playback.currentQueueIndex,
          isPlaying = ctrl.playback.isPlaying,
          currentPositionMs = ctrl.playback.currentPositionMs,
          durationMs =
            ctrl.playback.durationMs.takeIf { it > 0L } ?: (activeTrack?.durationMs ?: 0L),
          repeatMode = ctrl.playback.repeatMode,
          shuffleEnabled = ctrl.playback.shuffleEnabled,
          playbackSpeed = ctrl.playback.playbackSpeed,
          sleepTimerRemainingMs = ctrl.playback.sleepTimerRemainingMs,
          audioSessionId = ctrl.playback.audioSessionId,
          eqBands = ctrl.eqBands,
          eqPresets = ctrl.eqPresets,
          preferences = ctrl.prefs,
          sortOrder = lib.sort,
          searchQuery = ctrl.query,
          statusBannerMessage = banner,
        )
      }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000L),
          initialValue = PlayerUiState(),
        )

    viewModelScope.launch { repository.ensureBundledTracksSeeded() }
    connectToPlaybackService()
    startPositionTicker()
  }

  private data class LibrarySnapshot(
    val audio: List<TrackEntity>,
    val video: List<TrackEntity>,
    val audioPl: List<PlaylistWithTracks>,
    val videoPl: List<PlaylistWithTracks>,
    val sort: SortOrder,
  )

  private data class ControlsSnapshot(
    val playback: InternalPlaybackState,
    val prefs: PlayerPreferences,
    val query: String,
    val activeVideo: TrackEntity?,
    val eqBands: List<EqBandUi>,
    val eqPresets: List<String>,
  )

  private fun connectToPlaybackService() {
    val context = getApplication<Application>()
    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future =
      MediaController.Builder(context, sessionToken)
        .setListener(
          object : MediaController.Listener {
            override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
              handleSessionExtras(extras)
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

  private fun handleSessionExtras(extras: Bundle) {
    val endMs = extras.getLong(PlaybackService.EXTRA_SLEEP_TIMER_END_MS, 0L)
    updateSleepTimerEnd(endMs)
    val sessionId = extras.getInt(PlaybackService.EXTRA_AUDIO_SESSION_ID, C.AUDIO_SESSION_ID_UNSET)
    if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != _playbackState.value.audioSessionId) {
      _playbackState.update { it.copy(audioSessionId = sessionId) }
      initHardwareEqualizer(sessionId)
    }
  }

  private fun initHardwareEqualizer(sessionId: Int) {
    if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
    if (hardwareEqualizer != null) return
    try {
      val eq = Equalizer(0, sessionId)
      eq.enabled = true
      hardwareEqualizer = eq
      refreshEqState()
    } catch (_: Exception) {
      hardwareEqualizer = null
    }
  }

  private fun refreshEqState() {
    val eq = hardwareEqualizer ?: return
    try {
      val numBands = eq.numberOfBands
      val range = eq.bandLevelRange
      val bands =
        (0 until numBands).map { i ->
          val idx = i.toShort()
          EqBandUi(
            bandIndex = idx,
            centerFreqHz = eq.getCenterFreq(idx) / 1000,
            minLevelMb = range[0],
            maxLevelMb = range[1],
            currentLevelMb = eq.getBandLevel(idx),
          )
        }
      val presets = (0 until eq.numberOfPresets).map { i -> eq.getPresetName(i.toShort()) }
      _eqState.value = bands to presets
    } catch (_: Exception) {}
  }

  fun applyEqPreset(presetIndex: Int, presetName: String) {
    val eq = hardwareEqualizer ?: return
    try {
      eq.usePreset(presetIndex.toShort())
      refreshEqState()
      viewModelScope.launch { preferencesManager.saveEqPreset(presetName) }
    } catch (_: Exception) {}
  }

  fun resetEqualizer() {
    val eq = hardwareEqualizer ?: return
    try {
      for (i in 0 until eq.numberOfBands) {
        eq.setBandLevel(i.toShort(), 0.toShort())
      }
      refreshEqState()
      viewModelScope.launch { preferencesManager.saveEqPreset("Nonaktif") }
    } catch (_: Exception) {}
  }

  fun setEqBandLevel(bandIndex: Short, levelMb: Short) {
    val eq = hardwareEqualizer ?: return
    try {
      eq.setBandLevel(bandIndex, levelMb)
      refreshEqState()
      viewModelScope.launch { preferencesManager.saveEqPreset("Kustom") }
    } catch (_: Exception) {}
  }

  private suspend fun restoreQueueIfEmpty(controller: MediaController) {
    val prefs = preferencesManager.preferencesFlow.first()
    if (controller.mediaItemCount == 0) {
      val tracks = repository.audioTracks.first()
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
    applyVolumeScale(if (prefs.volumeNormalizationEnabled) 0.85f else 1.0f)
    syncFromController(controller)
  }

  private fun attachControllerListener(controller: MediaController) {
    handleSessionExtras(controller.sessionExtras)
    controller.addListener(
      object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
          syncFromController(controller)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
          mediaItem?.mediaId?.let { id ->
            viewModelScope.launch { repository.recordTrackPlayed(id) }
          }
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
        currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
        durationMs = controller.duration.takeIf { it > 0L } ?: 0L,
        repeatMode = controller.repeatMode,
        shuffleEnabled = controller.shuffleModeEnabled,
        playbackSpeed = controller.playbackParameters.speed,
      )
    }
  }

  private fun startPositionTicker() {
    viewModelScope.launch {
      var ticks = 0
      while (isActive) {
        val controller = mediaController
        if (controller != null) {
          val nowElapsed = SystemClock.elapsedRealtime()
          val pos = controller.currentPosition.coerceAtLeast(0L)
          _playbackState.update { state ->
            val remaining =
              if (state.sleepTimerEndElapsedMs > nowElapsed) {
                state.sleepTimerEndElapsedMs - nowElapsed
              } else {
                0L
              }
            state.copy(
              currentPositionMs = pos,
              durationMs = controller.duration.takeIf { it > 0L } ?: state.durationMs,
              sleepTimerRemainingMs = remaining,
            )
          }
          if (controller.isPlaying && ticks % 12 == 0) {
            controller.currentMediaItem?.mediaId?.let { id ->
              repository.updateTrackPosition(id, pos)
            }
          }
          ticks++
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
    if (track.mediaType == MediaType.VIDEO) {
      openVideoTrack(track)
      return
    }
    val controller = mediaController ?: return
    val startIndex = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    controller.setMediaItems(list.map { it.toMediaItem() }, startIndex, track.lastPositionMs)
    controller.prepare()
    controller.play()
    viewModelScope.launch { repository.recordTrackPlayed(track.id) }
  }

  fun openVideoTrack(track: TrackEntity) {
    mediaController?.pause()
    _activeVideoTrack.value = track
    viewModelScope.launch { repository.recordTrackPlayed(track.id) }
  }

  fun closeVideoTrack(lastPosMs: Long = 0L) {
    val active = _activeVideoTrack.value
    if (active != null && lastPosMs > 0L) {
      viewModelScope.launch { repository.updateTrackPosition(active.id, lastPosMs) }
    }
    _activeVideoTrack.value = null
  }

  fun shuffleAllAudio() {
    val list = uiState.value.audioTracks
    if (list.isEmpty()) return
    val controller = mediaController ?: return
    controller.shuffleModeEnabled = true
    viewModelScope.launch { preferencesManager.saveShuffleEnabled(true) }
    val startIndex = list.indices.random()
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
      if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
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
    mediaController?.setPlaybackSpeed(speed)
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
      val autoLoadLyrics = preferencesManager.preferencesFlow.first().autoLoadLyricsEnabled
      val added = repository.importMediaUris(uris, MediaType.AUDIO, autoLoadLyrics)
      if (added > 0) {
        _statusBanner.value = "$added lagu berhasil ditambahkan"
        val updated = repository.audioTracks.first()
        val controller = mediaController
        if (controller != null && !controller.isPlaying && controller.mediaItemCount <= 4) {
          controller.setMediaItems(updated.map { it.toMediaItem() })
          controller.prepare()
        }
      }
    }
  }

  fun importLocalVideos(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      val added = repository.importMediaUris(uris, MediaType.VIDEO)
      if (added > 0) _statusBanner.value = "$added video berhasil ditambahkan"
    }
  }

  fun saveTrackLyrics(trackId: String, lyrics: String?) {
    viewModelScope.launch {
      repository.updateLyrics(trackId, lyrics)
      _statusBanner.value =
        if (lyrics.isNullOrBlank()) "Lirik dihapus" else "Lirik berhasil disimpan"
    }
  }

  fun importLrcFile(trackId: String, uri: Uri) {
    viewModelScope.launch {
      val ok = repository.importLrcForTrack(trackId, uri)
      _statusBanner.value =
        if (ok) "File lirik berhasil dimuat" else "File lirik kosong atau tidak dapat dibaca"
    }
  }

  fun createPlaylist(name: String, mediaType: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.createPlaylist(name, mediaType)
      _statusBanner.value = "Daftar putar \"$name\" dibuat"
    }
  }

  fun addTrackToPlaylist(playlistId: String, trackId: String) {
    viewModelScope.launch {
      repository.addTrackToPlaylist(playlistId, trackId)
      _statusBanner.value = "Ditambahkan ke daftar putar"
    }
  }

  fun deletePlaylist(playlistId: String) {
    viewModelScope.launch {
      repository.deletePlaylist(playlistId)
      _statusBanner.value = "Daftar putar dihapus"
    }
  }

  fun importM3u(uri: Uri, mediaType: String) {
    viewModelScope.launch {
      val (name, count) = repository.importM3uPlaylist(uri, mediaType)
      _statusBanner.value = "Impor \"$name\" selesai ($count item cocok)"
    }
  }

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { preferencesManager.saveThemeMode(mode) }
  }

  fun setAppLanguage(language: String) {
    viewModelScope.launch {
      preferencesManager.saveAppLanguage(language)
      _statusBanner.value = "Bahasa diatur ke $language"
    }
  }

  fun setSmoothFade(enabled: Boolean) {
    viewModelScope.launch { preferencesManager.saveSmoothFade(enabled) }
  }

  fun setGapless(enabled: Boolean) {
    viewModelScope.launch { preferencesManager.saveGapless(enabled) }
  }

  fun setVolumeNormalization(enabled: Boolean) {
    viewModelScope.launch {
      preferencesManager.saveVolumeNormalization(enabled)
      applyVolumeScale(if (enabled) 0.85f else 1.0f)
    }
  }

  fun setAutoPip(enabled: Boolean) {
    viewModelScope.launch { preferencesManager.saveAutoPip(enabled) }
  }

  fun setAutoLoadLyrics(enabled: Boolean) {
    viewModelScope.launch { preferencesManager.saveAutoLoadLyrics(enabled) }
  }

  private fun applyVolumeScale(scale: Float) {
    val controller = mediaController ?: return
    val args = Bundle().apply { putFloat(PlaybackService.ARG_VOLUME_SCALE, scale) }
    controller.sendCustomCommand(
      SessionCommand(PlaybackService.COMMAND_SET_VOLUME_SCALE, Bundle.EMPTY),
      args,
    )
  }

  fun exportBackup(outputUri: Uri) {
    viewModelScope.launch {
      val count = repository.exportBackupJson(outputUri)
      _statusBanner.value = "Cadangan diekspor ($count item)"
    }
  }

  fun importBackup(inputUri: Uri) {
    viewModelScope.launch {
      val count = repository.importBackupJson(inputUri)
      _statusBanner.value = "Pemulihan selesai ($count item)"
    }
  }

  fun clearCache() {
    viewModelScope.launch {
      val freed = repository.clearArtworkCache()
      _statusBanner.value = "Cache sementara dibersihkan ($freed file)"
    }
  }

  fun setSortOrder(order: SortOrder) {
    _sortOrder.value = order
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
    try {
      hardwareEqualizer?.release()
    } catch (_: Exception) {}
    controllerFuture?.let { MediaController.releaseFuture(it) }
    mediaController = null
    super.onCleared()
  }
}
