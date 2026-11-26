package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey val id: String,
  val name: String,
  val mediaType: String = MediaType.AUDIO,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "playlist_tracks",
  primaryKeys = ["playlistId", "trackId"],
  indices = [Index(value = ["trackId"])],
)
data class PlaylistTrackCrossRef(
  val playlistId: String,
  val trackId: String,
  val addedAt: Long = System.currentTimeMillis(),
)

data class PlaylistWithTracks(
  @Embedded val playlist: PlaylistEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "id",
    associateBy =
      Junction(
        value = PlaylistTrackCrossRef::class,
        parentColumn = "playlistId",
        entityColumn = "trackId",
      ),
  )
  val tracks: List<TrackEntity>,
)
