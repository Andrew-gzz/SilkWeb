package com.example.silkweb.presentation.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.silkweb.R
import com.example.silkweb.data.dao.UserDao

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Referencias a los campos
        val etUser = findViewById<EditText>(R.id.id_EtUser)   // 👈 tu id correcto
        val etPassword = findViewById<EditText>(R.id.id_etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUser.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                Thread {
                    try {
                        val user = UserDao.loginUser(username, password)

                        runOnUiThread {
                            if (user == null) {
                                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Bienvenido ${user.username}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this, "Error 500: Fallo de conexión con la BD\n${e.message}", Toast.LENGTH_LONG).show()
                        }
                        e.printStackTrace() // Para ver en el Logcat la traza completa
                    }
                }.start()
            }
        }

    }
}
