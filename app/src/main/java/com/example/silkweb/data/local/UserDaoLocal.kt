package com.example.silkweb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDaoLocal {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("""
        UPDATE users SET 
            name = :name,
            lastName = :lastName,
            username = :username,
            email = :email,
            password = :password,
            phone = :phone,
            direction = :direction
        WHERE id = :id
    """)
    suspend fun updateUserData(
        id: Int,
        name: String?,
        lastName: String?,
        username: String,
        email: String,
        password: String,
        phone: String?,
        direction: String?
    )
}
