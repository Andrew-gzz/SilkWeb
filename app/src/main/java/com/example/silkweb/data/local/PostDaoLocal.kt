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
}