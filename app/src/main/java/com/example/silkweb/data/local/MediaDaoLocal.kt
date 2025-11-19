package com.example.silkweb.data.local

import androidx.room.*
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
@Dao
interface MediaDaoLocal {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity)

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getMediaById(id: Int): MediaEntity?

    @Query("DELETE FROM media")
    suspend fun clearMedia()

    @Query("SELECT * FROM media")
    suspend fun getAllMedia(): List<MediaEntity>

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteMediaById(id: Int)

    //En caso de eliminar el borrador tambien se eliminaran las fotos adjuntas
    @Query("DELETE FROM media WHERE idPost = :idPost")
    suspend fun deleteMediaByPostId(idPost: Int)

}
