package com.example.silkweb.data.controller

import com.example.silkweb.api.ApiClient
import com.example.silkweb.data.dao.MySqlConexion
import com.example.silkweb.data.model.InteractionModel
import org.json.JSONObject

object interactionController { //Ya usa HTTP
    /* interact Antiguo
    fun interact(data: InteractionModel): String {
        val conn = MySqlConexion.getConexion()
        val call = conn.prepareCall("{ CALL sp_interact(?,?,?,?) }")

        call.setInt(1, data.idPost)
        call.setString(2, data.username)

        if (data.idComment == null) {
            call.setNull(3, java.sql.Types.INTEGER)
        } else {
            call.setInt(3, data.idComment)
        }

        call.setInt(4, data.type)

        val rs = call.executeQuery()

        rs.next()
        val message = rs.getString("message")

        rs.close()
        call.close()
        conn.close()

        return message
    }*/
    fun interact(data: InteractionModel): String {
        return try {
            val response = ApiClient.interact(data)

            if (response == null) {
                "Error de servidor"
            } else {
                val json = JSONObject(response)
                json.optString("message", "Acción realizada")
            }

        } catch (e: Exception) {
            e.message ?: "Error inesperado"
        }
    }
}