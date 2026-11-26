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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alunan_prefs")

data class PlayerPreferences(
  val lastTrackId: String? = null,
  val repeatMode: Int = 2, // Player.REPEAT_MODE_ALL
  val shuffleEnabled: Boolean = false,
  val playbackSpeed: Float = 1.0f,
)

class PreferencesManager(private val context: Context) {
  private object Keys {
    val LAST_TRACK_ID = stringPreferencesKey("last_track_id")
    val REPEAT_MODE = intPreferencesKey("repeat_mode")
    val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
    val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
  }

  val preferencesFlow: Flow<PlayerPreferences> =
    context.dataStore.data.map { prefs ->
      PlayerPreferences(
        lastTrackId = prefs[Keys.LAST_TRACK_ID],
        repeatMode = prefs[Keys.REPEAT_MODE] ?: 2,
        shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
        playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
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
}
