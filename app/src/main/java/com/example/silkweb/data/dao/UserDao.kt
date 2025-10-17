package com.example.silkweb.data.dao

import com.example.silkweb.data.model.UserLogin
import com.example.silkweb.data.model.UserModel
import com.example.silkweb.data.model.UserRegister


object UserDao {

    fun loginUser(user: String, password: String): UserModel? {
        val conn = MySqlConexion.getConexion()
        val sql = "SELECT id, username, password FROM users WHERE username = ? OR email = ?"
        val ps = conn.prepareStatement(sql)

        ps.setString(1, user)
        ps.setString(2, user)
        val rs = ps.executeQuery()

        var user: UserModel? = null

        if (rs.next()) {
            val dbPassword = rs.getString("password")

            // Validar contraseña
            if (dbPassword == password) {
                user = UserModel(
                    rs.getInt("id"),
                    null,   // idPhoto
                    "",     // name
                    "",     // lastname
                    rs.getString("username"),
                    "",     // email
                    dbPassword,
                    null,   // phone
                    null,   // direction
                    "",     // createdAt
                    null    // updatedAt
                )
            } else {
                // Contraseña incorrecta → retorna null
                user = null
            }
        }

        rs.close()
        ps.close()
        conn.close()

        return user
    }

    fun listar(dato: String): List<UserModel>{
        val lista = mutableListOf<UserModel>()
        val ps = MySqlConexion.getConexion().prepareStatement(
            "SELECT id, username, password FROM users WHERE username LIKE ? OR password LIKE concat('%',?,'%');"
        )

        ps.setString(1, dato)

        val rs = ps.executeQuery()

        while (rs.next()){
            lista.add(
                UserModel(
                    rs.getInt("id"),
                    null,
                    "",
                    "",
                    rs.getString("username"),
                    "",
                    rs.getString("password"),
                    null,
                    null,
                    "",
                    null
                )
            )
        }

        rs.close()
        ps.close()

        return lista
    }

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

    fun checkUserExists(username:String): UserModel? {
        val conn = MySqlConexion.getConexion()
        val sql = "SELECT id, username FROM users WHERE username = ?"
        val ps = conn.prepareStatement(sql)

        ps.setString(1, username)
        val rs = ps.executeQuery()
        var user: UserModel? = null

        if (rs.next()) {
            val dbUser = rs.getString("username")

            // Validar si existe un usuario con ese nombre
             if(dbUser == username){
                user = UserModel(
                    rs.getInt("id"),
                    null,   // idPhoto
                    "",     // name
                    "",     // lastname
                    dbUser,
                    "",     // email
                    "",
                    null,   // phone
                    null,   // direction
                    "",     // createdAt
                    null    // updatedAt
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

    fun checkEmailExists(email:String): UserModel? {
        val conn = MySqlConexion.getConexion()
        val sql = "SELECT id, email FROM users WHERE email = ?"
        val ps = conn.prepareStatement(sql)

        ps.setString(1, email)
        val rs = ps.executeQuery()
        var user: UserModel? = null

        if (rs.next()) {
            val dbEmail = rs.getString("email")

            // Validar si existe un usuario con ese nombre
            if(dbEmail == email){
                user = UserModel(
                    rs.getInt("id"),
                    null,   // idPhoto
                    "",     // name
                    "",     // lastname
                    "",
                    dbEmail,     // email
                    "",
                    null,   // phone
                    null,   // direction
                    "",     // createdAt
                    null    // updatedAt
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

    fun checkFullNameExists(name:String, lastname:String): UserModel? {
        val conn = MySqlConexion.getConexion()
        val sql = "SELECT id, name, lastname FROM users WHERE name = ? AND lastname = ?"
        val ps = conn.prepareStatement(sql)

        ps.setString(1, name)
        ps.setString(2, lastname)

        val rs = ps.executeQuery()
        var user: UserModel? = null

        if (rs.next()) {
            val dbname = rs.getString("name")
            val dblastname = rs.getString("lastname")

            // Validar si existe un usuario con ese nombre
            if(dbname == name && dblastname == lastname){
                user = UserModel(
                    rs.getInt("id"),
                    null,   // idPhoto
                    dbname,     // name
                    dblastname,     // lastname
                    "",
                    "",     // email
                    "",
                    null,   // phone
                    null,   // direction
                    "",     // createdAt
                    null    // updatedAt
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
}