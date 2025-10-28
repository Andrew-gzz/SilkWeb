package com.example.silkweb.data.model

data class MediaModel(
    val id: Int?,
    val idPost: Int?,
    val fileName: String?,
    val route: String?
)
data class MediaDB(
    val id: Int?,
    val idPost: Int?,
    val file: ByteArray?,
    val fileName: String?,
    val route: String?,
    val status: Boolean?
)
