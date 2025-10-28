package com.example.silkweb.data.model

data class CreatePostModel(
    val username: String,
    val title: String,
    val body: String,
    val status: Int = 1
)

