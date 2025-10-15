package com.example.silkweb.presentation.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.silkweb.R

class ConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        buttons()
    }
    private fun buttons(){
        val btnback = findViewById<ImageButton>(R.id.id_backToProfile)
        val btnMod = findViewById<Button>(R.id.id_btnModify)
        val btnImg = findViewById<ImageButton>(R.id.id_ivProfileImage)

        btnback.setOnClickListener {
            Log.i("ConfigActivity", "✅ Botón encontrado, asignando listener")
            finish()
        }
        btnMod.setOnClickListener {

        }
        btnImg.setOnClickListener {
            modifyData()
        }
    }
    private fun modifyData(){

    }
}