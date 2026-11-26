package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.R
import com.example.data.TrackEntity
import com.example.ui.PlayerUiState
import com.example.ui.PlayerViewModel
import com.example.ui.components.AnimatedEqualizerBars
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingScreen
import com.example.ui.components.PlaybackSpeedDialog
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.TrackArtworkImage
import com.example.ui.components.formatDurationMs

enum class AlunanTab {
  LIBRARY,
  FAVORITES,
  QUEUE,
}

@Composable
fun AlunanMainScreen(viewModel: PlayerViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var selectedTab by rememberSaveable { mutableStateOf(AlunanTab.LIBRARY) }
  var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
  var showSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
  var showSpeedDialog by rememberSaveable { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.statusBannerMessage) {
    uiState.statusBannerMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusBanner()
    }
  }

  val audioPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenMultipleDocuments(),
      onResult = { uris ->
        if (uris.isNotEmpty()) {
          viewModel.importLocalAudio(uris)
        }
      },
    )

  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }
  var dismissedNotificationBanner by rememberSaveable { mutableStateOf(false) }

  val notificationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { granted -> hasNotificationPermission = granted },
    )

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useRail = maxWidth >= 600.dp

      Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
          ) {
            Spacer(modifier = Modifier.height(16.dp))
            AlunanTab.entries.forEach { tab ->
              val selected = selectedTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { selectedTab = tab },
                icon = { TabIcon(tab = tab, selected = selected) },
                label = { Text(tabLabel(tab)) },
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  ),
              )
            }
          }
        }

        Scaffold(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          containerColor = MaterialTheme.colorScheme.background,
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
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipToNext() },
              )
              if (!useRail) {
                NavigationBar(
                  containerColor = MaterialTheme.colorScheme.surface,
                  tonalElevation = 0.dp,
                ) {
                  AlunanTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                      selected = selected,
                      onClick = { selectedTab = tab },
                      icon = { TabIcon(tab = tab, selected = selected) },
                      label = {
                        Text(
                          text = tabLabel(tab),
                          style = MaterialTheme.typography.labelSmall,
                        )
                      },
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = MaterialTheme.colorScheme.primary,
                          selectedTextColor = MaterialTheme.colorScheme.primary,
                          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                      modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
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
            modifier =
              Modifier.fillMaxSize().padding(innerPadding).statusBarsPadding(),
            contentAlignment = Alignment.TopCenter,
          ) {
            Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
              TopAppHeader(
                isPlaying = uiState.isPlaying,
                sleepTimerRemainingMs = uiState.sleepTimerRemainingMs,
                onOpenSleepTimer = { showSleepTimerDialog = true },
                onImportAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
              )

              if (
                !hasNotificationPermission &&
                  !dismissedNotificationBanner &&
                  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
              ) {
                NotificationPermissionCard(
                  onAllow = {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  },
                  onDismiss = { dismissedNotificationBanner = true },
                )
              }

              when (selectedTab) {
                AlunanTab.LIBRARY ->
                  LibraryTabContent(
                    uiState = uiState,
                    onSearchChange = viewModel::updateSearchQuery,
                    onPlayTrack = { track -> viewModel.playTrackFromList(track, uiState.allTracks) },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onToggleShufflePlay = {
                      if (uiState.allTracks.isNotEmpty()) {
                        val randomTrack = uiState.allTracks.random()
                        if (!uiState.shuffleEnabled) viewModel.toggleShuffle()
                        viewModel.playTrackFromList(randomTrack, uiState.allTracks)
                      }
                    },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDeleteTrack = viewModel::deleteLocalTrack,
                    onImportAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                  )

                AlunanTab.FAVORITES ->
                  FavoritesTabContent(
                    uiState = uiState,
                    onPlayTrack = { track ->
                      viewModel.playTrackFromList(track, uiState.favoriteTracks)
                    },
                    onToggleFavorite = viewModel::toggleFavorite,
                  )

                AlunanTab.QUEUE ->
                  QueueTabContent(
                    uiState = uiState,
                    onSelectQueueIndex = viewModel::playQueueIndex,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
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
        onToggleFavorite = {
          uiState.currentTrack?.let { viewModel.toggleFavorite(it) }
        },
        onOpenSleepTimer = { showSleepTimerDialog = true },
        onOpenSpeedDialog = { showSpeedDialog = true },
        onOpenQueue = {
          isNowPlayingExpanded = false
          selectedTab = AlunanTab.QUEUE
        },
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
  }
}

@Composable
private fun TabIcon(tab: AlunanTab, selected: Boolean) {
  val icon =
    when (tab) {
      AlunanTab.LIBRARY ->
        if (selected) Icons.Rounded.LibraryMusic else Icons.Outlined.LibraryMusic
      AlunanTab.FAVORITES ->
        if (selected) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder
      AlunanTab.QUEUE -> Icons.AutoMirrored.Rounded.QueueMusic
    }
  Icon(imageVector = icon, contentDescription = tabLabel(tab))
}

@Composable
private fun tabLabel(tab: AlunanTab): String =
  when (tab) {
    AlunanTab.LIBRARY -> stringResource(R.string.tab_library)
    AlunanTab.FAVORITES -> stringResource(R.string.tab_favorites)
    AlunanTab.QUEUE -> stringResource(R.string.tab_queue)
  }

@Composable
private fun TopAppHeader(
  isPlaying: Boolean,
  sleepTimerRemainingMs: Long,
  onOpenSleepTimer: () -> Unit,
  onImportAudio: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(8.dp)
              .clip(CircleShape)
              .background(
                if (isPlaying) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary
              )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text =
            if (isPlaying) stringResource(R.string.background_active)
            else stringResource(R.string.background_ready),
          style = MaterialTheme.typography.bodySmall,
          color =
            if (isPlaying) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      IconButton(
        onClick = onOpenSleepTimer,
        modifier = Modifier.size(48.dp).testTag("header_sleep_timer_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Bedtime,
          contentDescription = stringResource(R.string.sleep_timer),
          tint =
            if (sleepTimerRemainingMs > 0L) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FilledTonalButton(
        onClick = onImportAudio,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.height(48.dp).testTag("header_import_audio_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Tambah",
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
private fun NotificationPermissionCard(
  onAllow: () -> Unit,
  onDismiss: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.NotificationsActive,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = stringResource(R.string.notification_permission_banner),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onAllow, modifier = Modifier.testTag("allow_notification_button")) {
        Text(
          text = stringResource(R.string.allow_notification),
          color = MaterialTheme.colorScheme.secondary,
        )
      }
      IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "Tutup",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun LibraryTabContent(
  uiState: PlayerUiState,
  onSearchChange: (String) -> Unit,
  onPlayTrack: (TrackEntity) -> Unit,
  onTogglePlayPause: () -> Unit,
  onToggleShufflePlay: () -> Unit,
  onToggleFavorite: (TrackEntity) -> Unit,
  onDeleteTrack: (TrackEntity) -> Unit,
  onImportAudio: () -> Unit,
) {
  val spotlightTrack = uiState.currentTrack ?: uiState.allTracks.firstOrNull()

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (spotlightTrack != null && uiState.searchQuery.isBlank()) {
      item(key = "hero_spotlight") {
        HeroSpotlightCard(
          track = spotlightTrack,
          isCurrentTrack = uiState.currentTrack?.id == spotlightTrack.id,
          isPlaying = uiState.isPlaying,
          onPlayOrToggle = {
            if (uiState.currentTrack?.id == spotlightTrack.id) {
              onTogglePlayPause()
            } else {
              onPlayTrack(spotlightTrack)
            }
          },
          onShuffleAll = onToggleShufflePlay,
        )
      }
    }

    item(key = "search_bar") {
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchChange,
        placeholder = {
          Text(
            text = "Cari judul lagu atau artis...",
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        trailingIcon = {
          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchChange("") }) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Hapus pencarian",
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        modifier = Modifier.fillMaxWidth().testTag("search_tracks_input"),
      )
    }

    item(key = "library_section_title") {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Daftar Putar (${uiState.allTracks.size})",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Ketuk untuk putar",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    items(items = uiState.allTracks, key = { it.id }) { track ->
      val isCurrent = uiState.currentTrack?.id == track.id
      TrackRowCard(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isCurrent && uiState.isPlaying,
        onClick = { onPlayTrack(track) },
        onToggleFavorite = { onToggleFavorite(track) },
        onDelete = if (!track.isBundled) ({ onDeleteTrack(track) }) else null,
      )
    }

    item(key = "import_local_card") {
      Surface(
        onClick = onImportAudio,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("import_local_card"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(26.dp),
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.add_local_audio),
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = stringResource(R.string.import_audio_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item(key = "bottom_spacer") {
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun HeroSpotlightCard(
  track: TrackEntity,
  isCurrentTrack: Boolean,
  isPlaying: Boolean,
  onPlayOrToggle: () -> Unit,
  onShuffleAll: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                  MaterialTheme.colorScheme.surfaceContainer,
                )
            )
          )
          .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TrackArtworkImage(
          artworkUri = track.artworkFileUri,
          contentDescription = stringResource(R.string.cd_album_art, track.title),
          modifier = Modifier.size(96.dp),
          cornerRadius = 18.dp,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isCurrentTrack && isPlaying) "SEDANG BERJALAN" else "SOROTAN AUDIO",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = track.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onPlayOrToggle,
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
              modifier = Modifier.height(42.dp).testTag("hero_play_button"),
            ) {
              Icon(
                imageVector =
                  if (isCurrentTrack && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isCurrentTrack && isPlaying) "Jeda" else "Putar",
                style = MaterialTheme.typography.labelLarge,
              )
            }
            FilledTonalButton(
              onClick = onShuffleAll,
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.height(42.dp).testTag("hero_shuffle_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = stringResource(R.string.shuffle),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TrackRowCard(
  track: TrackEntity,
  isCurrent: Boolean,
  isPlaying: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onDelete: (() -> Unit)?,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color =
      if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh
      else MaterialTheme.colorScheme.surfaceContainer,
    border =
      if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
      else null,
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .testTag("track_card_${track.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(contentAlignment = Alignment.Center) {
        TrackArtworkImage(
          artworkUri = track.artworkFileUri,
          contentDescription = stringResource(R.string.cd_album_art, track.title),
          modifier = Modifier.size(54.dp),
          cornerRadius = 12.dp,
        )
        if (isCurrent) {
          Box(
            modifier =
              Modifier.size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
          ) {
            AnimatedEqualizerBars(
              isPlaying = isPlaying,
              barColor = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(width = 22.dp, height = 18.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleSmall,
          color =
            if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${track.artist} • ${track.album}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      if (track.durationMs > 0L) {
        Text(
          text = formatDurationMs(track.durationMs),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.size(48.dp).testTag("favorite_btn_${track.id}"),
      ) {
        Icon(
          imageVector =
            if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
          contentDescription = stringResource(R.string.cd_favorite),
          tint =
            if (track.isFavorite) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }

      if (onDelete != null) {
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(48.dp).testTag("delete_btn_${track.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.cd_delete_track),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun FavoritesTabContent(
  uiState: PlayerUiState,
  onPlayTrack: (TrackEntity) -> Unit,
  onToggleFavorite: (TrackEntity) -> Unit,
) {
  if (uiState.favoriteTracks.isEmpty()) {
    EmptyStateView(
      icon = Icons.Rounded.FavoriteBorder,
      title = stringResource(R.string.empty_favorites_title),
      description = stringResource(R.string.empty_favorites_desc),
    )
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "favorites_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Lagu Favorit (${uiState.favoriteTracks.size})",
          style = MaterialTheme.typography.titleMedium,
        )
        FilledTonalButton(
          onClick = { onPlayTrack(uiState.favoriteTracks.first()) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(44.dp).testTag("play_all_favorites_button"),
        ) {
          Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.play_all))
        }
      }
    }

    items(items = uiState.favoriteTracks, key = { it.id }) { track ->
      val isCurrent = uiState.currentTrack?.id == track.id
      TrackRowCard(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isCurrent && uiState.isPlaying,
        onClick = { onPlayTrack(track) },
        onToggleFavorite = { onToggleFavorite(track) },
        onDelete = null,
      )
    }
  }
}

@Composable
private fun QueueTabContent(
  uiState: PlayerUiState,
  onSelectQueueIndex: (Int) -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
) {
  if (uiState.queue.isEmpty()) {
    EmptyStateView(
      icon = Icons.AutoMirrored.Rounded.QueueMusic,
      title = stringResource(R.string.empty_queue_title),
      description = stringResource(R.string.empty_queue_desc),
    )
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item(key = "queue_controls") {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          onClick = onToggleShuffle,
          shape = RoundedCornerShape(14.dp),
          color =
            if (uiState.shuffleEnabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.weight(1f).height(48.dp).testTag("queue_shuffle_button"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Shuffle,
              contentDescription = null,
              tint =
                if (uiState.shuffleEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (uiState.shuffleEnabled) "Acak: Aktif" else "Acak: Mati",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        Surface(
          onClick = onCycleRepeat,
          shape = RoundedCornerShape(14.dp),
          color =
            if (uiState.repeatMode != Player.REPEAT_MODE_OFF)
              MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.weight(1f).height(48.dp).testTag("queue_repeat_button"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector =
                if (uiState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                else Icons.Rounded.Repeat,
              contentDescription = null,
              tint =
                if (uiState.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            val repeatLabel =
              when (uiState.repeatMode) {
                Player.REPEAT_MODE_ONE -> "Ulangi 1"
                Player.REPEAT_MODE_ALL -> "Ulangi Semua"
                else -> "Ulangi: Mati"
              }
            Text(text = repeatLabel, style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }

    itemsIndexed(items = uiState.queue, key = { index, item -> "${item.trackId}_$index" }) {
      index,
      entry ->
      val isCurrent = index == uiState.currentQueueIndex
      Surface(
        shape = RoundedCornerShape(16.dp),
        color =
          if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh
          else MaterialTheme.colorScheme.surfaceContainer,
        border =
          if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
          else null,
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelectQueueIndex(index) }
            .testTag("queue_item_$index"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color =
              if (isCurrent) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(26.dp),
          )
          TrackArtworkImage(
            artworkUri = entry.artworkUri,
            contentDescription = stringResource(R.string.cd_album_art, entry.title),
            modifier = Modifier.size(46.dp),
            cornerRadius = 10.dp,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = entry.title,
              style = MaterialTheme.typography.titleSmall,
              color =
                if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = entry.artist,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (isCurrent) {
            AnimatedEqualizerBars(
              isPlaying = uiState.isPlaying,
              barColor = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(width = 20.dp, height = 16.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyStateView(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier =
        Modifier.size(72.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(34.dp),
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 320.dp),
    )
  }
}
