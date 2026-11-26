package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
  @PrimaryKey val id: String,
  val title: String,
  val artist: String,
  val album: String,
  val uriString: String,
  val durationMs: Long,
  val artworkResName: String? = null,
  val artworkFileUri: String? = null,
  val isFavorite: Boolean = false,
  val isBundled: Boolean = false,
  val sortOrder: Int = 0,
  val addedAt: Long = System.currentTimeMillis(),
)
