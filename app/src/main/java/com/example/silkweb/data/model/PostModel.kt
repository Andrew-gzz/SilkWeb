package com.example.silkweb.data.model

data class CreatePostModel(
    val username: String,
    val title: String,
    val body: String,
    val status: Int = 1
)

data class PostModel(
    val id: Int,
    val username: String,

    val userPhotoFile: ByteArray?,
    val userPhotoName: String?,
    val userPhotoRoute: String?,

    val title: String,
    val body: String,
    val createdAt: String,

    val mediaListJson: String?,   // viene del VIEW
    val likeCount: Int,
    val commentCount: Int,

    var mediaFiles: MutableMap<Int, ByteArray>? = null
)
