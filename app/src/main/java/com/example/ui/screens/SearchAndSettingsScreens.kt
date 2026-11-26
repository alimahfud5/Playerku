package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MediaType
import com.example.data.TrackEntity
import com.example.ui.PlayerUiState
import com.example.ui.components.MediaListItemRow
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.TrendAmber
import com.example.ui.theme.TrendAmberBg

private enum class SearchFilter(val label: String) {
  ALL("Semua"),
  MUSIC("Musik"),
  VIDEO("Video"),
}

@Composable
fun SearchTabScreen(
  uiState: PlayerUiState,
  onQueryChange: (String) -> Unit,
  onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
  onOpenTrackMenu: (TrackEntity) -> Unit,
) {
  var filter by remember { mutableStateOf(SearchFilter.ALL) }
  val query = uiState.searchQuery

  val combined =
    remember(uiState.audioTracks, uiState.videoTracks, filter, query) {
      val base =
        when (filter) {
          SearchFilter.ALL -> uiState.audioTracks + uiState.videoTracks
          SearchFilter.MUSIC -> uiState.audioTracks
          SearchFilter.VIDEO -> uiState.videoTracks
        }
      if (query.isBlank()) base
      else
        base.filter {
          it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = "Pencarian",
      style = MaterialTheme.typography.displaySmall,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      placeholder = { Text("Cari lagu, artis, album, atau video...") },
      leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = { onQueryChange("") }) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Hapus")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
          focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
      modifier = Modifier.fillMaxWidth().testTag("search_tab_input"),
    )

    Spacer(modifier = Modifier.height(10.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(SearchFilter.entries) { f ->
        FilterChip(
          selected = filter == f,
          onClick = { filter = f },
          label = { Text(f.label) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.secondary,
            ),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (combined.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = "Tidak ada hasil untuk \"$query\"",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(items = combined, key = { it.id }) { track ->
          val isCurrent =
            track.mediaType == MediaType.AUDIO && uiState.currentTrack?.id == track.id
          MediaListItemRow(
            track = track,
            isCurrent = isCurrent,
            isPlaying = isCurrent && uiState.isPlaying,
            onClick = { onPlayTrack(track, combined.filter { it.mediaType == MediaType.AUDIO }) },
            onMoreClick = { onOpenTrackMenu(track) },
          )
        }
      }
    }
  }
}

@Composable
fun SettingsTabScreen(
  uiState: PlayerUiState,
  onSelectLanguage: (String) -> Unit,
  onSelectTheme: (AppThemeMode) -> Unit,
  onOpenEqualizer: () -> Unit,
  onOpenSpeedDialog: () -> Unit,
  onToggleSmoothFade: (Boolean) -> Unit,
  onToggleGapless: (Boolean) -> Unit,
  onToggleVolumeNorm: (Boolean) -> Unit,
  onToggleAutoLoadLyrics: (Boolean) -> Unit,
  onOpenSleepTimer: () -> Unit,
  onToggleAutoPip: (Boolean) -> Unit,
  onImportAudio: () -> Unit,
  onBackupData: () -> Unit,
  onRestoreData: () -> Unit,
  onClearCache: () -> Unit,
) {
  val context = LocalContext.current
  val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
  val ignoringBatteryOpt =
    remember(uiState.currentTrack) {
      powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

  var showThemeDialog by remember { mutableStateOf(false) }
  var showLanguageDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = "Pengaturan",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    item {
      SettingsSection(title = "Bahasa aplikasi") {
        SettingsActionRow(
          icon = Icons.Rounded.Language,
          iconTint = MaterialTheme.colorScheme.secondary,
          iconBg = MaterialTheme.colorScheme.primaryContainer,
          title = "Ubah bahasa",
          subtitle = uiState.preferences.appLanguage,
          onClick = { showLanguageDialog = true },
        )
      }
    }

    item {
      SettingsSection(title = "Tampilan & Tema") {
        SettingsActionRow(
          icon = Icons.Rounded.Palette,
          iconTint = MaterialTheme.colorScheme.secondary,
          iconBg = MaterialTheme.colorScheme.primaryContainer,
          title = "Tema aplikasi",
          subtitle = uiState.preferences.themeMode.displayName,
          onClick = { showThemeDialog = true },
          testTag = "settings_theme_row",
        )
      }
    }

    item {
      SettingsSection(title = "Suara dan equalizer") {
        SettingsActionRow(
          icon = Icons.Rounded.Equalizer,
          iconTint = MaterialTheme.colorScheme.secondary,
          iconBg = MaterialTheme.colorScheme.primaryContainer,
          title = "Equalizer perangkat keras",
          subtitle = "Preset saat ini: ${uiState.preferences.eqPresetName}",
          onClick = onOpenEqualizer,
          testTag = "settings_eq_row",
        )
        SectionDivider()
        SettingsActionRow(
          icon = Icons.Rounded.Speed,
          iconTint = MaterialTheme.colorScheme.secondary,
          iconBg = MaterialTheme.colorScheme.primaryContainer,
          title = "Kecepatan putar",
          subtitle = "${uiState.playbackSpeed}x",
          onClick = onOpenSpeedDialog,
        )
        SectionDivider()
        SettingsSwitchRow(
          title = "Fade putar/jeda halus",
          subtitle = "Naikkan dan turunkan volume secara halus saat memutar dan menjeda",
          checked = uiState.preferences.smoothFadeEnabled,
          onCheckedChange = onToggleSmoothFade,
        )
        SectionDivider()
        SettingsSwitchRow(
          title = "Pemutaran tanpa jeda",
          subtitle = "Berpindah antar-lagu dengan mulus tanpa jeda",
          checked = uiState.preferences.gaplessEnabled,
          onCheckedChange = onToggleGapless,
        )
        SectionDivider()
        SettingsSwitchRow(
          title = "Normalisasi volume",
          subtitle = "Seimbangkan kenyaringan saat memutar musik latar",
          checked = uiState.preferences.volumeNormalizationEnabled,
          onCheckedChange = onToggleVolumeNorm,
        )
      }
    }

    item {
      SettingsSection(title = "Pengatur waktu tidur") {
        SettingsActionRow(
          icon = Icons.Rounded.Bedtime,
          iconTint = TrendAmber,
          iconBg = TrendAmberBg,
          title = "Pengatur waktu tidur",
          subtitle =
            if (uiState.sleepTimerRemainingMs > 0L)
              "Aktif: ${formatDurationMs(uiState.sleepTimerRemainingMs)}"
            else "Belum diatur",
          onClick = onOpenSleepTimer,
          testTag = "settings_sleep_timer_row",
        )
      }
    }

    item {
      SettingsSection(title = "Pemutaran latar belakang") {
        SettingsActionRow(
          icon = Icons.Rounded.BatteryChargingFull,
          iconTint = TrendAmber,
          iconBg = TrendAmberBg,
          title = "Pemutaran latar tanpa batas",
          subtitle =
            if (ignoringBatteryOpt) "Diizinkan — baterai tidak dibatasi"
            else "Dibatasi — ketuk untuk membuka pengaturan baterai",
          onClick = {
            runCatching {
              context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
              )
            }
          },
        )
      }
    }

    item {
      SettingsSection(title = "Pemutaran video") {
        SettingsSwitchRow(
          title = "Picture-in-picture otomatis",
          subtitle = "Saat Anda kembali ke layar utama selama video, lanjutkan di jendela kecil",
          checked = uiState.preferences.autoPipEnabled,
          onCheckedChange = onToggleAutoPip,
          leadingIcon = Icons.Rounded.PictureInPictureAlt,
        )
      }
    }

    item {
      SettingsSection(title = stringResource(R.string.subtitles_and_lyrics_section)) {
        SettingsSwitchRow(
          title = stringResource(R.string.auto_load_lyrics_title),
          subtitle = stringResource(R.string.auto_load_lyrics_desc),
          checked = uiState.preferences.autoLoadLyricsEnabled,
          onCheckedChange = onToggleAutoLoadLyrics,
          leadingIcon = Icons.Rounded.Lyrics,
        )
      }
    }

    item {
      SettingsSection(title = "Penyimpanan dan integrasi") {
        SettingsActionRow(
          icon = Icons.Rounded.LibraryMusic,
          iconTint = MaterialTheme.colorScheme.secondary,
          iconBg = MaterialTheme.colorScheme.primaryContainer,
          title = "Tambah musik lokal",
          subtitle = "Pilih file MP3, WAV, M4A, FLAC, atau OGG",
          onClick = onImportAudio,
        )
        SectionDivider()
        SettingsActionRow(
          icon = Icons.Rounded.Upload,
          iconTint = MaterialTheme.colorScheme.secondary,
          iconBg = MaterialTheme.colorScheme.primaryContainer,
          title = "Cadangkan data aplikasi",
          subtitle = "Ekspor daftar putar dan lagu suka ke JSON",
          onClick = onBackupData,
        )
        SectionDivider()
        SettingsActionRow(
          icon = Icons.Rounded.Download,
          iconTint = MaterialTheme.colorScheme.secondary,
          iconBg = MaterialTheme.colorScheme.primaryContainer,
          title = "Pulihkan data aplikasi",
          subtitle = "Impor daftar putar dan lagu suka dari cadangan",
          onClick = onRestoreData,
        )
        SectionDivider()
        SettingsActionRow(
          icon = Icons.Rounded.CleaningServices,
          iconTint = MaterialTheme.colorScheme.error,
          iconBg = Color(0xFF2E1924),
          title = "Bersihkan cache sementara",
          subtitle = "Bersihkan file sementara sampul yang tidak terpakai",
          onClick = onClearCache,
        )
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }

  if (showThemeDialog) {
    AlertDialog(
      onDismissRequest = { showThemeDialog = false },
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      title = { Text("Tema aplikasi") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          AppThemeMode.entries.forEach { mode ->
            val selected = uiState.preferences.themeMode == mode
            Surface(
              onClick = {
                onSelectTheme(mode)
                showThemeDialog = false
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
                Text(mode.displayName, style = MaterialTheme.typography.bodyLarge)
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
      confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Tutup") } },
    )
  }

  if (showLanguageDialog) {
    val langs = listOf("Bawaan sistem", "Bahasa Indonesia", "English")
    AlertDialog(
      onDismissRequest = { showLanguageDialog = false },
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      title = { Text("Ubah bahasa") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          langs.forEach { lang ->
            val selected = uiState.preferences.appLanguage == lang
            Surface(
              onClick = {
                onSelectLanguage(lang)
                showLanguageDialog = false
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
                Text(lang, style = MaterialTheme.typography.bodyLarge)
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
      confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Tutup") } },
    )
  }
}

@Composable
private fun SettingsSection(
  title: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
  }
}

@Composable
private fun SettingsActionRow(
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String? = null,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .let { if (testTag != null) it.testTag(testTag) else it }
        .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
      contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(
      imageVector = Icons.Rounded.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  leadingIcon: ImageVector? = null,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable { onCheckedChange(!checked) }
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (leadingIcon != null) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun SectionDivider() {
  HorizontalDivider(
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    modifier = Modifier.padding(horizontal = 16.dp),
  )
}
