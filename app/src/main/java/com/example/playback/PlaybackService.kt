package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

  private var mediaSession: MediaSession? = null
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var sleepTimerJob: Job? = null
  private var sleepTimerEndElapsedMs: Long = 0L
  private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

  override fun onCreate() {
    super.onCreate()

    val audioAttributes =
      AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    val player =
      ExoPlayer.Builder(this)
        .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    currentAudioSessionId = player.audioSessionId

    player.addListener(
      object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
          currentAudioSessionId = audioSessionId
          publishSessionExtras()
        }
      }
    )

    val launchIntent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val sessionActivityPendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        launchIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    val notificationProvider =
      DefaultMediaNotificationProvider.Builder(this)
        .setChannelName(R.string.playback_channel_name)
        .build()
    setMediaNotificationProvider(notificationProvider)

    mediaSession =
      MediaSession.Builder(this, player)
        .setSessionActivity(sessionActivityPendingIntent)
        .setCallback(AlunanSessionCallback())
        .build()

    publishSessionExtras()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (
      player == null ||
        !player.playWhenReady ||
        player.mediaItemCount == 0 ||
        player.playbackState == Player.STATE_ENDED
    ) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    sleepTimerJob?.cancel()
    serviceScope.cancel()
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }

  private fun setSleepTimer(minutes: Int) {
    sleepTimerJob?.cancel()
    if (minutes <= 0) {
      sleepTimerEndElapsedMs = 0L
      publishSessionExtras()
      return
    }
    val durationMs = minutes * 60_000L
    sleepTimerEndElapsedMs = SystemClock.elapsedRealtime() + durationMs
    publishSessionExtras()

    sleepTimerJob =
      serviceScope.launch {
        delay(durationMs)
        mediaSession?.player?.pause()
        sleepTimerEndElapsedMs = 0L
        publishSessionExtras()
      }
  }

  private fun publishSessionExtras() {
    val extras =
      Bundle().apply {
        putLong(EXTRA_SLEEP_TIMER_END_MS, sleepTimerEndElapsedMs)
        putInt(EXTRA_AUDIO_SESSION_ID, currentAudioSessionId)
      }
    mediaSession?.setSessionExtras(extras)
  }

  private inner class AlunanSessionCallback : MediaSession.Callback {
    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
      val sessionCommands =
        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
          .add(SessionCommand(COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY))
          .add(SessionCommand(COMMAND_SET_VOLUME_SCALE, Bundle.EMPTY))
          .build()
      return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
        .setAvailableSessionCommands(sessionCommands)
        .build()
    }

    override fun onCustomCommand(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
      customCommand: SessionCommand,
      args: Bundle,
    ): ListenableFuture<SessionResult> {
      return when (customCommand.customAction) {
        COMMAND_SET_SLEEP_TIMER -> {
          val minutes = args.getInt(ARG_SLEEP_MINUTES, 0)
          setSleepTimer(minutes)
          val resultBundle =
            Bundle().apply {
              putLong(EXTRA_SLEEP_TIMER_END_MS, sleepTimerEndElapsedMs)
              putInt(EXTRA_AUDIO_SESSION_ID, currentAudioSessionId)
            }
          ImmediateListenableFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
        }
        COMMAND_SET_VOLUME_SCALE -> {
          val vol = args.getFloat(ARG_VOLUME_SCALE, 1.0f).coerceIn(0.2f, 1.0f)
          session.player.volume = vol
          ImmediateListenableFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        else -> ImmediateListenableFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
      }
    }
  }

  private class ImmediateListenableFuture<T>(private val value: T) : ListenableFuture<T> {
    override fun addListener(listener: Runnable, executor: Executor) {
      executor.execute(listener)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

    override fun isCancelled(): Boolean = false

    override fun isDone(): Boolean = true

    override fun get(): T = value

    override fun get(timeout: Long, unit: TimeUnit): T = value
  }

  companion object {
    const val COMMAND_SET_SLEEP_TIMER = "com.example.alunan.SET_SLEEP_TIMER"
    const val COMMAND_SET_VOLUME_SCALE = "com.example.alunan.SET_VOLUME_SCALE"
    const val ARG_SLEEP_MINUTES = "minutes"
    const val ARG_VOLUME_SCALE = "volume_scale"
    const val EXTRA_SLEEP_TIMER_END_MS = "sleep_timer_end_ms"
    const val EXTRA_AUDIO_SESSION_ID = "audio_session_id"
  }
}
