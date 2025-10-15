package com.example.silkweb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idPhoto: Int? = null,
    val name: String? = null,
    val lastName: String? = null,
    val username: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val direction: String? = null
)


