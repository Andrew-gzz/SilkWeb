package com.example.silkweb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

//Aqui se guardaran las publicaciones de los usuarios y borradores del usuario
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idPost: Int? = null, //En caso de ser una publicación  optenida de la bd externa
    val userId: Int,
    val title: String,
    val body: String,
    val isDraft: Boolean,
    val createdAt: String,
    val isPendingPublish: Boolean = false
)
