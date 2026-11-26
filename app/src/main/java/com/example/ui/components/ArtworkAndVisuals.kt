package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun TrackArtworkImage(
  artworkUri: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 14.dp,
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(
          Brush.linearGradient(
            colors =
              listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.surfaceContainerHigh,
              )
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.MusicNote,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
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
          animation = tween(durationMillis = 1100, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "eq_phase",
    )

  Canvas(modifier = modifier) {
    val barCount = 4
    val spacing = size.width * 0.12f
    val totalSpacing = spacing * (barCount - 1)
    val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

    for (i in 0 until barCount) {
      val fraction =
        if (isPlaying) {
          0.28f + 0.72f * abs(sin(phase + i * 1.15f))
        } else {
          0.25f + (i % 2) * 0.14f
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
          animation = tween(durationMillis = 1600, easing = LinearEasing),
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
        if (isPlaying) {
          0.27f * abs(sin(phase * 1.4f + i * 0.65f))
        } else {
          0f
        }
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

fun formatDurationMs(ms: Long): String {
  val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%02d:%02d".format(minutes, seconds)
}
