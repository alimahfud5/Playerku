package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.MediaType
import com.example.data.TrackEntity
import com.example.ui.PlayerUiState
import com.example.ui.PlayerViewModel
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EditLyricsDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingScreen
import com.example.ui.components.PlaybackSpeedDialog
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.TrackOptionsSheet
import com.example.ui.components.VideoPlayerOverlay
import com.example.ui.theme.SonicGlowTop

enum class AlunanMainTab {
  MUSIC,
  VIDEO,
  SEARCH,
  SETTINGS,
}

@Composable
fun AlunanMainScreen(
  uiState: PlayerUiState,
  viewModel: PlayerViewModel,
  onRequestPip: () -> Unit,
) {
  val context = LocalContext.current

  var selectedTab by rememberSaveable { mutableStateOf(AlunanMainTab.MUSIC) }
  var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
  var showSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
  var showSpeedDialog by rememberSaveable { mutableStateOf(false) }
  var showEqualizerDialog by rememberSaveable { mutableStateOf(false) }
  var showEditLyricsDialog by rememberSaveable { mutableStateOf(false) }
  var createPlaylistForType by rememberSaveable { mutableStateOf<String?>(null) }
  var menuTrack by remember { mutableStateOf<TrackEntity?>(null) }
  var addToPlaylistTrack by remember { mutableStateOf<TrackEntity?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.statusBannerMessage) {
    uiState.statusBannerMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusBanner()
    }
  }

  val notificationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = {},
    )

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  val audioPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenMultipleDocuments(),
      onResult = { uris -> viewModel.importLocalAudio(uris) },
    )

  val videoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(),
      onResult = { uris -> viewModel.importLocalVideos(uris) },
    )

  var m3uTargetType by remember { mutableStateOf(MediaType.AUDIO) }
  val m3uPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
      onResult = { uri -> uri?.let { viewModel.importM3u(it, m3uTargetType) } },
    )

  val backupCreateLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/json"),
      onResult = { uri -> uri?.let { viewModel.exportBackup(it) } },
    )

  val backupRestoreLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
      onResult = { uri -> uri?.let { viewModel.importBackup(it) } },
    )

  val lrcPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
      onResult = { uri ->
        val trackId = uiState.currentTrack?.id
        if (uri != null && trackId != null) {
          viewModel.importLrcFile(trackId, uri)
        }
      },
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors =
              listOf(
                if (uiState.preferences.themeMode == com.example.ui.theme.AppThemeMode.LIGHT)
                  MaterialTheme.colorScheme.background
                else SonicGlowTop.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.background,
              )
          )
        )
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useRail = maxWidth >= 600.dp

      Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
          ) {
            Spacer(modifier = Modifier.height(16.dp))
            AlunanMainTab.entries.forEach { tab ->
              val selected = selectedTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { selectedTab = tab },
                icon = { MainTabIcon(tab = tab, selected = selected) },
                label = { Text(mainTabLabel(tab)) },
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  ),
              )
            }
          }
        }

        Scaffold(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          snackbarHost = { SnackbarHost(snackbarHostState) },
          bottomBar = {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(
                    Brush.verticalGradient(
                      colors =
                        listOf(
                          Color.Transparent,
                          MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                        )
                    )
                  )
            ) {
              MiniPlayerBar(
                uiState = uiState,
                onExpand = { isNowPlayingExpanded = true },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSkipNext = viewModel::skipToNext,
              )
              if (!useRail) {
                NavigationBar(
                  containerColor = MaterialTheme.colorScheme.surface,
                  tonalElevation = 0.dp,
                ) {
                  AlunanMainTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                      selected = selected,
                      onClick = { selectedTab = tab },
                      icon = { MainTabIcon(tab = tab, selected = selected) },
                      label = {
                        Text(text = mainTabLabel(tab), style = MaterialTheme.typography.bodySmall)
                      },
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = MaterialTheme.colorScheme.secondary,
                          selectedTextColor = MaterialTheme.colorScheme.secondary,
                          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                      modifier = Modifier.testTag("main_nav_${tab.name.lowercase()}"),
                    )
                  }
                }
              } else {
                Spacer(modifier = Modifier.navigationBarsPadding())
              }
            }
          },
        ) { innerPadding ->
          Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).statusBarsPadding(),
            contentAlignment = Alignment.TopCenter,
          ) {
            Box(modifier = Modifier.widthIn(max = 780.dp).fillMaxSize()) {
              when (selectedTab) {
                AlunanMainTab.MUSIC ->
                  MusicTabScreen(
                    uiState = uiState,
                    onShuffleAll = viewModel::shuffleAllAudio,
                    onSelectSort = viewModel::setSortOrder,
                    onPlayTrack = viewModel::playTrackFromList,
                    onOpenTrackMenu = { menuTrack = it },
                    onImportAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    onImportM3u = {
                      m3uTargetType = MediaType.AUDIO
                      m3uPickerLauncher.launch(
                        arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "*/*")
                      )
                    },
                    onCreatePlaylist = { createPlaylistForType = MediaType.AUDIO },
                    onDeletePlaylist = viewModel::deletePlaylist,
                  )

                AlunanMainTab.VIDEO ->
                  VideoTabScreen(
                    uiState = uiState,
                    onPickVideos = {
                      videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                      )
                    },
                    onPlayVideo = viewModel::openVideoTrack,
                    onOpenTrackMenu = { menuTrack = it },
                    onCreatePlaylist = { createPlaylistForType = MediaType.VIDEO },
                    onImportM3u = {
                      m3uTargetType = MediaType.VIDEO
                      m3uPickerLauncher.launch(arrayOf("*/*"))
                    },
                  )

                AlunanMainTab.SEARCH ->
                  SearchTabScreen(
                    uiState = uiState,
                    onQueryChange = viewModel::updateSearchQuery,
                    onPlayTrack = viewModel::playTrackFromList,
                    onOpenTrackMenu = { menuTrack = it },
                  )

                AlunanMainTab.SETTINGS ->
                  SettingsTabScreen(
                    uiState = uiState,
                    onSelectLanguage = viewModel::setAppLanguage,
                    onSelectTheme = viewModel::setThemeMode,
                    onOpenEqualizer = { showEqualizerDialog = true },
                    onOpenSpeedDialog = { showSpeedDialog = true },
                    onToggleSmoothFade = viewModel::setSmoothFade,
                    onToggleGapless = viewModel::setGapless,
                    onToggleVolumeNorm = viewModel::setVolumeNormalization,
                    onToggleAutoLoadLyrics = viewModel::setAutoLoadLyrics,
                    onOpenSleepTimer = { showSleepTimerDialog = true },
                    onToggleAutoPip = viewModel::setAutoPip,
                    onImportAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    onBackupData = { backupCreateLauncher.launch("alunan_backup.json") },
                    onRestoreData = {
                      backupRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    onClearCache = viewModel::clearCache,
                  )
              }
            }
          }
        }
      }
    }

    AnimatedVisibility(
      visible = isNowPlayingExpanded,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
      NowPlayingScreen(
        uiState = uiState,
        onMinimize = { isNowPlayingExpanded = false },
        onTogglePlayPause = viewModel::togglePlayPause,
        onSkipNext = viewModel::skipToNext,
        onSkipPrevious = viewModel::skipToPrevious,
        onSeekTo = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeatMode,
        onToggleFavorite = { uiState.currentTrack?.let { viewModel.toggleFavorite(it) } },
        onOpenSleepTimer = { showSleepTimerDialog = true },
        onOpenSpeedDialog = { showSpeedDialog = true },
        onOpenEqualizer = { showEqualizerDialog = true },
        onEditLyrics = { if (uiState.currentTrack != null) showEditLyricsDialog = true },
        onImportLrc = {
          if (uiState.currentTrack != null) {
            lrcPickerLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
          }
        },
      )
    }

    uiState.activeVideoTrack?.let { video ->
      VideoPlayerOverlay(
        video = video,
        onClose = { pos -> viewModel.closeVideoTrack(pos) },
        onEnterPip = onRequestPip,
      )
    }

    menuTrack?.let { track ->
      TrackOptionsSheet(
        track = track,
        onPlayNow = {
          val list =
            if (track.mediaType == MediaType.VIDEO) uiState.videoTracks else uiState.audioTracks
          viewModel.playTrackFromList(track, list)
        },
        onToggleFavorite = { viewModel.toggleFavorite(track) },
        onAddToPlaylist = { addToPlaylistTrack = track },
        onDelete = if (!track.isBundled) ({ viewModel.deleteLocalTrack(track) }) else null,
        onDismiss = { menuTrack = null },
      )
    }

    addToPlaylistTrack?.let { track ->
      val playlists =
        if (track.mediaType == MediaType.VIDEO) uiState.videoPlaylists else uiState.audioPlaylists
      AddToPlaylistDialog(
        playlists = playlists,
        onSelectPlaylist = { plId -> viewModel.addTrackToPlaylist(plId, track.id) },
        onCreateNew = { createPlaylistForType = track.mediaType },
        onDismiss = { addToPlaylistTrack = null },
      )
    }

    createPlaylistForType?.let { type ->
      CreatePlaylistDialog(
        onCreate = { name -> viewModel.createPlaylist(name, type) },
        onDismiss = { createPlaylistForType = null },
      )
    }

    if (showSleepTimerDialog) {
      SleepTimerDialog(
        remainingMs = uiState.sleepTimerRemainingMs,
        onSelectMinutes = viewModel::setSleepTimerMinutes,
        onDismiss = { showSleepTimerDialog = false },
      )
    }

    if (showSpeedDialog) {
      PlaybackSpeedDialog(
        currentSpeed = uiState.playbackSpeed,
        onSelectSpeed = viewModel::setPlaybackSpeed,
        onDismiss = { showSpeedDialog = false },
      )
    }

    if (showEqualizerDialog) {
      EqualizerDialog(
        bands = uiState.eqBands,
        presets = uiState.eqPresets,
        selectedPreset = uiState.preferences.eqPresetName,
        onSelectPreset = viewModel::applyEqPreset,
        onBandChange = viewModel::setEqBandLevel,
        onReset = viewModel::resetEqualizer,
        onDismiss = { showEqualizerDialog = false },
      )
    }

    if (showEditLyricsDialog) {
      val currentTrack = uiState.currentTrack
      if (currentTrack != null) {
        EditLyricsDialog(
          initialLyrics = currentTrack.lyricsText.orEmpty(),
          onSave = { lyrics -> viewModel.saveTrackLyrics(currentTrack.id, lyrics) },
          onImportLrc = {
            lrcPickerLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
          },
          onDismiss = { showEditLyricsDialog = false },
        )
      }
    }
  }
}

@Composable
private fun MainTabIcon(tab: AlunanMainTab, selected: Boolean) {
  val icon =
    when (tab) {
      AlunanMainTab.MUSIC -> if (selected) Icons.Rounded.MusicNote else Icons.Outlined.MusicNote
      AlunanMainTab.VIDEO ->
        if (selected) Icons.Rounded.VideoLibrary else Icons.Outlined.VideoLibrary
      AlunanMainTab.SEARCH -> if (selected) Icons.Rounded.Search else Icons.Outlined.Search
      AlunanMainTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
    }
  Icon(imageVector = icon, contentDescription = mainTabLabel(tab))
}

@Composable
private fun mainTabLabel(tab: AlunanMainTab): String =
  when (tab) {
    AlunanMainTab.MUSIC -> stringResource(R.string.nav_music)
    AlunanMainTab.VIDEO -> stringResource(R.string.nav_video)
    AlunanMainTab.SEARCH -> stringResource(R.string.nav_search)
    AlunanMainTab.SETTINGS -> stringResource(R.string.nav_settings)
  }
