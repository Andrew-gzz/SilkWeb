package com.example.silkweb.data.model

data class InteractionModel(
    val idPost: Int,
    val username: String,
    val idComment: Int? = null,
    val type: Int,
)