package com.example.silkweb.data.local


import androidx.room.*
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Date

@Dao
interface PostDaoLocal {
    //insert, delete and update
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: PostEntity): Long
    
    //Eliminar publicaciones al momento de cerrar sesion
    @Query("DELETE FROM posts")
    suspend fun clearPosts()

    //Eliminar borrador
    @Query("DELETE FROM posts WHERE id = :id AND isDraft = true")
    suspend fun deletePostById(id: Int)

    //Modificar la publicación
    @Query("""
       UPDATE posts SET
        userId= :userId,
        title= :title,
        body= :body,
        isDraft= :isDraft,
        createdAt= :createdAt
       WHERE id = :id
    """)

    suspend fun updatePostData(
        id: Int,
        userId: Int,
        title: String,
        body: String,
        isDraft: Boolean,
        createdAt: String
    )
    @Query("SELECT * FROM posts WHERE isDraft = 1 ORDER BY id DESC")
    suspend fun getDrafts(): List<PostEntity>
    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getDraftById(id: Int): PostEntity?
    //---------------------------------//
    // Modo Offline                    //
    //---------------------------------//
    // Obtener publicaciones pendientes por publicar
    @Query("SELECT * FROM posts WHERE isPendingPublish = 1 ORDER BY id ASC")
    suspend fun getPendingPosts(): List<PostEntity>

    // Eliminar publicación pendiente
    @Query("DELETE FROM posts WHERE id = :id AND isPendingPublish = 1")
    suspend fun deletePendingPost(id: Int)

    @Query("UPDATE posts SET isPendingPublish = :pending WHERE id = :id")
    suspend fun updatePendingStatus(id: Int, pending: Boolean)


}