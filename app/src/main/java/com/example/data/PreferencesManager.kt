package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alunan_prefs")

data class PlayerPreferences(
  val lastTrackId: String? = null,
  val repeatMode: Int = 2,
  val shuffleEnabled: Boolean = false,
  val playbackSpeed: Float = 1.0f,
  val themeMode: AppThemeMode = AppThemeMode.SONIC_NOIR,
  val appLanguage: String = "Sistem",
  val smoothFadeEnabled: Boolean = false,
  val gaplessEnabled: Boolean = true,
  val volumeNormalizationEnabled: Boolean = false,
  val autoPipEnabled: Boolean = true,
  val autoLoadLyricsEnabled: Boolean = true,
  val eqPresetName: String = "Nonaktif",
)

class PreferencesManager(private val context: Context) {
  private object Keys {
    val LAST_TRACK_ID = stringPreferencesKey("last_track_id")
    val REPEAT_MODE = intPreferencesKey("repeat_mode")
    val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
    val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val SMOOTH_FADE = booleanPreferencesKey("smooth_fade")
    val GAPLESS = booleanPreferencesKey("gapless")
    val VOLUME_NORM = booleanPreferencesKey("volume_norm")
    val AUTO_PIP = booleanPreferencesKey("auto_pip")
    val AUTO_LOAD_LYRICS = booleanPreferencesKey("auto_load_lyrics")
    val EQ_PRESET = stringPreferencesKey("eq_preset")
  }

  val preferencesFlow: Flow<PlayerPreferences> =
    context.dataStore.data.map { prefs ->
      val theme =
        prefs[Keys.THEME_MODE]?.let { name ->
          AppThemeMode.entries.firstOrNull { it.name == name }
        } ?: AppThemeMode.SONIC_NOIR
      PlayerPreferences(
        lastTrackId = prefs[Keys.LAST_TRACK_ID],
        repeatMode = prefs[Keys.REPEAT_MODE] ?: 2,
        shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
        playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
        themeMode = theme,
        appLanguage = prefs[Keys.APP_LANGUAGE] ?: "Sistem",
        smoothFadeEnabled = prefs[Keys.SMOOTH_FADE] ?: false,
        gaplessEnabled = prefs[Keys.GAPLESS] ?: true,
        volumeNormalizationEnabled = prefs[Keys.VOLUME_NORM] ?: false,
        autoPipEnabled = prefs[Keys.AUTO_PIP] ?: true,
        autoLoadLyricsEnabled = prefs[Keys.AUTO_LOAD_LYRICS] ?: true,
        eqPresetName = prefs[Keys.EQ_PRESET] ?: "Nonaktif",
      )
    }

  suspend fun saveLastTrackId(trackId: String) {
    context.dataStore.edit { it[Keys.LAST_TRACK_ID] = trackId }
  }

  suspend fun saveRepeatMode(mode: Int) {
    context.dataStore.edit { it[Keys.REPEAT_MODE] = mode }
  }

  suspend fun saveShuffleEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
  }

  suspend fun savePlaybackSpeed(speed: Float) {
    context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
  }

  suspend fun saveThemeMode(themeMode: AppThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
  }

  suspend fun saveAppLanguage(language: String) {
    context.dataStore.edit { it[Keys.APP_LANGUAGE] = language }
  }

  suspend fun saveSmoothFade(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SMOOTH_FADE] = enabled }
  }

  suspend fun saveGapless(enabled: Boolean) {
    context.dataStore.edit { it[Keys.GAPLESS] = enabled }
  }

  suspend fun saveVolumeNormalization(enabled: Boolean) {
    context.dataStore.edit { it[Keys.VOLUME_NORM] = enabled }
  }

  suspend fun saveAutoPip(enabled: Boolean) {
    context.dataStore.edit { it[Keys.AUTO_PIP] = enabled }
  }

  suspend fun saveAutoLoadLyrics(enabled: Boolean) {
    context.dataStore.edit { it[Keys.AUTO_LOAD_LYRICS] = enabled }
  }

  suspend fun saveEqPreset(preset: String) {
    context.dataStore.edit { it[Keys.EQ_PRESET] = preset }
  }
}
