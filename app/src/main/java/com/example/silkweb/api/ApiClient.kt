package com.example.silkweb.api

import com.example.silkweb.data.model.CreatePostModel
import com.example.silkweb.data.model.InteractionModel
import com.example.silkweb.data.model.UserDataForUpdate
import com.example.silkweb.data.model.UserRegister
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ApiClient {

    private const val BASE_URL = ApiConfig.BASE_URL

    private val client = OkHttpClient()

    fun login(username: String, password: String): String? {
        val json = """
            {
                "username": "$username",
                "password": "$password"
            }
        """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }
    fun getProfilePhoto(username: String): String? {
        val request = Request.Builder()
            .url("$BASE_URL/media/photo/$username")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }
    fun register(user: UserRegister): String? {
        val json = """
        {
            "id_photo": ${user.id_photo ?: "null"},
            "name": "${user.name}",
            "lastname": "${user.lastName}",
            "username": "${user.username}",
            "email": "${user.email}",
            "password": "${user.password}",
            "phone": ${if (user.phone != null) "\"${user.phone}\"" else "null"},
            "direction": ${if (user.direction != null) "\"${user.direction}\"" else "null"}
        }
    """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/auth/register")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }
    fun updateProfileImage(username: String, bytes: ByteArray, fileName: String): String? {
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

        val json = """
        {
            "username": "$username",
            "file": "$base64",
            "fileName": "$fileName"
        }
    """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/media/update-profile-photo")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }
    fun updateUserData(user: UserDataForUpdate): String? {
        val json = """
        {
            "username": "${user.username}",
            "newUsername": ${if (!user.newUsername.isNullOrBlank()) "\"${user.newUsername}\"" else "null"},
            "name": "${user.name}",
            "lastname": "${user.lastname}",
            "email": "${user.email}",         
            "phone": ${if (user.phone != null) "\"${user.phone}\"" else "null"},
            "direction": ${if (user.direction != null) "\"${user.direction}\"" else "null"},
            "password": "${user.password}"
        }
    """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/auth/update")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }

    //Funciones para validaciones con base de datos
    fun checkUsername(username: String): String? {
        val request = Request.Builder()
            .url("$BASE_URL/auth/check-username/$username")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }
    fun checkEmail(email: String): String? {
        val request = Request.Builder()
            .url("$BASE_URL/auth/check-email/$email")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }
    fun checkFullname(name: String, lastname: String): String? {
        val request = Request.Builder()
            .url("$BASE_URL/auth/check-fullname?name=$name&lastname=$lastname")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }

    //------------------------//
    //      Publicaciones     //
    //------------------------//
    fun createPost(post: CreatePostModel, mediaJson: String?): String? {
        // Construimos el JSON de forma segura
        val jsonObject = JSONObject().apply {
            put("username", post.username)
            put("title", post.title)
            put("body", post.body)
            put("status", post.status)

            // sp_create_post espera un STRING con JSON, así que lo mandamos como string
            if (mediaJson != null) {
                put("mediaJson", mediaJson)   // JSONObject se encarga de escaparlo
            } else {
                put("mediaJson", JSONObject.NULL)
            }
        }

        val json = jsonObject.toString()

        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/posts/create")   // o "$BASE_URL/api/posts/create" según tu BASE_URL
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }
    fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }

    //------------------------//
    //      Interacciones     //
    //------------------------//
    fun interact(data: InteractionModel): String? {
        val json = """
        {
            "idPost": ${data.idPost},
            "username": "${data.username}",
            "idComment": ${data.idComment?.toString() ?: "null"},
            "type": ${data.type}
        }
    """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/interact")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }

    //------------------------//
    //         Comments       //
    //------------------------//
    fun post(endpoint: String, json: String): String? {
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL$endpoint")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string()
        }
    }

}
