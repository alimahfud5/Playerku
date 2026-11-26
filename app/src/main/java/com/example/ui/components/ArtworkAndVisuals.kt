package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.MediaType
import com.example.data.TrackEntity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun TrackArtworkImage(
  artworkUri: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 12.dp,
  showBorder: Boolean = false,
  isVideo: Boolean = false,
) {
  val borderMod =
    if (showBorder) {
      Modifier.border(
        width = 1.5.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
        shape = RoundedCornerShape(cornerRadius),
      )
    } else {
      Modifier
    }

  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .then(borderMod),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = if (isVideo) Icons.Rounded.PlayCircleOutline else Icons.Rounded.MusicNote,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
      modifier = Modifier.fillMaxSize(0.45f),
    )
    if (!artworkUri.isNullOrBlank()) {
      AsyncImage(
        model = artworkUri,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
fun AnimatedEqualizerBars(
  isPlaying: Boolean,
  barColor: Color,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
  val phase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "eq_phase",
    )

  Canvas(modifier = modifier) {
    val barCount = 4
    val spacing = size.width * 0.14f
    val totalSpacing = spacing * (barCount - 1)
    val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

    for (i in 0 until barCount) {
      val fraction =
        if (isPlaying) {
          0.3f + 0.7f * abs(sin(phase + i * 1.15f))
        } else {
          0.32f + (i % 2) * 0.18f
        }
      val barHeight = size.height * fraction
      val left = i * (barWidth + spacing)
      val top = size.height - barHeight
      drawRoundRect(
        color = barColor,
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
      )
    }
  }
}

@Composable
fun VinylSpectrumVisualizer(
  isPlaying: Boolean,
  progressFraction: Float,
  primaryColor: Color,
  inactiveColor: Color,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
  val phase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "spectrum_phase",
    )

  Canvas(modifier = modifier) {
    val barCount = 32
    val gap = size.width * 0.012f
    val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)

    for (i in 0 until barCount) {
      val normalizedIndex = i.toFloat() / barCount.toFloat()
      val baseShape = 0.28f + 0.45f * abs(sin(i * 0.55f + 0.4f))
      val dynamicPulse =
        if (isPlaying) 0.27f * abs(sin(phase * 1.4f + i * 0.65f)) else 0f
      val heightFraction = (baseShape + dynamicPulse).coerceIn(0.18f, 1.0f)
      val barHeight = size.height * heightFraction
      val left = i * (barWidth + gap)
      val top = (size.height - barHeight) / 2f
      val color = if (normalizedIndex <= progressFraction) primaryColor else inactiveColor

      drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
      )
    }
  }
}

@Composable
fun SmartPlaylistCard(
  title: String,
  countText: String,
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(iconBg),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(19.dp),
          )
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Text(
            text = "Otomatis",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = countText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun MediaListItemRow(
  track: TrackEntity,
  isCurrent: Boolean,
  isPlaying: Boolean,
  remainingMs: Long? = null,
  onClick: () -> Unit,
  onMoreClick: () -> Unit,
) {
  val containerColor =
    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    else Color.Transparent

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = containerColor,
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .testTag("track_row_${track.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(contentAlignment = Alignment.Center) {
        TrackArtworkImage(
          artworkUri = track.artworkFileUri,
          contentDescription = track.title,
          modifier = Modifier.size(50.dp),
          cornerRadius = 12.dp,
          isVideo = track.mediaType == MediaType.VIDEO,
        )
        if (isCurrent) {
          Box(
            modifier =
              Modifier.size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
          ) {
            AnimatedEqualizerBars(
              isPlaying = isPlaying,
              barColor = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(width = 20.dp, height = 14.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleMedium,
          color =
            if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        val durationPart =
          when {
            isCurrent && remainingMs != null && remainingMs > 0L ->
              "Tersisa ${formatDurationMs(remainingMs)}"
            track.durationMs > 0L -> formatDurationMs(track.durationMs)
            else -> ""
          }
        val subtitle =
          if (durationPart.isNotEmpty()) "${track.artist} • $durationPart" else track.artist
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      IconButton(
        onClick = onMoreClick,
        modifier = Modifier.size(48.dp).testTag("track_more_${track.id}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.MoreVert,
          contentDescription = "Opsi lagu",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

fun formatDurationMs(ms: Long): String {
  val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%02d:%02d".format(minutes, seconds)
  }
}
