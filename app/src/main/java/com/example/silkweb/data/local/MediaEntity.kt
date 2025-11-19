package com.example.silkweb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

//Esta entidad o tabla solo guardara id's de las fotos ya sea de publicaciones o de perfil para hacer eficiente el manejo de memoria
@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idPost: Int? = null,
    val fileName: String? = null,
    val route:String? = null,
    val localUri: String? = null
)
