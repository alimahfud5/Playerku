package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
  @Query(
    "SELECT * FROM tracks WHERE mediaType = :mediaType ORDER BY isBundled DESC, sortOrder ASC, addedAt DESC"
  )
  fun observeTracksByType(mediaType: String): Flow<List<TrackEntity>>

  @Query("SELECT * FROM tracks ORDER BY isBundled DESC, sortOrder ASC, addedAt DESC")
  fun observeAllTracks(): Flow<List<TrackEntity>>

  @Query("SELECT COUNT(*) FROM tracks")
  suspend fun getTrackCount(): Int

  @Query("SELECT * FROM tracks")
  suspend fun getAllTracksOnce(): List<TrackEntity>

  @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
  suspend fun getTrackById(id: String): TrackEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTracks(tracks: List<TrackEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrack(track: TrackEntity)

  @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :id")
  suspend fun updateFavorite(id: String, isFavorite: Boolean)

  @Query("UPDATE tracks SET lyricsText = :lyricsText WHERE id = :id")
  suspend fun updateLyrics(id: String, lyricsText: String?)

  @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :id")
  suspend fun recordPlay(id: String, playedAt: Long)

  @Query("UPDATE tracks SET lastPositionMs = :positionMs WHERE id = :id")
  suspend fun updateLastPosition(id: String, positionMs: Long)

  @Query("DELETE FROM tracks WHERE id = :id")
  suspend fun deleteTrackById(id: String)
}
