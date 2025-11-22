package com.example.silkweb.data.dao

import com.example.silkweb.data.model.UserDataForUpdate
import com.example.silkweb.data.model.UserLogin
import com.example.silkweb.data.model.UserModel
import com.example.silkweb.data.model.UserRegister
import java.sql.CallableStatement
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException


object UserDao {

    fun registrar(user: UserRegister) {
        val conn = MySqlConexion.getConexion()
        val sql = """
        INSERT INTO users 
        (id_photo, name, lastname, username, email, password, phone, direction, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW());
        """.trimIndent()

        val ps = conn.prepareStatement(sql)

        // Manejo de campos opcionales (nullable)
        if (user.id_photo != null) ps.setInt(1, user.id_photo) else ps.setNull(1, java.sql.Types.INTEGER)
        ps.setString(2, user.name)
        ps.setString(3, user.lastName)
        ps.setString(4, user.username)
        ps.setString(5, user.email)
        ps.setString(6, user.password)
        if (user.phone != null) ps.setString(7, user.phone) else ps.setNull(7, java.sql.Types.VARCHAR)
        if (user.direction != null) ps.setString(8, user.direction) else ps.setNull(8, java.sql.Types.VARCHAR)

        ps.executeUpdate()
        ps.close()
        conn.close()
    }

    fun userData(username: String): UserLogin? {
        val conn = MySqlConexion.getConexion()
        val sql = "SELECT id_photo, name, lastname, username, email, password, phone, direction FROM users WHERE username = ? OR email = ?;"
        val ps = conn.prepareStatement(sql)

        ps.setString(1, username)
        ps.setString(2, username)
        val rs = ps.executeQuery()
        var user: UserLogin? = null

        if (rs.next()) {
            val dbPhoto = rs.getInt("id_photo")
            val dbName = rs.getString("name")
            val dbLastname = rs.getString("lastname")
            val dbUsername = rs.getString("username")
            val dbEmail = rs.getString("email")
            val dbPassword = rs.getString("password")
            val dbPhone = rs.getString("phone")
            val dbDirection = rs.getString("direction")

            // Validar si existe un usuario con ese nombre
            if(dbUsername == username || dbEmail == username){
                user = UserLogin(
                    dbPhoto,   // id_photo
                    dbName,     // name
                    dbLastname,     // lastname
                    dbUsername,     // username
                    dbEmail,     // email
                    dbPassword, //password
                    dbPhone,   // phone
                    dbDirection,   // direction
                )
            }else{
                user = null
            }
        }

        rs.close()
        ps.close()
        conn.close()

        return user
    }

    fun loginUserSP(user: String, password: String): String {
        val conn = MySqlConexion.getConexion()
        val call = conn.prepareCall("{ CALL sp_login_user(?, ?) }")
        call.setString(1, user)
        call.setString(2, password)

        val rs = call.executeQuery()
        var message = "Error al ejecutar el procedimiento"

        if (rs.next()) {
            message = rs.getString("message")
        }

        rs.close()
        call.close()
        conn.close()

        return message
    }

    fun modUserSP(user: UserDataForUpdate): String {
        var message = "Error desconocido al actualizar el usuario"
        var conn: Connection? = null
        var call: CallableStatement? = null
        var rs: ResultSet? = null

        try {
            conn = MySqlConexion.getConexion()
            call = conn.prepareCall("{ CALL sp_update_user_data(?,?,?,?,?,?,?,?) }")

            call.setString(1, user.username)
            call.setString(2, user.name)
            call.setString(3, user.lastname)
            call.setString(4, null)
            call.setString(5, user.phone)
            call.setString(6, user.direction)
            call.setString(7, user.password)
            call.setString(8, user.newUsername)

            rs = call.executeQuery()

            if (rs.next()) {
                message = rs.getString("message")
            } else {
                message = "El procedimiento no devolvió ningún resultado"
            }

        } catch (e: SQLException) {
            e.printStackTrace()
            message = "❌ Error SQL: ${e.message}"
        } catch (e: Exception) {
            e.printStackTrace()
            message = "⚠️ Error general: ${e.message}"
        } finally {
            try {
                rs?.close()
                call?.close()
                conn?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                message = "⚠️ Error al cerrar la conexión: ${e.message}"
            }
        }

        return message
    }

    //Validaciones
    fun checkUserExistsSP(username:String): String {
        val conn = MySqlConexion.getConexion()
        val call = conn.prepareCall("{ CALL sp_check_username(?) }")
        call.setString(1, username)

        val rs = call.executeQuery()
        var message = "Error al ejecutar el procedimiento"

        if (rs.next()) {
            message = rs.getString("message")
        }

        rs.close()
        call.close()
        conn.close()

        return message
    }
    fun checkEmailExistsSP(email:String): String {
        val conn = MySqlConexion.getConexion()
        val call = conn.prepareCall("{ CALL sp_check_email(?) }")
        call.setString(1, email)

        val rs = call.executeQuery()
        var message = "Error al ejecutar el procedimiento"

        if (rs.next()) {
            message = rs.getString("message")
        }

        rs.close()
        call.close()
        conn.close()

        return message
    }
    fun checkFullNameExistsSP(name:String, lastname:String): String {
        val conn = MySqlConexion.getConexion()
        val call = conn.prepareCall("{ CALL sp_check_fullname(?,?) }")
        call.setString(1, name)
        call.setString(2, lastname)

        val rs = call.executeQuery()
        var message = "Error al ejecutar el procedimiento"

        if (rs.next()) {
            message = rs.getString("message")
        }

        rs.close()
        call.close()
        conn.close()

        return message
    }

}