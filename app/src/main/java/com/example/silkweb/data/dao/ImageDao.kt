package com.example.silkweb.data.dao

import com.example.silkweb.data.model.MediaDB

object ImageDao {

    fun addProfileImage(username: String, imageBytes: ByteArray, fileName: String, filePath: String): Int {
        var newId = -1
        try {
            val conn = MySqlConexion.getConexion()
            val call = conn.prepareCall("{ CALL sp_update_user_profile_image(?, ?, ?, ?) }")
            call.setString(1, username)
            call.setBytes(2, imageBytes)
            call.setString(3, fileName)
            call.setString(4, filePath)

            val rs = call.executeQuery()
            if (rs.next()) {
                val msg = rs.getString("message")
                // Extraer solo el número del mensaje, ej: "Foto actualizada correctamente, nueva ID = 45"
                val regex = Regex("(\\d+)")
                val match = regex.find(msg)
                if (match != null) {
                    newId = match.value.toInt()
                }
            }

            rs.close()
            call.close()
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newId
    }
    fun getPhotoDataByUsername(username: String): MediaDB? {
        var media: MediaDB? = null
        try {
            val conn = MySqlConexion.getConexion()
            val call = conn.prepareCall("{ CALL sp_get_photo_data_by_username(?) }")
            call.setString(1, username)

            val rs = call.executeQuery()
            if (rs.next()) {
                val fileBytes = rs.getBytes("photo_file") // los bytes del blob
                val fileName = rs.getString("photo_name")
                val route = rs.getString("photo_route")

                media = MediaDB(
                    id = 0, // si no necesitas ID
                    idPost = null,
                    file = fileBytes,
                    fileName = fileName,
                    route = route,
                    status = true
                )
            }

            rs.close()
            call.close()
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return media
    }


}
