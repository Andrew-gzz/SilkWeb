package com.example.silkweb.presentation.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.silkweb.R
import com.example.silkweb.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logo = findViewById(R.id.id_ivLogoImage)

        iniciarAnimacionLogo()
        verificarSesion()
    }

    private fun iniciarAnimacionLogo() {
        val rotation = ObjectAnimator.ofFloat(logo, "rotation", 0f, 360f)
        rotation.duration = 3500
        rotation.repeatCount = ObjectAnimator.INFINITE
        rotation.interpolator = LinearInterpolator()
        rotation.start()
    }

    private fun verificarSesion() {
        lifecycleScope.launch(Dispatchers.IO) {

            // Simula un par de segundos de carga
            delay(3500)

            val db = AppDatabase.getDatabase(this@SplashActivity)
            val userDao = db.userDaoLocal()
            val user = userDao.getUser()

            withContext(Dispatchers.Main) {
                if (user == null) {
                    // No hay usuario → Login
                    val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    // Hay usuario → MainActivity
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                }
                finish()
            }
        }
    }
}
