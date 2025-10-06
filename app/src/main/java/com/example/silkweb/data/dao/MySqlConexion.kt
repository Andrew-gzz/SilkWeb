package com.example.silkweb.data.dao
import java.sql.Connection
import java.sql.DriverManager

object MySqlConexion {
    fun getConexion(): Connection {
        Class.forName("com.mysql.jdbc.Driver")

        return DriverManager.getConnection(
            "jdbc:mysql://192.168.1.132:3306/silkweb_db",
            "silkweb_admin",
            "3p2e43NqnNk3"
        )
    }
}