package com.example.silkweb.data.controller

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.silkweb.api.ApiClient
import com.example.silkweb.api.ApiConfig.BASE_URL
import com.example.silkweb.data.dao.MySqlConexion
import com.example.silkweb.data.model.CreatePostModel
import com.example.silkweb.data.model.PostModel
import org.json.JSONArray
import org.json.JSONObject
import java.sql.Types

object postController {

    fun createPost(ctx: Context, post: CreatePostModel, imageUris: List<Uri>): Int {
        val mediaJson = buildMediaJson(ctx, imageUris)

        val response = ApiClient.createPost(post, mediaJson)

        if (response != null) {
            val json = JSONObject(response)

            if (json.optBoolean("success", false)) {
                return json.optInt("newPostId", -1)
            } else {
                throw Exception(json.optString("message", "No se pudo crear el post"))
            }
        }

        return -1
    }
    fun updatePost(postId: Int, username: String, title: String, body: String, mediaJson: String): Boolean {
        val response = ApiClient.updatePost(postId, username, title, body, mediaJson) ?: return false

        val json = JSONObject(response)
        return json.optBoolean("success", false)
    }
    /*IMPORTANTE PARA ELIMINAR POSTS*/
    fun deletePost(postId: Int): Boolean {
        val response = ApiClient.deletePost(postId) ?: return false
        val json = JSONObject(response)
        return json.optBoolean("success", false)
    }

    // --- Helpers ---
    fun buildMediaJson(ctx: Context, uris: List<Uri>): String? {
        if (uris.isEmpty()) return null
        val arr = JSONArray()

        for (uri in uris) {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val name = getFileName(ctx, uri) ?: "image_${System.currentTimeMillis()}.jpg"

            val obj = JSONObject().apply {
                put("file_b64", b64)
                put("file_name", name)
                put("route", JSONObject.NULL) // si fuera URL externa, colócala aquí
                put("status", 1)
            }
            arr.put(obj)
        }
        return if (arr.length() == 0) null else arr.toString()
    }
    fun getFileName(ctx: Context, uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = ctx.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) {
                name = it.getString(idx)
            }
        }
        return name
    }

    // -- Cosas acerca de los posts --
    /* getFeed Antiguo
    fun getFeed(): List<PostModel> {

        val conn = MySqlConexion.getConexion()
        val sql = "SELECT * FROM vw_feed_posts ORDER BY created_at DESC;"
        val ps = conn.prepareStatement(sql)
        val rs = ps.executeQuery()

        val list = mutableListOf<PostModel>()

        while (rs.next()) {
            val post = PostModel(
                id = rs.getInt("post_id"),
                username = rs.getString("username"),

                userPhotoFile = rs.getBytes("user_photo_file"),
                userPhotoName = rs.getString("user_photo_name"),
                userPhotoRoute = rs.getString("user_photo_route"),

                title = rs.getString("post_title"),
                body = rs.getString("post_body"),
                createdAt = rs.getString("created_at"),

                mediaListJson = rs.getString("media_list"),

                likeCount = rs.getInt("like_count"),
                commentCount = rs.getInt("comment_count")
            )

            list.add(post)
        }

        rs.close()
        ps.close()
        conn.close()

        return list

    }*/
    fun getFeed(limit: Int = 10, offset: Int = 0): List<PostModel> {
        val url = "$BASE_URL/posts/feed?limit=$limit&offset=$offset"
        val response = ApiClient.get(url) ?: return emptyList()

        val json = JSONObject(response)

        if (!json.optBoolean("success")) return emptyList()

        val feedArray = json.getJSONArray("feed")
        val list = mutableListOf<PostModel>()

        for (i in 0 until feedArray.length()) {
            val item = feedArray.getJSONObject(i)

            val userPhotoBytes = item.optString("user_photo_file", null)
                ?.takeIf { it.isNotEmpty() && it != "null" }
                ?.let { Base64.decode(it, Base64.DEFAULT) }

            val post = PostModel(
                id = item.getInt("post_id"),
                username = item.getString("username"),

                userPhotoFile = userPhotoBytes,
                userPhotoName = item.optString("user_photo_name", null),
                userPhotoRoute = item.optString("user_photo_route", null),

                title = item.getString("post_title"),
                body = item.getString("post_body"),
                createdAt = item.getString("created_at"),

                mediaListJson = item.optString("media_list", null),

                likeCount = item.getInt("like_count"),
                commentCount = item.getInt("comment_count"),

                mediaFiles = null // luego lo llenas con getPostMedia
            )

            list.add(post)
        }

        return list
    }
    fun getFeedFull(): List<PostModel> {
        val response = ApiClient.getAllPosts() ?: return emptyList()

        val json = JSONObject(response)
        if (!json.optBoolean("success")) return emptyList()

        val feedArray = json.getJSONArray("feed")
        val list = mutableListOf<PostModel>()

        for (i in 0 until feedArray.length()) {
            val item = feedArray.getJSONObject(i)

            val userPhotoBytes = item.optString("user_photo_file", null)
                ?.takeIf { it.isNotEmpty() && it != "null" }
                ?.let { Base64.decode(it, Base64.DEFAULT) }

            val post = PostModel(
                id = item.getInt("post_id"),
                username = item.getString("username"),

                userPhotoFile = userPhotoBytes,
                userPhotoName = item.optString("user_photo_name", null),
                userPhotoRoute = item.optString("user_photo_route", null),

                title = item.getString("post_title"),
                body = item.getString("post_body"),
                createdAt = item.getString("created_at"),

                mediaListJson = item.optString("media_list", null),

                likeCount = item.getInt("like_count"),
                commentCount = item.getInt("comment_count"),

                mediaFiles = null
            )

            list.add(post)
        }

        return list
    }
    fun getMyPosts(username: String): List<PostModel> {
        val response = ApiClient.getMyPosts(username) ?: return emptyList()

        val json = JSONObject(response)
        if (!json.optBoolean("success")) return emptyList()

        val feedArray = json.getJSONArray("feed")
        val list = mutableListOf<PostModel>()

        for (i in 0 until feedArray.length()) {
            val item = feedArray.getJSONObject(i)

            val userPhotoBytes = item.optString("user_photo_file", null)
                ?.takeIf { it.isNotEmpty() && it != "null" }
                ?.let { Base64.decode(it, Base64.DEFAULT) }

            val post = PostModel(
                id = item.getInt("post_id"),
                username = item.getString("username"),

                userPhotoFile = userPhotoBytes,
                userPhotoName = item.optString("user_photo_name", null),
                userPhotoRoute = item.optString("user_photo_route", null),

                title = item.getString("post_title"),
                body = item.getString("post_body"),
                createdAt = item.getString("created_at"),

                mediaListJson = item.optString("media_list", null),

                likeCount = item.getInt("like_count"),
                commentCount = item.getInt("comment_count"),

                mediaFiles = null
            )

            list.add(post)
        }

        return list
    }

    /* GetPostMedia Antiguo
    fun getPostMedia(postId: Int): MutableMap<Int, ByteArray> {
        val conn = MySqlConexion.getConexion()
        val call = conn.prepareCall("{ CALL sp_get_post_media(?) }")
        call.setInt(1, postId)

        val rs = call.executeQuery()

        val map = mutableMapOf<Int, ByteArray>()

        while (rs.next()) {
            val id = rs.getInt("media_id")
            val fileBytes = rs.getBytes("file")
            if (fileBytes != null)
                map[id] = fileBytes
        }

        rs.close()
        call.close()
        conn.close()

        return map
    }*/
    fun getPostMedia(postId: Int): MutableMap<Int, ByteArray> {
        val url = "$BASE_URL/posts/media/$postId"
        val response = ApiClient.get(url)

        val map = mutableMapOf<Int, ByteArray>()

        if (response == null) return map

        val json = JSONObject(response)

        if (!json.optBoolean("success")) return map

        val mediaArray = json.getJSONArray("media")

        for (i in 0 until mediaArray.length()) {
            val item = mediaArray.getJSONObject(i)

            val mediaId = item.getInt("media_id")
            val base64Data = item.optString("file", "")

            if (base64Data.isNotEmpty()) {
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                map[mediaId] = bytes
            }
        }
        return map
    }

    /*  getPostDetails Antiguo
    fun getPostDetails(idPost: Int): PostModel? {
        val conn = MySqlConexion.getConexion()
        val sql = "SELECT * FROM vw_feed_posts WHERE post_id = ? LIMIT 1"

        val ps = conn.prepareStatement(sql)
        ps.setInt(1, idPost)
        val rs = ps.executeQuery()

        var post: PostModel? = null

        if (rs.next()) {
            post = PostModel(
                id = rs.getInt("post_id"),
                username = rs.getString("username"),

                userPhotoFile = rs.getBytes("user_photo_file"),
                userPhotoName = rs.getString("user_photo_name"),
                userPhotoRoute = rs.getString("user_photo_route"),

                title = rs.getString("post_title"),
                body = rs.getString("post_body"),
                createdAt = rs.getString("created_at"),

                mediaListJson = rs.getString("media_list"),

                likeCount = rs.getInt("like_count"),
                commentCount = rs.getInt("comment_count")
            )
        }

        rs.close()
        ps.close()
        conn.close()

        return post
    }*/
    fun getPostDetails(idPost: Int): PostModel? {
        val url = "$BASE_URL/posts/details/$idPost"
        val response = ApiClient.get(url) ?: return null

        val json = JSONObject(response)
        if (!json.optBoolean("success")) return null

        val item = json.getJSONObject("post")

        // 👉 Solo decodificar si el string tiene base64 válido
        val userPhotoBytes = item.optString("user_photo_file", null)
            ?.takeIf { it.isNotEmpty() && it != "null" }
            ?.let { Base64.decode(it, Base64.DEFAULT) }

        return PostModel(
            id = item.getInt("post_id"),
            username = item.getString("username"),

            userPhotoFile = userPhotoBytes,
            userPhotoName = item.optString("user_photo_name", null),
            userPhotoRoute = item.optString("user_photo_route", null),

            title = item.getString("post_title"),
            body = item.getString("post_body"),
            createdAt = item.getString("created_at"),

            mediaListJson = item.optString("media_list", null),

            likeCount = item.getInt("like_count"),
            commentCount = item.getInt("comment_count"),

            mediaFiles = null
        )
    }

}
