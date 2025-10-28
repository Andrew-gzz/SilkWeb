package com.example.silkweb.data.controller

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.silkweb.data.dao.MySqlConexion
import com.example.silkweb.data.model.CreatePostModel
import org.json.JSONArray
import org.json.JSONObject
import java.sql.Types

object postController {

    fun createPost(ctx: Context, post: CreatePostModel, imageUris: List<Uri>): Int {
        val conn = MySqlConexion.getConexion()
        val call = conn.prepareCall("{ CALL sp_create_post(?, ?, ?, ?, ?) }")

        val mediaJson = buildMediaJson(ctx, imageUris)

        call.setString(1, post.username)  // <-- antes era userId (Int)
        call.setString(2, post.title)
        call.setString(3, post.body)
        call.setInt(4, post.status)
        if (mediaJson == null) call.setNull(5, Types.VARCHAR) else call.setString(5, mediaJson)

        val rs = call.executeQuery()
        var newId = -1
        if (rs.next()) newId = rs.getInt("new_post_id")

        rs.close(); call.close(); conn.close()
        return newId
    }

    // --- Helpers ---
    private fun buildMediaJson(ctx: Context, uris: List<Uri>): String? {
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

    private fun getFileName(ctx: Context, uri: Uri): String? {
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
}
