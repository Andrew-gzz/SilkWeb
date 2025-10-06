package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.silkweb.R
import com.example.silkweb.presentation.ui.LoginActivity

class MainActivity : ComponentActivity() {
    // Override the onCreate method to set the content view to the activity_main layout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)// Set the content view to the activity_main layout
        //setBienvenida()
        toLogin()
    }
//    // Create a function to set the bienvenida text
//    private fun setBienvenida(){
//        val textView = findViewById<TextView>(R.id.tv_bienvenida)
//        val button = findViewById<Button>(R.id.btn_toLogin)
//        textView.text = "Bienvenido a el Main Activity"
//        button.text = "Ir a Login"
//    }
//    // Create a function to navigate to the LoginActivity
    public fun toLogin() {
        val btn = findViewById<Button>(R.id.btn_toLogin)
        btn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}