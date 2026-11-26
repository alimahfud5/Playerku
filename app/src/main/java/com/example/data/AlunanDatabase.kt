package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackCrossRef::class],
  version = 3,
  exportSchema = false,
)
abstract class AlunanDatabase : RoomDatabase() {
  abstract fun trackDao(): TrackDao
  abstract fun playlistDao(): PlaylistDao

  companion object {
    @Volatile private var instance: AlunanDatabase? = null

    fun getInstance(context: Context): AlunanDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                AlunanDatabase::class.java,
                "alunan_music.db",
              )
              .fallbackToDestructiveMigration()
              .build()
              .also { instance = it }
        }
    }
  }
}
