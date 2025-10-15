package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle

import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.silkweb.R
import com.example.silkweb.data.local.AppDatabase
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setData()
        options()
    }

    private fun setData(){
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDaoLocal()

        lifecycleScope.launch {
            val user = userDao.getUser()
            runOnUiThread {
                if (user != null) {
                    val usernameView = findViewById<TextView>(R.id.id_tvUsername)
                    usernameView.text = user.username  // 👈 mostramos el username
                } else {
                    val usernameView = findViewById<TextView>(R.id.id_tvUsername)
                    usernameView.text = "user101_"
                }
            }
        }
    }
    private fun options() {
        val btn = findViewById<ImageButton>(R.id.id_back)
        val btnDrafts = findViewById<Button>(R.id.id_btnDrafts)
        val btnConfig = findViewById<Button>(R.id.id_btnConfig)
        val btnLogout = findViewById<Button>(R.id.id_btnLogout)

        btn.setOnClickListener {
            finish()
        }

        btnDrafts.setOnClickListener {

        }

        btnConfig.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            logoutUser()
        }
    }
    private fun logoutUser() {
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDaoLocal()

        lifecycleScope.launch {
            try {
                // Borra el usuario local
                userDao.clearUsers()

                // Notifica al usuario
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                }

                // Regresa al Main
                finish()

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Error al cerrar sesión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}