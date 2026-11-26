package com.example.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.R
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(
  private val context: Context,
  private val trackDao: TrackDao,
) {
  val allTracks: Flow<List<TrackEntity>> = trackDao.observeAllTracks()
  val favoriteTracks: Flow<List<TrackEntity>> = trackDao.observeFavoriteTracks()

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
              uriString = "$scheme://$pkg/${R.raw.senja_di_pantai}",
              durationMs = 32_000L,
              artworkResName = "img_art_senja",
              artworkFileUri = "$scheme://$pkg/${R.drawable.img_art_senja}",
              isFavorite = true,
              isBundled = true,
              sortOrder = 0,
            ),
            TrackEntity(
              id = "bundled_hujan",
              title = "Hujan Malam",
              artist = "Kopi & Rintik",
              album = "Teduh",
              uriString = "$scheme://$pkg/${R.raw.hujan_malam}",
              durationMs = 32_000L,
              artworkResName = "img_art_hujan",
              artworkFileUri = "$scheme://$pkg/${R.drawable.img_art_hujan}",
              isFavorite = false,
              isBundled = true,
              sortOrder = 1,
            ),
            TrackEntity(
              id = "bundled_gema",
              title = "Gema Analog",
              artist = "Sintesa Ruang",
              album = "Frekuensi",
              uriString = "$scheme://$pkg/${R.raw.gema_analog}",
              durationMs = 32_000L,
              artworkResName = "img_art_malam",
              artworkFileUri = "$scheme://$pkg/${R.drawable.img_art_malam}",
              isFavorite = false,
              isBundled = true,
              sortOrder = 2,
            ),
            TrackEntity(
              id = "bundled_embun",
              title = "Embun Pagi",
              artist = "Akustik Rimba",
              album = "Fajar",
              uriString = "$scheme://$pkg/${R.raw.embun_pagi}",
              durationMs = 28_000L,
              artworkResName = "img_art_pagi",
              artworkFileUri = "$scheme://$pkg/${R.drawable.img_art_pagi}",
              isFavorite = false,
              isBundled = true,
              sortOrder = 3,
            ),
          )
        trackDao.insertTracks(bundled)
      }
    }

  suspend fun importAudioUris(uris: List<Uri>): Int =
    withContext(Dispatchers.IO) {
      var importedCount = 0
      val artworkDir = File(context.filesDir, "artworks").apply { mkdirs() }
      for (uri in uris) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        } catch (_: SecurityException) {
          // Some providers issue non-persistable temporary grants; still playable in-session.
        }

        val trackId = "local_${UUID.nameUUIDFromBytes(uri.toString().toByteArray())}"
        val fallbackName = resolveDisplayName(uri)
        var title = fallbackName
        var artist = "Artis Lokal"
        var album = "Koleksi Perangkat"
        var durationMs = 0L
        var artUri: String? = null

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
          retriever.embeddedPicture?.let { bytes ->
            val artFile = File(artworkDir, "$trackId.jpg")
            artFile.writeBytes(bytes)
            artUri = Uri.fromFile(artFile).toString()
          }
        } catch (_: Exception) {
          // Fallback to display name when metadata extraction fails
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
            uriString = uri.toString(),
            durationMs = durationMs,
            artworkResName = null,
            artworkFileUri = artUri,
            isFavorite = false,
            isBundled = false,
            sortOrder = 100,
          )
        trackDao.insertTrack(entity)
        importedCount++
      }
      importedCount
    }

  suspend fun toggleFavorite(trackId: String, currentFavorite: Boolean) {
    withContext(Dispatchers.IO) { trackDao.updateFavorite(trackId, !currentFavorite) }
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

  private fun resolveDisplayName(uri: Uri): String {
    var name: String? = null
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
      cursor ->
      if (cursor.moveToFirst()) {
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0) {
          name = cursor.getString(index)
        }
      }
    }
    val raw = name ?: uri.lastPathSegment ?: "Lagu Lokal"
    return raw.substringBeforeLast('.').ifBlank { raw }
  }
}
