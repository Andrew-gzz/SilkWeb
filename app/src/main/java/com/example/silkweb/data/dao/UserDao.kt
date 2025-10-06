package com.example.silkweb.data.dao

import com.example.silkweb.data.model.UserModel

object UserDao {

    fun loginUser(username: String, password: String): UserModel? {
        val conn = MySqlConexion.getConexion()
        val sql = "SELECT id, username, password FROM users WHERE username = ?"
        val ps = conn.prepareStatement(sql)

        ps.setString(1, username)
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

    fun registrar(user: UserModel) {
        val conn = MySqlConexion.getConexion()
        val sql = """
        INSERT INTO users 
        (id_photo, name, lastname, username, email, password, phone, direction, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW());
    """.trimIndent()

        val ps = conn.prepareStatement(sql)

        // Manejo de campos opcionales (nullable)
        if (user.idPhoto != null) ps.setInt(1, user.idPhoto) else ps.setNull(1, java.sql.Types.INTEGER)
        ps.setString(2, user.name)
        ps.setString(3, user.lastname)
        ps.setString(4, user.username)
        ps.setString(5, user.email)
        ps.setString(6, user.password)
        if (user.phone != null) ps.setString(7, user.phone) else ps.setNull(7, java.sql.Types.VARCHAR)
        if (user.direction != null) ps.setString(8, user.direction) else ps.setNull(8, java.sql.Types.VARCHAR)

        ps.executeUpdate()
        ps.close()
        conn.close()
    }

}