package com.example.ui.components

import android.net.Uri
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.R
import com.example.data.PlaylistWithTracks
import com.example.data.TrackEntity
import com.example.ui.EqBandUi
import com.example.ui.PlayerUiState
import kotlinx.coroutines.delay

@Composable
fun MiniPlayerBar(
  uiState: PlayerUiState,
  onExpand: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val track = uiState.currentTrack ?: return
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .shadow(14.dp, RoundedCornerShape(20.dp))
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onExpand)
        .testTag("mini_player_bar"),
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    shape = RoundedCornerShape(20.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TrackArtworkImage(
        artworkUri = track.artworkFileUri,
        contentDescription = stringResource(R.string.cd_album_art, track.title),
        modifier = Modifier.size(46.dp),
        cornerRadius = 12.dp,
        showBorder = true,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
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
      }
      IconButton(
        onClick = onTogglePlayPause,
        modifier = Modifier.size(48.dp).testTag("mini_play_pause_button"),
      ) {
        Icon(
          imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
          contentDescription = stringResource(R.string.cd_play_pause),
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(28.dp),
        )
      }
      IconButton(
        onClick = onSkipNext,
        modifier = Modifier.size(48.dp).testTag("mini_next_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.SkipNext,
          contentDescription = stringResource(R.string.cd_next),
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(26.dp),
        )
      }
    }
  }
}

@Composable
fun NowPlayingScreen(
  uiState: PlayerUiState,
  onMinimize: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  onSkipPrevious: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onToggleFavorite: () -> Unit,
  onOpenSleepTimer: () -> Unit,
  onOpenSpeedDialog: () -> Unit,
  onOpenEqualizer: () -> Unit,
  onEditLyrics: () -> Unit,
  onImportLrc: () -> Unit,
) {
  var showLyrics by rememberSaveable { mutableStateOf(false) }

  BackHandler {
    if (showLyrics) {
      showLyrics = false
    } else {
      onMinimize()
    }
  }

  val track = uiState.currentTrack
  val durationMs = uiState.durationMs.coerceAtLeast(1L)
  val rawProgress = (uiState.currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

  var isScrubbing by remember { mutableStateOf(false) }
  var scrubFraction by remember { mutableFloatStateOf(rawProgress) }
  val displayedProgress = if (isScrubbing) scrubFraction else rawProgress

  val infiniteSpin = rememberInfiniteTransition(label = "vinyl_spin")
  val pulse by
    infiniteSpin.animateFloat(
      initialValue = 1f,
      targetValue = 1.02f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "vinyl_pulse",
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors =
              listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(
                  alpha = if (showLyrics) 0.55f else 0.65f
                ),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface,
              )
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 560.dp)
          .fillMaxSize()
          .padding(horizontal = 20.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          IconButton(
            onClick = onMinimize,
            modifier = Modifier.size(48.dp).testTag("now_playing_minimize_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.KeyboardArrowDown,
              contentDescription = stringResource(R.string.cd_minimize),
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(30.dp),
            )
          }

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
            border =
              BorderStroke(
                1.dp,
                if (uiState.isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.outline,
              ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier.size(7.dp)
                    .clip(CircleShape)
                    .background(
                      if (uiState.isPlaying) MaterialTheme.colorScheme.tertiary
                      else MaterialTheme.colorScheme.onSurfaceVariant
                    )
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text =
                  if (showLyrics) stringResource(R.string.now_playing)
                  else if (uiState.isPlaying) stringResource(R.string.background_active)
                  else stringResource(R.string.now_playing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onOpenEqualizer,
            modifier = Modifier.size(48.dp).testTag("now_playing_eq_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Equalizer,
              contentDescription = stringResource(R.string.cd_equalizer),
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
          IconButton(
            onClick = onOpenSleepTimer,
            modifier = Modifier.size(48.dp).testTag("now_playing_sleep_top_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Bedtime,
              contentDescription = stringResource(R.string.sleep_timer),
              tint =
                if (uiState.sleepTimerRemainingMs > 0L) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
            )
          }
          IconButton(
            onClick = onEditLyrics,
            enabled = track != null,
            modifier = Modifier.size(48.dp).testTag("now_playing_edit_lyrics_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = stringResource(R.string.cd_edit_lyrics),
              tint =
                if (!track?.lyricsText.isNullOrBlank()) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onBackground,
            )
          }
        }
      }

      if (showLyrics) {
        Spacer(modifier = Modifier.height(6.dp))
        NowPlayingLyricsBody(
          uiState = uiState,
          track = track,
          displayedProgress = displayedProgress,
          durationMs = durationMs,
          onScrubChange = { value ->
            isScrubbing = true
            scrubFraction = value
          },
          onScrubFinished = {
            isScrubbing = false
            onSeekTo((scrubFraction * durationMs).toLong())
          },
          onSeekTo = onSeekTo,
          onTogglePlayPause = onTogglePlayPause,
          onSkipNext = onSkipNext,
          onSkipPrevious = onSkipPrevious,
          onToggleShuffle = onToggleShuffle,
          onCycleRepeat = onCycleRepeat,
          onToggleFavorite = onToggleFavorite,
          onOpenSleepTimer = onOpenSleepTimer,
          onOpenSpeedDialog = onOpenSpeedDialog,
          onOpenEqualizer = onOpenEqualizer,
          onEditLyrics = onEditLyrics,
          onImportLrc = onImportLrc,
          onCloseLyrics = { showLyrics = false },
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      } else {
        Column(
          modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Spacer(modifier = Modifier.height(14.dp))

      Box(
        modifier =
          Modifier.size(268.dp).graphicsLayer {
            val s = if (uiState.isPlaying) pulse else 1f
            scaleX = s
            scaleY = s
          },
        contentAlignment = Alignment.Center,
      ) {
        TrackArtworkImage(
          artworkUri = track?.artworkFileUri,
          contentDescription = stringResource(R.string.cd_album_art, track?.title ?: ""),
          modifier =
            Modifier.size(252.dp)
              .shadow(24.dp, RoundedCornerShape(28.dp))
              .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp)),
          cornerRadius = 28.dp,
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      VinylSpectrumVisualizer(
        isPlaying = uiState.isPlaying,
        progressFraction = displayedProgress,
        primaryColor = MaterialTheme.colorScheme.primary,
        inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().height(32.dp),
      )

      Spacer(modifier = Modifier.height(18.dp))

      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = track?.title ?: "Pilih Lagu",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text =
              if (track != null) "${track.artist} • ${track.album}"
              else "Tidak ada lagu yang dimuat",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onToggleFavorite,
          enabled = track != null,
          modifier = Modifier.size(48.dp).testTag("now_playing_favorite_button"),
        ) {
          Icon(
            imageVector =
              if (track?.isFavorite == true) Icons.Rounded.Favorite
              else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(R.string.cd_favorite),
            tint =
              if (track?.isFavorite == true) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Slider(
        value = displayedProgress,
        onValueChange = { value ->
          isScrubbing = true
          scrubFraction = value
        },
        onValueChangeFinished = {
          isScrubbing = false
          onSeekTo((scrubFraction * durationMs).toLong())
        },
        colors =
          SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          ),
        modifier = Modifier.fillMaxWidth().testTag("now_playing_seek_slider"),
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = formatDurationMs((displayedProgress * durationMs).toLong()),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = formatDurationMs(uiState.durationMs),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onToggleShuffle,
          modifier = Modifier.size(48.dp).testTag("now_playing_shuffle_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Shuffle,
            contentDescription = stringResource(R.string.shuffle_all),
            tint =
              if (uiState.shuffleEnabled) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(
          onClick = onSkipPrevious,
          modifier = Modifier.size(56.dp).testTag("now_playing_previous_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.SkipPrevious,
            contentDescription = stringResource(R.string.cd_previous),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(36.dp),
          )
        }

        FilledIconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier.size(72.dp).testTag("now_playing_play_pause_button"),
          shape = CircleShape,
          colors =
            IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
          Icon(
            imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.cd_play_pause),
            modifier = Modifier.size(38.dp),
          )
        }

        IconButton(
          onClick = onSkipNext,
          modifier = Modifier.size(56.dp).testTag("now_playing_next_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = stringResource(R.string.cd_next),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(36.dp),
          )
        }

        IconButton(
          onClick = onCycleRepeat,
          modifier = Modifier.size(48.dp).testTag("now_playing_repeat_button"),
        ) {
          val repeatIcon =
            if (uiState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
            else Icons.Rounded.Repeat
          val repeatActive = uiState.repeatMode != Player.REPEAT_MODE_OFF
          Icon(
            imageVector = repeatIcon,
            contentDescription = "Ulangi",
            tint =
              if (repeatActive) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          onClick = onOpenSleepTimer,
          shape = RoundedCornerShape(16.dp),
          color =
            if (uiState.sleepTimerRemainingMs > 0L) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.weight(1f).height(48.dp).testTag("sleep_timer_button"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Bedtime,
              contentDescription = null,
              tint =
                if (uiState.sleepTimerRemainingMs > 0L) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text =
                if (uiState.sleepTimerRemainingMs > 0L) formatDurationMs(uiState.sleepTimerRemainingMs)
                else "Timer Tidur",
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
            )
          }
        }

        Surface(
          onClick = onOpenSpeedDialog,
          shape = RoundedCornerShape(16.dp),
          color =
            if (uiState.playbackSpeed != 1.0f) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.weight(0.85f).height(48.dp).testTag("playback_speed_button"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Speed,
              contentDescription = null,
              tint =
                if (uiState.playbackSpeed != 1.0f) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "${uiState.playbackSpeed}x", style = MaterialTheme.typography.labelLarge)
          }
        }

        Surface(
          onClick = { showLyrics = true },
          shape = RoundedCornerShape(16.dp),
          color =
            if (!track?.lyricsText.isNullOrBlank()) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.weight(0.95f).height(48.dp).testTag("now_playing_lyrics_toggle"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Lyrics,
              contentDescription = null,
              tint =
                if (!track?.lyricsText.isNullOrBlank()) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.lyrics_label),
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
            )
          }
        }
      }
        Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}

@Composable
fun VideoPlayerOverlay(
  video: TrackEntity,
  onClose: (Long) -> Unit,
  onEnterPip: () -> Unit,
) {
  val context = LocalContext.current
  val exoPlayer = remember(video.id) {
    ExoPlayer.Builder(context).build().apply {
      setMediaItem(MediaItem.fromUri(Uri.parse(video.uriString)))
      if (video.lastPositionMs > 0L) seekTo(video.lastPositionMs)
      prepare()
      playWhenReady = true
    }
  }

  var isPlaying by remember { mutableStateOf(true) }
  var posMs by remember { mutableLongStateOf(0L) }
  var durMs by remember { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }

  BackHandler { onClose(exoPlayer.currentPosition) }

  DisposableEffect(exoPlayer) {
    val listener =
      object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
          isPlaying = playing
        }
      }
    exoPlayer.addListener(listener)
    onDispose {
      exoPlayer.removeListener(listener)
      exoPlayer.release()
    }
  }

  LaunchedEffect(exoPlayer) {
    while (true) {
      posMs = exoPlayer.currentPosition.coerceAtLeast(0L)
      durMs = exoPlayer.duration.takeIf { it > 0L } ?: durMs
      delay(250L)
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()
  ) {
    AndroidView(
      factory = { ctx -> SurfaceView(ctx).apply { exoPlayer.setVideoSurfaceView(this) } },
      modifier = Modifier.fillMaxSize(),
    )

    Row(
      modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = { onClose(exoPlayer.currentPosition) },
        modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
      ) {
        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Tutup", tint = Color.White)
      }
      Text(
        text = video.title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
      )
      IconButton(
        onClick = onEnterPip,
        modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
      ) {
        Icon(
          imageVector = Icons.Rounded.PictureInPictureAlt,
          contentDescription = "Picture in Picture",
          tint = Color.White,
        )
      }
    }

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(Color.Black.copy(alpha = 0.6f))
          .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
      val progress = (posMs.toFloat() / durMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
      Slider(
        value = progress,
        onValueChange = { exoPlayer.seekTo((it * durMs).toLong()) },
        modifier = Modifier.fillMaxWidth(),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${formatDurationMs(posMs)} / ${formatDurationMs(durMs)}",
          style = MaterialTheme.typography.labelMedium,
          color = Color.White,
        )
        FilledIconButton(
          onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
          modifier = Modifier.size(52.dp),
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.cd_play_pause),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
  track: TrackEntity,
  onPlayNow: () -> Unit,
  onToggleFavorite: () -> Unit,
  onAddToPlaylist: () -> Unit,
  onDelete: (() -> Unit)?,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
      Text(
        text = track.title,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = track.artist,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(16.dp))

      SheetOptionItem(
        icon = Icons.Rounded.PlayArrow,
        label = "Putar sekarang",
        onClick = {
          onPlayNow()
          onDismiss()
        },
      )
      SheetOptionItem(
        icon = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        label = if (track.isFavorite) "Hapus dari Suka" else "Tambahkan ke Suka",
        onClick = {
          onToggleFavorite()
          onDismiss()
        },
      )
      SheetOptionItem(
        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
        label = "Tambahkan ke daftar putar",
        onClick = {
          onDismiss()
          onAddToPlaylist()
        },
      )
      if (onDelete != null) {
        SheetOptionItem(
          icon = Icons.Rounded.DeleteOutline,
          label = "Hapus dari pustaka",
          tint = MaterialTheme.colorScheme.error,
          onClick = {
            onDelete()
            onDismiss()
          },
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SheetOptionItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color = MaterialTheme.colorScheme.onSurface,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(52.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = tint)
    Spacer(modifier = Modifier.width(14.dp))
    Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
  }
}

@Composable
fun CreatePlaylistDialog(
  onCreate: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = { Text("Daftar putar baru") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nama daftar putar") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("playlist_name_input"),
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (name.isNotBlank()) {
            onCreate(name.trim())
            onDismiss()
          }
        }
      ) {
        Text("Buat", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}

@Composable
fun AddToPlaylistDialog(
  playlists: List<PlaylistWithTracks>,
  onSelectPlaylist: (String) -> Unit,
  onCreateNew: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = { Text("Pilih daftar putar") },
    text = {
      if (playlists.isEmpty()) {
        Text("Belum ada daftar putar. Buat daftar putar baru terlebih dahulu.")
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(playlists, key = { it.playlist.id }) { pl ->
            Surface(
              onClick = {
                onSelectPlaylist(pl.playlist.id)
                onDismiss()
              },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(pl.playlist.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                  "${pl.tracks.size} item",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onDismiss()
          onCreateNew()
        }
      ) {
        Text("+ Daftar putar baru")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
  )
}

@Composable
fun SleepTimerDialog(
  remainingMs: Long,
  onSelectMinutes: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val options = listOf(0, 5, 15, 30, 60)
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = { Text(stringResource(R.string.sleep_timer)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (remainingMs > 0L) {
          Text(
            text = "Sisa waktu: ${formatDurationMs(remainingMs)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        options.forEach { mins ->
          val label =
            if (mins == 0) stringResource(R.string.sleep_timer_off)
            else stringResource(R.string.sleep_timer_minutes, mins)
          Surface(
            onClick = {
              onSelectMinutes(mins)
              onDismiss()
            },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(text = label, style = MaterialTheme.typography.bodyLarge)
              if (mins == 0 && remainingMs == 0L) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
  )
}

@Composable
fun PlaybackSpeedDialog(
  currentSpeed: Float,
  onSelectSpeed: (Float) -> Unit,
  onDismiss: () -> Unit,
) {
  val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = { Text(stringResource(R.string.playback_speed)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        speeds.forEach { speed ->
          val selected = speed == currentSpeed
          Surface(
            onClick = {
              onSelectSpeed(speed)
              onDismiss()
            },
            shape = RoundedCornerShape(12.dp),
            color =
              if (selected) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                style = MaterialTheme.typography.bodyLarge,
              )
              if (selected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
  )
}

@Composable
fun EqualizerDialog(
  bands: List<EqBandUi>,
  presets: List<String>,
  selectedPreset: String,
  onSelectPreset: (Int, String) -> Unit,
  onBandChange: (Short, Short) -> Unit,
  onReset: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = { Text("Equalizer perangkat keras") },
    text = {
      if (bands.isEmpty()) {
        Text(
          "Putar lagu terlebih dahulu untuk mengaktifkan equalizer perangkat keras.",
          style = MaterialTheme.typography.bodyMedium,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          if (presets.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(presets.indices.toList()) { idx ->
                val name = presets[idx]
                FilterChip(
                  selected = selectedPreset == name,
                  onClick = { onSelectPreset(idx, name) },
                  label = { Text(name) },
                )
              }
            }
          }
          bands.forEach { band ->
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("${band.centerFreqHz} Hz", style = MaterialTheme.typography.labelMedium)
                Text("${band.currentLevelMb / 100} dB", style = MaterialTheme.typography.labelSmall)
              }
              Slider(
                value = band.currentLevelMb.toFloat(),
                onValueChange = { onBandChange(band.bandIndex, it.toInt().toShort()) },
                valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
              )
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Selesai") } },
    dismissButton = {
      if (bands.isNotEmpty()) {
        TextButton(onClick = onReset) { Text("Reset") }
      }
    },
  )
}
