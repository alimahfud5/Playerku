package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.TrackEntity
import com.example.ui.PlayerUiState
import com.example.ui.components.MediaListItemRow
import com.example.ui.components.SmartPlaylistCard
import com.example.ui.theme.HeartPink
import com.example.ui.theme.HeartPinkBg
import com.example.ui.theme.HistoryIndigo
import com.example.ui.theme.HistoryIndigoBg
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenBg
import com.example.ui.theme.TrendAmber
import com.example.ui.theme.TrendAmberBg

private enum class VideoSubTab(val label: String) {
  BERANDA("Beranda"),
  FOLDER("Folder"),
  SEMUA("Semua"),
  DAFTAR_PUTAR("Daftar putar"),
}

@Composable
fun VideoTabScreen(
  uiState: PlayerUiState,
  onPickVideos: () -> Unit,
  onPlayVideo: (TrackEntity) -> Unit,
  onOpenTrackMenu: (TrackEntity) -> Unit,
  onCreatePlaylist: () -> Unit,
  onImportM3u: () -> Unit,
) {
  var selectedSubTab by rememberSaveable { mutableStateOf(VideoSubTab.BERANDA) }
  var drillTitle by remember { mutableStateOf<String?>(null) }
  var drillVideos by remember { mutableStateOf<List<TrackEntity>>(emptyList()) }

  if (drillTitle != null) {
    BackHandler { drillTitle = null }
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = { drillTitle = null }, modifier = Modifier.size(48.dp)) {
          Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
        }
        Column {
          Text(drillTitle!!, style = MaterialTheme.typography.titleLarge)
          Text(
            "${drillVideos.size} video",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (drillVideos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            "Belum ada video dalam kategori ini.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(drillVideos, key = { it.id }) { video ->
            MediaListItemRow(
              track = video,
              isCurrent = false,
              isPlaying = false,
              onClick = { onPlayVideo(video) },
              onMoreClick = { onOpenTrackMenu(video) },
            )
          }
        }
      }
    }
    return
  }

  val videos = uiState.videoTracks
  val likedVideos = remember(videos) { videos.filter { it.isFavorite } }
  val continuedVideos = remember(videos) { videos.filter { it.lastPositionMs > 0L } }
  val mostWatched =
    remember(videos) { videos.filter { it.playCount > 0 }.sortedByDescending { it.playCount } }

  Column(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
      Text(
        text = "Video saya",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "${videos.size} video",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // Zero-permission Photo Picker Video Access Card (matching Screenshot 2 & 5)
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
          Box(
            modifier =
              Modifier.size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.VideoLibrary,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Text(
            text =
              "Pilih video dari galeri atau penyimpanan perangkat Anda melalui Pemilih Media untuk ditonton di dalam aplikasi.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(
            onClick = onPickVideos,
            shape = RoundedCornerShape(50),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.testTag("pick_videos_button"),
          ) {
            Text("Pilih video", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    TabRow(
      selectedTabIndex = selectedSubTab.ordinal,
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.primary,
      divider = {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(1.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        )
      },
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab.ordinal]),
          height = 2.5.dp,
          color = MaterialTheme.colorScheme.primary,
        )
      },
    ) {
      VideoSubTab.entries.forEach { tab ->
        val selected = selectedSubTab == tab
        Tab(
          selected = selected,
          onClick = { selectedSubTab = tab },
          text = {
            Text(
              text = tab.label,
              style = MaterialTheme.typography.titleSmall,
              color =
                if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
        )
      }
    }

    when (selectedSubTab) {
      VideoSubTab.BERANDA ->
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Text("Daftar putar cerdas", style = MaterialTheme.typography.titleLarge)
          }
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartPlaylistCard(
                  title = "Suka",
                  countText = "${likedVideos.size} video",
                  icon = Icons.Rounded.Favorite,
                  iconTint = HeartPink,
                  iconBg = HeartPinkBg,
                  onClick = {
                    drillTitle = "Video Disukai"
                    drillVideos = likedVideos
                  },
                  modifier = Modifier.weight(1f),
                )
                SmartPlaylistCard(
                  title = "Baru ditambahkan",
                  countText = "${videos.size} video",
                  icon = Icons.Rounded.LibraryAdd,
                  iconTint = MintGreen,
                  iconBg = MintGreenBg,
                  onClick = {
                    drillTitle = "Video Baru Ditambahkan"
                    drillVideos = videos
                  },
                  modifier = Modifier.weight(1f),
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartPlaylistCard(
                  title = "Lanjut menonton",
                  countText = "${continuedVideos.size} video",
                  icon = Icons.Rounded.History,
                  iconTint = HistoryIndigo,
                  iconBg = HistoryIndigoBg,
                  onClick = {
                    drillTitle = "Lanjut Menonton"
                    drillVideos = continuedVideos
                  },
                  modifier = Modifier.weight(1f),
                )
                SmartPlaylistCard(
                  title = "Video paling sering...",
                  countText = "${mostWatched.size} video",
                  icon = Icons.Rounded.TrendingUp,
                  iconTint = TrendAmber,
                  iconBg = TrendAmberBg,
                  onClick = {
                    drillTitle = "Video Paling Sering Ditonton"
                    drillVideos = mostWatched
                  },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
          item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Baru diputar", style = MaterialTheme.typography.titleLarge)
          }
          if (videos.isEmpty()) {
            item {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Text("Tidak ada item terbaru", style = MaterialTheme.typography.titleMedium)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    "Putar video untuk menyimpan posisi dan riwayat tontonan Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(videos, key = { it.id }) { video ->
              MediaListItemRow(
                track = video,
                isCurrent = false,
                isPlaying = false,
                onClick = { onPlayVideo(video) },
                onMoreClick = { onOpenTrackMenu(video) },
              )
            }
          }
        }

      VideoSubTab.FOLDER -> {
        val grouped = remember(videos) { videos.groupBy { it.folderName }.toList() }
        if (grouped.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              "Belum ada folder video.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(grouped, key = { it.first }) { (folder, list) ->
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                      drillTitle = folder
                      drillVideos = list
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Folder,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(folder, style = MaterialTheme.typography.titleMedium)
                  Text(
                    "${list.size} video",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      VideoSubTab.SEMUA ->
        if (videos.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              "Ketuk \"Pilih video\" di atas untuk menambahkan video.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(videos, key = { it.id }) { video ->
              MediaListItemRow(
                track = video,
                isCurrent = false,
                isPlaying = false,
                onClick = { onPlayVideo(video) },
                onMoreClick = { onOpenTrackMenu(video) },
              )
            }
          }
        }

      VideoSubTab.DAFTAR_PUTAR ->
        Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
              onClick = onCreatePlaylist,
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
              modifier = Modifier.weight(1.35f).height(48.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "Daftar putar baru",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
            }
            Surface(
              onClick = onImportM3u,
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(imageVector = Icons.Rounded.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Impor M3U", style = MaterialTheme.typography.labelLarge)
              }
            }
          }

          if (uiState.videoPlaylists.isEmpty()) {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier =
                  Modifier.size(78.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(34.dp),
                )
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text("Tidak ada daftar putar", style = MaterialTheme.typography.headlineSmall)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                "Buat daftar putar sendiri dengan video favorit Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          } else {
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(uiState.videoPlaylists, key = { it.playlist.id }) { pl ->
                Surface(
                  onClick = {
                    drillTitle = pl.playlist.name
                    drillVideos = pl.tracks
                  },
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceContainer,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text(pl.playlist.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                      "${pl.tracks.size} video",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
    }
  }
}
