package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AlunanDarkColorScheme =
  darkColorScheme(
    primary = CoralAmber,
    onPrimary = ObsidianBg,
    primaryContainer = CoralAmberDark,
    onPrimaryContainer = WarmGold,
    secondary = WarmGold,
    onSecondary = ObsidianBg,
    secondaryContainer = WarmGoldDark,
    onSecondaryContainer = WarmGold,
    tertiary = TealMist,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = SlateSurface,
    onSurface = IvoryText,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = WarmGrayText,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceCardElevated,
    outline = OutlineSubtle,
    error = DangerRose,
    onError = ObsidianBg,
  )

private val AlunanLightColorScheme =
  lightColorScheme(
    primary = CoralDeep,
    onPrimary = CreamSurface,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3E1200),
    secondary = Color(0xFFB37422),
    onSecondary = CreamSurface,
    tertiary = Color(0xFF0E7C78),
    background = CreamBg,
    onBackground = CharcoalText,
    surface = CreamSurface,
    onSurface = CharcoalText,
    surfaceVariant = CreamCard,
    onSurfaceVariant = CharcoalMuted,
    outline = Color(0xFFDCD6CC),
  )

@Composable
fun AlunanTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) AlunanDarkColorScheme else AlunanLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AlunanTypography,
    content = content,
  )
}
