package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String) {
  SONIC_NOIR("Gelap (Sonic Noir)"),
  MIDNIGHT("Gelap Pekat (Midnight)"),
  LIGHT("Terang"),
}

private val SonicNoirColorScheme =
  darkColorScheme(
    primary = IndigoAccent,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoOnContainer,
    secondary = IndigoBright,
    onSecondary = SonicBg,
    secondaryContainer = SonicCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = MintGreen,
    onTertiary = SonicBg,
    background = SonicBg,
    onBackground = TextPrimary,
    surface = SonicSurface,
    onSurface = TextPrimary,
    surfaceVariant = SonicCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = SonicBg,
    surfaceContainerLow = SonicSurface,
    surfaceContainer = SonicCard,
    surfaceContainerHigh = SonicCardElevated,
    outline = SonicOutline,
    error = HeartPink,
    onError = Color.White,
  )

private val MidnightColorScheme =
  darkColorScheme(
    primary = IndigoAccent,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoOnContainer,
    secondary = IndigoBright,
    onSecondary = MidnightBg,
    secondaryContainer = MidnightCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = MintGreen,
    onTertiary = MidnightBg,
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = MidnightBg,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = MidnightCard,
    surfaceContainerHigh = MidnightCardElevated,
    outline = SonicOutline,
    error = HeartPink,
    onError = Color.White,
  )

private val AlunanLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF4F58E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E2FF),
    onPrimaryContainer = Color(0xFF14186B),
    secondary = Color(0xFF5A62D6),
    onSecondary = Color.White,
    tertiary = Color(0xFF0E9F6E),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightBg,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCardElevated,
    outline = LightOutline,
    error = Color(0xFFD93855),
    onError = Color.White,
  )

@Composable
fun AlunanTheme(
  themeMode: AppThemeMode = AppThemeMode.SONIC_NOIR,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when (themeMode) {
      AppThemeMode.SONIC_NOIR -> SonicNoirColorScheme
      AppThemeMode.MIDNIGHT -> MidnightColorScheme
      AppThemeMode.LIGHT -> AlunanLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AlunanTypography,
    content = content,
  )
}
