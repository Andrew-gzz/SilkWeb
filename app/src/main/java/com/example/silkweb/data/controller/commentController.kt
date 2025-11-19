package com.example.silkweb.data.controller

import android.util.Base64
import com.example.silkweb.api.ApiClient
import com.example.silkweb.api.ApiConfig.BASE_URL
import com.example.silkweb.data.dao.MySqlConexion
import com.example.silkweb.data.model.CommentModel
import com.example.silkweb.data.model.CreateComment
import org.json.JSONObject


object commentController {
    /* createComment Antiguo
    fun createComment(data: CreateComment): Int {
        val conn = MySqlConexion.getConexion()
        val call = conn.prepareCall("{ CALL sp_create_comment(?,?,?,?) }")

        call.setInt(1, data.idPost)
        call.setString(2, data.username)

        if (data.idComment == null) {
            call.setNull(3, java.sql.Types.INTEGER)
        } else {
            call.setInt(3, data.idComment)
        }

        call.setString(4, data.body)

        val rs = call.executeQuery()

        rs.next()
        val newId = rs.getInt("new_comment_id")

        rs.close()
        call.close()
        conn.close()

        return newId
    }*/
    fun createComment(data: CreateComment): Int {
        val json = """
            {
                "idPost": ${data.idPost},
                "username": "${data.username}",
                "idComment": ${data.idComment?.toString() ?: "null"},
                "body": "${data.body}"
            }
        """.trimIndent()

        val response = ApiClient.post("/comments/create", json)

        if (response == null) return -1

        val obj = JSONObject(response)

        return obj.optInt("new_comment_id", -1)
    }
    /* getComments Antiguo
    fun getComments(idPost: Int): List<CommentModel> {
        val conn = MySqlConexion.getConexion()
        val sql = """
            SELECT * 
            FROM vw_post_comments
            WHERE post_id = ?
            ORDER BY created_at ASC;
        """
        val ps = conn.prepareStatement(sql)
        ps.setInt(1, idPost)

        val rs = ps.executeQuery()
        val list = mutableListOf<CommentModel>()

        while (rs.next()) {
            val comment = CommentModel(
                commentId = rs.getInt("comment_id"),
                postId = rs.getInt("post_id"),
                userId = rs.getInt("user_id"),
                username = rs.getString("username"),
                userName = rs.getString("user_name"),
                userLastName = rs.getString("user_lastname"),
                userPhotoFile = rs.getBytes("user_photo_file"),
                userPhotoName = rs.getString("user_photo_name"),
                userPhotoRoute = rs.getString("user_photo_route"),
                body = rs.getString("comment_body"),
                createdAt = rs.getString("created_at"),
                parentCommentId = rs.getInt("parent_comment_id").takeIf { !rs.wasNull() },
                isReply = rs.getInt("is_reply") == 1
            )
            list.add(comment)
        }

        rs.close()
        ps.close()
        conn.close()

        return list
    }*/
    fun getComments(idPost: Int): List<CommentModel> {
        val response = ApiClient.get("$BASE_URL/comments/$idPost")
            ?: return emptyList()

        val json = JSONObject(response)
        if (!json.optBoolean("success")) return emptyList()

        val array = json.getJSONArray("comments")
        val list = mutableListOf<CommentModel>()

        for (i in 0 until array.length()) {
            val c = array.getJSONObject(i)

            // 👉 Solo decodificar si es un base64 válido
            val userPhotoBytes = c.optString("user_photo_file", null)
                ?.takeIf { it.isNotEmpty() && it != "null" }
                ?.let { Base64.decode(it, Base64.DEFAULT) }

            val model = CommentModel(
                commentId = c.getInt("comment_id"),
                postId = c.getInt("post_id"),
                userId = c.getInt("user_id"),
                username = c.getString("username"),
                userName = c.getString("user_name"),
                userLastName = c.getString("user_lastname"),
                userPhotoFile = userPhotoBytes,
                userPhotoName = c.optString("user_photo_name", null),
                userPhotoRoute = c.optString("user_photo_route", null),
                body = c.getString("comment_body"),
                createdAt = c.getString("created_at"),
                parentCommentId = c.optInt("parent_comment_id").takeIf { it != 0 },
                isReply = c.getBoolean("is_reply")
            )

            list.add(model)
        }

        return list
    }

}