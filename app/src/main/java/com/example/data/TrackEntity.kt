package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object MediaType {
  const val AUDIO = "AUDIO"
  const val VIDEO = "VIDEO"
}

@Entity(tableName = "tracks")
data class TrackEntity(
  @PrimaryKey val id: String,
  val title: String,
  val artist: String,
  val album: String,
  val folderName: String = "Musik",
  val mediaType: String = MediaType.AUDIO,
  val uriString: String,
  val durationMs: Long,
  val artworkResName: String? = null,
  val artworkFileUri: String? = null,
  val lyricsText: String? = null,
  val isFavorite: Boolean = false,
  val isBundled: Boolean = false,
  val playCount: Int = 0,
  val lastPlayedAt: Long = 0L,
  val lastPositionMs: Long = 0L,
  val sortOrder: Int = 0,
  val addedAt: Long = System.currentTimeMillis(),
)
