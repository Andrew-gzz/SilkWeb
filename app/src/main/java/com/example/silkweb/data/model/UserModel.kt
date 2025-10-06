package com.example.silkweb.data.model

data class UserModel(
    val id: Int,
    val idPhoto: Int?,
    val name: String,
    val lastname: String,
    val username: String,
    val email: String,
    val password: String,
    val phone: String?,
    val direction: String?,
    val createdAt: String,
    val updatedAt: String?
)


