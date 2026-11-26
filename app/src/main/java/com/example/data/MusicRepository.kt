package com.example.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MusicRepository(
  private val context: Context,
  private val trackDao: TrackDao,
  private val playlistDao: PlaylistDao,
) {
  val audioTracks: Flow<List<TrackEntity>> = trackDao.observeTracksByType(MediaType.AUDIO)
  val videoTracks: Flow<List<TrackEntity>> = trackDao.observeTracksByType(MediaType.VIDEO)
  val allTracks: Flow<List<TrackEntity>> = trackDao.observeAllTracks()
  val audioPlaylists: Flow<List<PlaylistWithTracks>> =
    playlistDao.observePlaylistsByType(MediaType.AUDIO)
  val videoPlaylists: Flow<List<PlaylistWithTracks>> =
    playlistDao.observePlaylistsByType(MediaType.VIDEO)

  suspend fun ensureBundledTracksSeeded() =
    withContext(Dispatchers.IO) {
      if (trackDao.getTrackCount() == 0) {
        val pkg = context.packageName
        val scheme = ContentResolver.SCHEME_ANDROID_RESOURCE
        val bundled =
          listOf(
            TrackEntity(
              id = "bundled_senja",
              title = "Senja di Pantai",
              artist = "Nusantara Lo-Fi",
              album = "Sore Hari",
              folderName = "Bawaan Alunan",
              mediaType = MediaType.AUDIO,
              uriString = "$scheme://$pkg/${R.raw.senja_di_pantai}",
              durationMs = 32_000L,
              artworkResName = "img_art_senja",
              artworkFileUri = "$scheme://$pkg/${R.drawable.img_art_senja}",
              isFavorite = true,
              isBundled = true,
              playCount = 2,
              lastPlayedAt = System.currentTimeMillis() - 300_000L,
              sortOrder = 0,
            ),
            TrackEntity(
              id = "bundled_hujan",
              title = "Hujan Malam",
              artist = "Kopi & Rintik",
              album = "Teduh",
              folderName = "Bawaan Alunan",
              mediaType = MediaType.AUDIO,
              uriString = "$scheme://$pkg/${R.raw.hujan_malam}",
              durationMs = 32_000L,
              artworkResName = "img_art_hujan",
              artworkFileUri = "$scheme://$pkg/${R.drawable.img_art_hujan}",
              isFavorite = false,
              isBundled = true,
              playCount = 1,
              lastPlayedAt = System.currentTimeMillis() - 900_000L,
              sortOrder = 1,
            ),
            TrackEntity(
              id = "bundled_gema",
              title = "Gema Analog",
              artist = "Sintesa Ruang",
              album = "Frekuensi",
              folderName = "Bawaan Alunan",
              mediaType = MediaType.AUDIO,
              uriString = "$scheme://$pkg/${R.raw.gema_analog}",
              durationMs = 32_000L,
              artworkResName = "img_art_malam",
              artworkFileUri = "$scheme://$pkg/${R.drawable.img_art_malam}",
              isFavorite = false,
              isBundled = true,
              playCount = 0,
              sortOrder = 2,
            ),
            TrackEntity(
              id = "bundled_embun",
              title = "Embun Pagi",
              artist = "Akustik Rimba",
              album = "Fajar",
              folderName = "Bawaan Alunan",
              mediaType = MediaType.AUDIO,
              uriString = "$scheme://$pkg/${R.raw.embun_pagi}",
              durationMs = 28_000L,
              artworkResName = "img_art_pagi",
              artworkFileUri = "$scheme://$pkg/${R.drawable.img_art_pagi}",
              isFavorite = false,
              isBundled = true,
              playCount = 0,
              sortOrder = 3,
            ),
          )
        trackDao.insertTracks(bundled)
      }
    }

  suspend fun importMediaUris(
    uris: List<Uri>,
    mediaType: String,
    autoLoadLyrics: Boolean = true,
  ): Int =
    withContext(Dispatchers.IO) {
      var importedCount = 0
      val artworkDir = File(context.filesDir, "artworks").apply { mkdirs() }
      for (uri in uris) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        } catch (_: SecurityException) {}

        val trackId = "${mediaType.lowercase()}_${UUID.nameUUIDFromBytes(uri.toString().toByteArray())}"
        val fallbackName = resolveDisplayName(uri)
        val folder = resolveFolderName(uri, mediaType)
        var title = fallbackName
        var artist = if (mediaType == MediaType.VIDEO) "Video Perangkat" else "Artis tidak dikenal"
        var album = if (mediaType == MediaType.VIDEO) "Video Lokal" else "Koleksi Perangkat"
        var durationMs = 0L
        var artUri: String? = null
        var extractedLyrics: String? = null

        val retriever = MediaMetadataRetriever()
        try {
          retriever.setDataSource(context, uri)
          retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?.let { title = it.trim() }
          retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() }
            ?.let { artist = it.trim() }
          retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            ?.takeIf { it.isNotBlank() }
            ?.let { album = it.trim() }
          retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.let { durationMs = it }

          if (mediaType == MediaType.AUDIO) {
            retriever.embeddedPicture?.let { bytes ->
              val artFile = File(artworkDir, "$trackId.jpg")
              artFile.writeBytes(bytes)
              artUri = Uri.fromFile(artFile).toString()
            }
            if (autoLoadLyrics) {
              extractedLyrics = LrcParser.extractId3Lyrics(context, uri)
            }
          } else {
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame != null) {
              val thumbFile = File(artworkDir, "$trackId.jpg")
              FileOutputStream(thumbFile).use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 82, out)
              }
              frame.recycle()
              artUri = Uri.fromFile(thumbFile).toString()
            }
          }
        } catch (_: Exception) {
        } finally {
          try {
            retriever.release()
          } catch (_: Exception) {}
        }

        val entity =
          TrackEntity(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            folderName = folder,
            mediaType = mediaType,
            uriString = uri.toString(),
            durationMs = durationMs,
            artworkResName = null,
            artworkFileUri = artUri,
            lyricsText = extractedLyrics,
            isFavorite = false,
            isBundled = false,
            sortOrder = 100,
          )
        trackDao.insertTrack(entity)
        importedCount++
      }
      importedCount
    }

  suspend fun createPlaylist(name: String, mediaType: String): String =
    withContext(Dispatchers.IO) {
      val id = "pl_${UUID.randomUUID()}"
      playlistDao.insertPlaylist(
        PlaylistEntity(id = id, name = name.trim(), mediaType = mediaType)
      )
      id
    }

  suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
    withContext(Dispatchers.IO) {
      playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, trackId))
    }
  }

  suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
    withContext(Dispatchers.IO) { playlistDao.removeTrackFromPlaylist(playlistId, trackId) }
  }

  suspend fun deletePlaylist(playlistId: String) {
    withContext(Dispatchers.IO) {
      playlistDao.clearPlaylistTracks(playlistId)
      playlistDao.deletePlaylist(playlistId)
    }
  }

  suspend fun importM3uPlaylist(uri: Uri, mediaType: String): Pair<String, Int> =
    withContext(Dispatchers.IO) {
      val playlistName = resolveDisplayName(uri).ifBlank { "Daftar Putar M3U" }
      val lines =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readLines() }
          ?: emptyList()

      val allExisting = trackDao.getAllTracksOnce().filter { it.mediaType == mediaType }
      val playlistId = createPlaylist(playlistName, mediaType)
      var matchedCount = 0

      for (rawLine in lines) {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) continue
        val fileName = line.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
        val match =
          allExisting.firstOrNull {
            it.uriString == line ||
              it.title.equals(fileName, ignoreCase = true) ||
              line.contains(it.title, ignoreCase = true)
          }
        if (match != null) {
          playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, match.id))
          matchedCount++
        }
      }
      playlistName to matchedCount
    }

  suspend fun toggleFavorite(trackId: String, currentFavorite: Boolean) {
    withContext(Dispatchers.IO) { trackDao.updateFavorite(trackId, !currentFavorite) }
  }

  suspend fun recordTrackPlayed(trackId: String) {
    withContext(Dispatchers.IO) { trackDao.recordPlay(trackId, System.currentTimeMillis()) }
  }

  suspend fun updateTrackPosition(trackId: String, positionMs: Long) {
    withContext(Dispatchers.IO) { trackDao.updateLastPosition(trackId, positionMs) }
  }

  suspend fun updateLyrics(trackId: String, lyricsText: String?) {
    withContext(Dispatchers.IO) {
      trackDao.updateLyrics(trackId, lyricsText?.trim()?.takeIf { it.isNotEmpty() })
    }
  }

  suspend fun importLrcForTrack(trackId: String, lrcUri: Uri): Boolean =
    withContext(Dispatchers.IO) {
      val text =
        try {
          context.contentResolver.openInputStream(lrcUri)?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
          null
        }
      if (text.isNullOrBlank()) return@withContext false
      trackDao.updateLyrics(trackId, text.trim())
      true
    }

  suspend fun deleteLocalTrack(track: TrackEntity) {
    if (track.isBundled) return
    withContext(Dispatchers.IO) {
      trackDao.deleteTrackById(track.id)
      track.artworkFileUri?.let { uriStr ->
        if (uriStr.startsWith("file://")) {
          Uri.parse(uriStr).path?.let { path -> File(path).delete() }
        }
      }
    }
  }

  suspend fun exportBackupJson(outputUri: Uri): Int =
    withContext(Dispatchers.IO) {
      val tracks = trackDao.getAllTracksOnce()
      val playlists = playlistDao.getAllPlaylistsOnce()
      val root = JSONObject()
      val favArray = JSONArray()
      tracks.filter { it.isFavorite }.forEach { favArray.put(it.id) }
      root.put("favorites", favArray)

      val plArray = JSONArray()
      for (pl in playlists) {
        val plObj = JSONObject()
        plObj.put("id", pl.playlist.id)
        plObj.put("name", pl.playlist.name)
        plObj.put("mediaType", pl.playlist.mediaType)
        val trackIds = JSONArray()
        pl.tracks.forEach { trackIds.put(it.id) }
        plObj.put("trackIds", trackIds)
        plArray.put(plObj)
      }
      root.put("playlists", plArray)

      context.contentResolver.openOutputStream(outputUri)?.use { out ->
        out.write(root.toString(2).toByteArray(Charsets.UTF_8))
      }
      playlists.size + favArray.length()
    }

  suspend fun importBackupJson(inputUri: Uri): Int =
    withContext(Dispatchers.IO) {
      val content =
        context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readText() }
          ?: return@withContext 0
      val root = JSONObject(content)
      var restored = 0
      val favArray = root.optJSONArray("favorites")
      if (favArray != null) {
        for (i in 0 until favArray.length()) {
          val id = favArray.optString(i)
          if (id.isNotBlank()) {
            trackDao.updateFavorite(id, true)
            restored++
          }
        }
      }
      val plArray = root.optJSONArray("playlists")
      if (plArray != null) {
        for (i in 0 until plArray.length()) {
          val plObj = plArray.optJSONObject(i) ?: continue
          val plId = plObj.optString("id").ifBlank { "pl_${UUID.randomUUID()}" }
          val plName = plObj.optString("name").ifBlank { "Daftar Putar" }
          val mediaType = plObj.optString("mediaType", MediaType.AUDIO)
          playlistDao.insertPlaylist(PlaylistEntity(id = plId, name = plName, mediaType = mediaType))
          val ids = plObj.optJSONArray("trackIds")
          if (ids != null) {
            for (j in 0 until ids.length()) {
              val trackId = ids.optString(j)
              if (trackId.isNotBlank() && trackDao.getTrackById(trackId) != null) {
                playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(plId, trackId))
              }
            }
          }
          restored++
        }
      }
      restored
    }

  suspend fun clearArtworkCache(): Int =
    withContext(Dispatchers.IO) {
      val artworkDir = File(context.filesDir, "artworks")
      val cacheFiles = context.cacheDir.listFiles()?.toList().orEmpty()
      var count = 0
      cacheFiles.forEach { file ->
        if (file.deleteRecursively()) count++
      }
      if (artworkDir.exists()) {
        val activePaths =
          trackDao.getAllTracksOnce().mapNotNull { it.artworkFileUri }.toSet()
        artworkDir.listFiles()?.forEach { file ->
          val fileUri = Uri.fromFile(file).toString()
          if (fileUri !in activePaths && file.delete()) count++
        }
      }
      count
    }

  private fun resolveDisplayName(uri: Uri): String {
    var name: String? = null
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
      cursor ->
      if (cursor.moveToFirst()) {
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0) name = cursor.getString(index)
      }
    }
    val raw = name ?: uri.lastPathSegment ?: "Media Lokal"
    return raw.substringBeforeLast('.').ifBlank { raw }
  }

  private fun resolveFolderName(uri: Uri, mediaType: String): String {
    val seg = uri.lastPathSegment.orEmpty()
    val decoded = Uri.decode(seg)
    val folder =
      decoded.substringBeforeLast('/', "").substringAfterLast('/').substringAfterLast(':')
    return folder.takeIf { it.isNotBlank() }
      ?: if (mediaType == MediaType.VIDEO) "Video Lokal" else "Musik Lokal"
  }
}
