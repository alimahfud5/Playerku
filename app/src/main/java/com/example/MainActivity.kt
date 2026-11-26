package com.example

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PlayerViewModel
import com.example.ui.screens.AlunanMainScreen
import com.example.ui.theme.AlunanTheme
import com.example.ui.theme.AppThemeMode

class MainActivity : ComponentActivity() {
  private val playerViewModel: PlayerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent {
      val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
      val isLight = uiState.preferences.themeMode == AppThemeMode.LIGHT
      LaunchedEffect(isLight) {
        enableEdgeToEdge(
          statusBarStyle =
            if (isLight) {
              SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
              )
            } else {
              SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            },
          navigationBarStyle =
            if (isLight) {
              SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
              )
            } else {
              SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            },
        )
      }

      AlunanTheme(themeMode = uiState.preferences.themeMode) {
        AlunanMainScreen(
          uiState = uiState,
          viewModel = playerViewModel,
          onRequestPip = { tryEnterPip() },
        )
      }
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    val state = playerViewModel.uiState.value
    if (state.activeVideoTrack != null && state.preferences.autoPipEnabled) {
      tryEnterPip()
    }
  }

  private fun tryEnterPip() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
      enterPictureInPictureMode(params)
    }
  }
}
