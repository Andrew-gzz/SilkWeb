package com.example.silkweb.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle

import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.silkweb.R
import com.example.silkweb.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setData()
        options()
    }
    override fun onResume() {
        super.onResume()
        setData()
    }

    private fun setData() {
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDaoLocal()
        val mediaDao = db.mediaDaoLocal()
        val usernameView = findViewById<TextView>(R.id.id_tvUsername)
        val photoView = findViewById<ImageView>(R.id.id_ivProfileImage)

        lifecycleScope.launch {
            // 1) Trae usuario
            val user = withContext(Dispatchers.IO) { userDao.getUser() }

            // 2) Pinta username
            usernameView.text = user?.username ?: "user101_"

            // 3) Si hay id de foto, busca media
            val media = if (user?.idPhoto != null) {
                withContext(Dispatchers.IO) { mediaDao.getMediaById(user.idPhoto!!) }
            } else null

            // 4) Resuelve qué dibujar
            loadProfileImage(photoView, media?.localUri, media?.route)
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
            val intent = Intent(this, DraftsActivity::class.java)
            startActivity(intent)
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
        val mediaDao = db.mediaDaoLocal()

        lifecycleScope.launch {
            try {
                // Borra el usuario local
                userDao.clearUsers()
                mediaDao.clearMedia()
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
    private fun loadProfileImage(view: ImageView, localUri: String?, route: String?) {
        when {
            // Caso A: tenemos URI local (content:// o file://)
            !localUri.isNullOrBlank() -> {
                try {
                    view.setImageURI(Uri.parse(localUri))
                    // Opcional: si tu URI puede requerir permisos y falla, podrías caer al else if
                } catch (_: Exception) {
                    // Si falló, intenta con route si existe
                    if (!route.isNullOrBlank()) {
                        Glide.with(view.context).load(route).into(view)
                    } else {
                        view.setImageResource(R.drawable.silkweb)
                    }
                }
            }
            // Caso B: no hay URI local pero sí URL remota
            !route.isNullOrBlank() -> {
                Glide.with(view.context)
                    .load(route)
                    .placeholder(R.drawable.silkweb)
                    .error(R.drawable.silkweb)
                    .into(view)
            }
            // Caso C: no hay nada guardado → placeholder
            else -> {
                view.setImageResource(R.drawable.silkweb)
            }
        }
    }
}