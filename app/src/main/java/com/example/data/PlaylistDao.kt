package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
  @Transaction
  @Query("SELECT * FROM playlists WHERE mediaType = :mediaType ORDER BY createdAt DESC")
  fun observePlaylistsByType(mediaType: String): Flow<List<PlaylistWithTracks>>

  @Transaction
  @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
  suspend fun getAllPlaylistsOnce(): List<PlaylistWithTracks>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun addTrackToPlaylist(ref: PlaylistTrackCrossRef)

  @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
  suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

  @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun clearPlaylistTracks(playlistId: String)

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: String)
}
