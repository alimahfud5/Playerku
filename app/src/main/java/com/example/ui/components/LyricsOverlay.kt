package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.R
import com.example.data.LrcParser
import com.example.data.LyricLine
import com.example.data.TrackEntity
import com.example.ui.PlayerUiState

@Composable
fun NowPlayingLyricsBody(
  uiState: PlayerUiState,
  track: TrackEntity?,
  displayedProgress: Float,
  durationMs: Long,
  onScrubChange: (Float) -> Unit,
  onScrubFinished: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onTogglePlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  onSkipPrevious: () -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onToggleFavorite: () -> Unit,
  onOpenSleepTimer: () -> Unit,
  onOpenSpeedDialog: () -> Unit,
  onOpenEqualizer: () -> Unit,
  onEditLyrics: () -> Unit,
  onImportLrc: () -> Unit,
  onCloseLyrics: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val parsedLines = remember(track?.lyricsText) { LrcParser.parse(track?.lyricsText) }
  val isTimed = remember(parsedLines) { parsedLines.any { it.timestampMs != null } }
  val activeLineIndex =
    remember(parsedLines, uiState.currentPositionMs, isTimed) {
      if (isTimed) LrcParser.findActiveIndex(parsedLines, uiState.currentPositionMs) else -1
    }

  val listState = rememberLazyListState()
  var showMoreMenu by remember { mutableStateOf(false) }

  LaunchedEffect(activeLineIndex) {
    if (activeLineIndex >= 0 && parsedLines.isNotEmpty()) {
      val viewportHeight = listState.layoutInfo.viewportSize.height
      val offset = if (viewportHeight > 0) -(viewportHeight / 3) else -180
      listState.animateScrollToItem(index = activeLineIndex, scrollOffset = offset)
    }
  }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = stringResource(R.string.lyrics_label),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.secondary,
        )
        if (isTimed) {
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
          ) {
            Text(
              text = "LRC",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
      }

      IconButton(
        onClick = onCloseLyrics,
        modifier = Modifier.size(48.dp).testTag("lyrics_close_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = stringResource(R.string.cd_close_lyrics),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Box(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentAlignment = Alignment.Center,
    ) {
      if (parsedLines.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Box(
            modifier =
              Modifier.size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Lyrics,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(30.dp),
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = stringResource(R.string.lyrics_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.lyrics_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(20.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = onImportLrc,
              enabled = track != null,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.heightIn(min = 48.dp).testTag("lyrics_import_lrc_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.FileOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.import_lrc_file))
            }
            Button(
              onClick = onEditLyrics,
              enabled = track != null,
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
              modifier = Modifier.heightIn(min = 48.dp).testTag("lyrics_write_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.write_or_paste_lyrics))
            }
          }
        }
      } else {
        LazyColumn(
          state = listState,
          contentPadding = PaddingValues(vertical = 44.dp, horizontal = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier.fillMaxSize()
              .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
              .drawWithContent {
                drawContent()
                drawRect(
                  brush =
                    Brush.verticalGradient(
                      0.0f to Color.Transparent,
                      0.12f to Color.Black,
                      0.88f to Color.Black,
                      1.0f to Color.Transparent,
                    ),
                  blendMode = BlendMode.DstIn,
                )
              }
              .testTag("lyrics_scroll_list"),
        ) {
          itemsIndexed(parsedLines) { index, line ->
            val isActive = index == activeLineIndex
            LyricLineRow(
              line = line,
              isActive = isActive,
              isTimed = isTimed,
              onSeekTo = onSeekTo,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
      modifier = Modifier.fillMaxWidth().testTag("lyrics_compact_player_card"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = track?.title ?: "Pilih Lagu",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = track?.artist ?: "Artis tidak dikenal",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.secondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
          value = displayedProgress,
          onValueChange = onScrubChange,
          onValueChangeFinished = onScrubFinished,
          colors =
            SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.onSurface,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            ),
          modifier = Modifier.fillMaxWidth().testTag("lyrics_seek_slider"),
        )

        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
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

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(48.dp).testTag("lyrics_shuffle_button"),
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
            modifier = Modifier.size(48.dp).testTag("lyrics_previous_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.SkipPrevious,
              contentDescription = stringResource(R.string.cd_previous),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(30.dp),
            )
          }

          FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(58.dp).testTag("lyrics_play_pause_button"),
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
              modifier = Modifier.size(32.dp),
            )
          }

          IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(48.dp).testTag("lyrics_next_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.SkipNext,
              contentDescription = stringResource(R.string.cd_next),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(30.dp),
            )
          }

          IconButton(
            onClick = onCycleRepeat,
            modifier = Modifier.size(48.dp).testTag("lyrics_repeat_button"),
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

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
          Box(modifier = Modifier.align(Alignment.Center)) {
            Surface(
              onClick = { showMoreMenu = true },
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
              modifier = Modifier.heightIn(min = 48.dp).testTag("lyrics_more_pill"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.KeyboardArrowUp,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.lyrics_more),
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }

            DropdownMenu(
              expanded = showMoreMenu,
              onDismissRequest = { showMoreMenu = false },
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_lyrics_title)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  onEditLyrics()
                },
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.import_lrc_file)) },
                leadingIcon = { Icon(Icons.Rounded.FileOpen, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  onImportLrc()
                },
              )
              DropdownMenuItem(
                text = {
                  Text(if (track?.isFavorite == true) "Hapus dari Suka" else "Tambahkan ke Suka")
                },
                leadingIcon = {
                  Icon(
                    imageVector =
                      if (track?.isFavorite == true) Icons.Rounded.Favorite
                      else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showMoreMenu = false
                  onToggleFavorite()
                },
              )
              DropdownMenuItem(
                text = { Text("Kecepatan (${uiState.playbackSpeed}x)") },
                leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  onOpenSpeedDialog()
                },
              )
              DropdownMenuItem(
                text = { Text("Equalizer") },
                leadingIcon = { Icon(Icons.Rounded.Equalizer, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  onOpenEqualizer()
                },
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.sleep_timer)) },
                leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  onOpenSleepTimer()
                },
              )
            }
          }

          Surface(
            onClick = onCloseLyrics,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterEnd).heightIn(min = 48.dp).testTag("lyrics_active_pill"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.lyrics_label),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LyricLineRow(
  line: LyricLine,
  isActive: Boolean,
  isTimed: Boolean,
  onSeekTo: (Long) -> Unit,
) {
  val targetColor =
    when {
      isActive -> MaterialTheme.colorScheme.onBackground
      isTimed -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f)
      else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    }
  val textColor by animateColorAsState(
    targetValue = targetColor,
    animationSpec = tween(durationMillis = 220),
    label = "lyric_line_color",
  )

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(enabled = line.timestampMs != null) {
          line.timestampMs?.let(onSeekTo)
        }
        .padding(horizontal = 12.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = line.text,
      fontSize = if (isActive) 21.sp else 17.sp,
      lineHeight = if (isActive) 28.sp else 24.sp,
      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
      color = textColor,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun EditLyricsDialog(
  initialLyrics: String,
  onSave: (String?) -> Unit,
  onImportLrc: () -> Unit,
  onDismiss: () -> Unit,
) {
  var text by remember(initialLyrics) { mutableStateOf(initialLyrics) }
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = { Text(stringResource(R.string.edit_lyrics_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Tempel teks biasa atau format waktu LRC (contoh: [00:12.50] Baris lirik).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          placeholder = { Text("[00:10.00] Tulis lirik di sini...") },
          minLines = 6,
          maxLines = 12,
          modifier = Modifier.fillMaxWidth().testTag("edit_lyrics_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          TextButton(
            onClick = {
              onDismiss()
              onImportLrc()
            }
          ) {
            Icon(
              imageVector = Icons.Rounded.FileOpen,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.import_lrc_file))
          }
          if (text.isNotBlank()) {
            TextButton(onClick = { text = "" }) {
              Text("Kosongkan", color = MaterialTheme.colorScheme.error)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onSave(text.trim().takeIf { it.isNotEmpty() })
          onDismiss()
        },
        modifier = Modifier.testTag("save_lyrics_button"),
      ) {
        Text("Simpan", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
  )
}
