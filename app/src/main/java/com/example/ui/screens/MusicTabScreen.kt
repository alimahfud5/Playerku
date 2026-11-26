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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.PlaylistWithTracks
import com.example.data.TrackEntity
import com.example.ui.PlayerUiState
import com.example.ui.SortOrder
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

private enum class MusicSubTab(val label: String) {
  BERANDA("Beranda"),
  FOLDER("Folder"),
  ARTIS("Artis"),
  ALBUM("Album"),
  LAGU("Lagu"),
  DAFTAR_PUTAR("Daftar putar"),
}

private data class DrillDownGroup(
  val title: String,
  val tracks: List<TrackEntity>,
  val playlistId: String? = null,
)

@Composable
fun MusicTabScreen(
  uiState: PlayerUiState,
  onShuffleAll: () -> Unit,
  onSelectSort: (SortOrder) -> Unit,
  onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
  onOpenTrackMenu: (TrackEntity) -> Unit,
  onImportAudio: () -> Unit,
  onImportM3u: () -> Unit,
  onCreatePlaylist: () -> Unit,
  onDeletePlaylist: (String) -> Unit,
) {
  var selectedSubTab by rememberSaveable { mutableStateOf(MusicSubTab.BERANDA) }
  var showSortMenu by remember { mutableStateOf(false) }
  var activeDrillDown by remember { mutableStateOf<DrillDownGroup?>(null) }

  if (activeDrillDown != null) {
    val group = activeDrillDown!!
    BackHandler { activeDrillDown = null }
    DrillDownTrackList(
      title = group.title,
      tracks = group.tracks,
      currentTrackId = uiState.currentTrack?.id,
      isPlaying = uiState.isPlaying,
      onBack = { activeDrillDown = null },
      onPlayTrack = { track -> onPlayTrack(track, group.tracks) },
      onOpenTrackMenu = onOpenTrackMenu,
      onDeletePlaylist = group.playlistId?.let { id ->
        {
          onDeletePlaylist(id)
          activeDrillDown = null
        }
      },
    )
    return
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Top Header: "Musik saya" + count + sort + "Acak semua"
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "Musik saya",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "${uiState.audioTracks.size} lagu",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
          IconButton(
            onClick = { showSortMenu = true },
            modifier = Modifier.size(48.dp).testTag("music_sort_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Sort,
              contentDescription = "Urutkan lagu",
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
          DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            SortOrder.entries.forEach { order ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = order.label,
                    color =
                      if (uiState.sortOrder == order) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
                  )
                },
                onClick = {
                  onSelectSort(order)
                  showSortMenu = false
                },
              )
            }
          }
        }

        Surface(
          onClick = onShuffleAll,
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
          modifier = Modifier.height(42.dp).testTag("music_shuffle_all_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Shuffle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Acak semua",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }
    }

    // Sub-tabs matching Screenshot 1, 3, 4, 6, 7, 8
    ScrollableTabRow(
      selectedTabIndex = selectedSubTab.ordinal,
      edgePadding = 16.dp,
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
      MusicSubTab.entries.forEach { tab ->
        val selected = selectedSubTab == tab
        Tab(
          selected = selected,
          onClick = { selectedSubTab = tab },
          text = {
            Text(
              text = tab.label,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
              color =
                if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          modifier = Modifier.testTag("music_subtab_${tab.name.lowercase()}"),
        )
      }
    }

    when (selectedSubTab) {
      MusicSubTab.BERANDA ->
        MusicHomeSubTab(
          uiState = uiState,
          onPlayTrack = onPlayTrack,
          onOpenTrackMenu = onOpenTrackMenu,
          onOpenSmartList = { title, list -> activeDrillDown = DrillDownGroup(title, list) },
        )

      MusicSubTab.FOLDER ->
        MusicFoldersSubTab(
          tracks = uiState.audioTracks,
          onOpenFolder = { folder, list -> activeDrillDown = DrillDownGroup(folder, list) },
          onImportAudio = onImportAudio,
        )

      MusicSubTab.ARTIS ->
        MusicArtistsSubTab(
          tracks = uiState.audioTracks,
          onOpenArtist = { artist, list -> activeDrillDown = DrillDownGroup(artist, list) },
        )

      MusicSubTab.ALBUM ->
        MusicAlbumsSubTab(
          tracks = uiState.audioTracks,
          onOpenAlbum = { album, list -> activeDrillDown = DrillDownGroup(album, list) },
        )

      MusicSubTab.LAGU ->
        MusicSongsSubTab(
          uiState = uiState,
          onPlayTrack = onPlayTrack,
          onOpenTrackMenu = onOpenTrackMenu,
          onImportAudio = onImportAudio,
        )

      MusicSubTab.DAFTAR_PUTAR ->
        MusicPlaylistsSubTab(
          playlists = uiState.audioPlaylists,
          onCreatePlaylist = onCreatePlaylist,
          onImportM3u = onImportM3u,
          onOpenPlaylist = { pl ->
            activeDrillDown = DrillDownGroup(pl.playlist.name, pl.tracks, pl.playlist.id)
          },
          onDeletePlaylist = onDeletePlaylist,
        )
    }
  }
}

@Composable
private fun MusicHomeSubTab(
  uiState: PlayerUiState,
  onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
  onOpenTrackMenu: (TrackEntity) -> Unit,
  onOpenSmartList: (String, List<TrackEntity>) -> Unit,
) {
  val tracks = uiState.audioTracks
  val likedTracks = remember(tracks) { tracks.filter { it.isFavorite } }
  val recentAdded = remember(tracks) { tracks.sortedByDescending { it.addedAt } }
  val continueTracks = remember(tracks) { tracks.filter { it.lastPositionMs > 0L || it.lastPlayedAt > 0L } }
  val mostPlayed =
    remember(tracks) { tracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount } }
  val recentlyPlayed =
    remember(tracks) {
      tracks.filter { it.lastPlayedAt > 0L }.sortedByDescending { it.lastPlayedAt }
        .ifEmpty { tracks.take(4) }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "smart_title") {
      Text(
        text = "Daftar putar cerdas",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    item(key = "smart_grid") {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          SmartPlaylistCard(
            title = "Suka",
            countText = "${likedTracks.size} lagu",
            icon = Icons.Rounded.Favorite,
            iconTint = HeartPink,
            iconBg = HeartPinkBg,
            onClick = { onOpenSmartList("Suka", likedTracks) },
            modifier = Modifier.weight(1f).testTag("smart_liked_card"),
          )
          SmartPlaylistCard(
            title = "Baru ditambahkan",
            countText = "${recentAdded.size} lagu",
            icon = Icons.Rounded.LibraryAdd,
            iconTint = MintGreen,
            iconBg = MintGreenBg,
            onClick = { onOpenSmartList("Baru ditambahkan", recentAdded) },
            modifier = Modifier.weight(1f).testTag("smart_recent_added_card"),
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          SmartPlaylistCard(
            title = "Lanjut mendengark...",
            countText = "${continueTracks.size} lagu",
            icon = Icons.Rounded.History,
            iconTint = HistoryIndigo,
            iconBg = HistoryIndigoBg,
            onClick = { onOpenSmartList("Lanjut mendengarkan", continueTracks) },
            modifier = Modifier.weight(1f).testTag("smart_continue_card"),
          )
          SmartPlaylistCard(
            title = "Lagu paling sering ...",
            countText = "${mostPlayed.size} lagu",
            icon = Icons.Rounded.TrendingUp,
            iconTint = TrendAmber,
            iconBg = TrendAmberBg,
            onClick = { onOpenSmartList("Lagu paling sering diputar", mostPlayed) },
            modifier = Modifier.weight(1f).testTag("smart_most_played_card"),
          )
        }
      }
    }

    item(key = "recent_header") {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Baru diputar",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    items(items = recentlyPlayed, key = { "recent_${it.id}" }) { track ->
      val isCurrent = uiState.currentTrack?.id == track.id
      val remaining =
        if (isCurrent && uiState.durationMs > 0L) {
          (uiState.durationMs - uiState.currentPositionMs).coerceAtLeast(0L)
        } else {
          null
        }
      MediaListItemRow(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isCurrent && uiState.isPlaying,
        remainingMs = remaining,
        onClick = { onPlayTrack(track, recentlyPlayed) },
        onMoreClick = { onOpenTrackMenu(track) },
      )
    }
  }
}

@Composable
private fun MusicFoldersSubTab(
  tracks: List<TrackEntity>,
  onOpenFolder: (String, List<TrackEntity>) -> Unit,
  onImportAudio: () -> Unit,
) {
  val grouped = remember(tracks) { tracks.groupBy { it.folderName }.toList().sortedBy { it.first } }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    items(items = grouped, key = { it.first }) { (folderName, itemsInFolder) ->
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenFolder(folderName, itemsInFolder) }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = folderName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${itemsInFolder.size} lagu",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = { onOpenFolder(folderName, itemsInFolder) }, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Buka folder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item(key = "add_folder_audio") {
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        onClick = onImportAudio,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Tambah file musik dari perangkat",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

@Composable
private fun MusicArtistsSubTab(
  tracks: List<TrackEntity>,
  onOpenArtist: (String, List<TrackEntity>) -> Unit,
) {
  val artists = remember(tracks) { tracks.groupBy { it.artist }.toList().sortedBy { it.first } }
  val avatarColors = remember {
    listOf(
      Color(0xFFC850C0),
      Color(0xFF7B61FF),
      Color(0xFF5E5CE6),
      Color(0xFF38BDF8),
      Color(0xFF2DD4BF),
      Color(0xFFF59E0B),
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    items(items = artists, key = { it.first }) { (artistName, artistTracks) ->
      val color = avatarColors[kotlin.math.abs(artistName.hashCode()) % avatarColors.size]
      val initial = artistName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenArtist(artistName, artistTracks) }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(48.dp).clip(CircleShape).background(color),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = artistName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${artistTracks.size} lagu",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = { onOpenArtist(artistName, artistTracks) }, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Buka artis",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun MusicAlbumsSubTab(
  tracks: List<TrackEntity>,
  onOpenAlbum: (String, List<TrackEntity>) -> Unit,
) {
  val albums = remember(tracks) { tracks.groupBy { it.album }.toList().sortedBy { it.first } }

  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 156.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(items = albums, key = { it.first }) { (albumName, albumTracks) ->
      val firstTrack = albumTracks.firstOrNull()
      Surface(
        onClick = { onOpenAlbum(albumName, albumTracks) },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Album,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(42.dp),
            )
            firstTrack?.artworkFileUri?.let { artUri ->
              AsyncImage(
                model = artUri,
                contentDescription = albumName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = albumName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${firstTrack?.artist ?: "Artis"}\n${albumTracks.size} lagu",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Icon(
              imageVector = Icons.Rounded.MoreVert,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MusicSongsSubTab(
  uiState: PlayerUiState,
  onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
  onOpenTrackMenu: (TrackEntity) -> Unit,
  onImportAudio: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    item(key = "add_local_music_banner") {
      Surface(
        onClick = onImportAudio,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).testTag("songs_import_button"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Tambah musik lokal (MP3, WAV, M4A, FLAC, OGG)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    }

    items(items = uiState.audioTracks, key = { it.id }) { track ->
      val isCurrent = uiState.currentTrack?.id == track.id
      MediaListItemRow(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isCurrent && uiState.isPlaying,
        onClick = { onPlayTrack(track, uiState.audioTracks) },
        onMoreClick = { onOpenTrackMenu(track) },
      )
    }
  }
}

@Composable
private fun MusicPlaylistsSubTab(
  playlists: List<PlaylistWithTracks>,
  onCreatePlaylist: () -> Unit,
  onImportM3u: () -> Unit,
  onOpenPlaylist: (PlaylistWithTracks) -> Unit,
  onDeletePlaylist: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
    // Action row matching Screenshot 1
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Surface(
        onClick = onCreatePlaylist,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.weight(1.35f).height(48.dp).testTag("new_playlist_button"),
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
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Daftar putar baru",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
          )
        }
      }

      Surface(
        onClick = onImportM3u,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.weight(1f).height(48.dp).testTag("import_m3u_button"),
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Impor M3U",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    if (playlists.isEmpty()) {
      Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier.size(84.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
          )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = "Tidak ada daftar putar",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Buat daftar putar sendiri dengan lagu favorit Anda.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
          onClick = onCreatePlaylist,
          shape = RoundedCornerShape(50),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
          modifier = Modifier.testTag("create_first_playlist_button"),
        ) {
          Text(
            text = "Buat daftar putar pertama",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    } else {
      Spacer(modifier = Modifier.height(14.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items = playlists, key = { it.playlist.id }) { pl ->
          Surface(
            onClick = { onOpenPlaylist(pl) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier.size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(pl.playlist.name, style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "${pl.tracks.size} lagu",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(onClick = { onDeletePlaylist(pl.playlist.id) }, modifier = Modifier.size(48.dp)) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = "Hapus daftar putar",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DrillDownTrackList(
  title: String,
  tracks: List<TrackEntity>,
  currentTrackId: String?,
  isPlaying: Boolean,
  onBack: () -> Unit,
  onPlayTrack: (TrackEntity) -> Unit,
  onOpenTrackMenu: (TrackEntity) -> Unit,
  onDeletePlaylist: (() -> Unit)? = null,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${tracks.size} item",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (tracks.isNotEmpty()) {
        IconButton(onClick = { onPlayTrack(tracks.first()) }, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Putar semua",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
      if (onDeletePlaylist != null) {
        IconButton(onClick = onDeletePlaylist, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Hapus daftar putar",
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    if (tracks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = "Belum ada lagu dalam kategori ini.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(items = tracks, key = { it.id }) { track ->
          val isCurrent = currentTrackId == track.id
          MediaListItemRow(
            track = track,
            isCurrent = isCurrent,
            isPlaying = isCurrent && isPlaying,
            onClick = { onPlayTrack(track) },
            onMoreClick = { onOpenTrackMenu(track) },
          )
        }
      }
    }
  }
}
