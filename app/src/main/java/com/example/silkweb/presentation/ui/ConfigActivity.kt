package com.example.silkweb.presentation.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.silkweb.R
import com.example.silkweb.data.dao.DataValidator
import com.example.silkweb.data.local.AppDatabase
import kotlinx.coroutines.launch

class ConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        setUserData()
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
            modifyData()
        }
        btnImg.setOnClickListener {

        }
    }
    private fun modifyData(){
        provisionalname()
    }
    private fun setUserData(){
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDaoLocal()

        lifecycleScope.launch {
            val user = userDao.getUser()
            runOnUiThread {
                if (user != null) {
                    val usernameView = findViewById<TextView>(R.id.id_tvUsername)
                    usernameView.text = user.username
                    val nombre = findViewById<EditText>(R.id.id_etName)
                    nombre.hint = user.name
                    val apellido = findViewById<EditText>(R.id.id_etLastName)
                    apellido.hint = user.lastName
                    val correo = findViewById<EditText>(R.id.id_etMail)
                    correo.hint = user.email
                    val telefono = findViewById<EditText>(R.id.id_etPhone)
                    telefono.hint = user.phone
                    val direccion = findViewById<EditText>(R.id.id_etDirection)
                    direccion.hint = user.direction
                    val usuario = findViewById<EditText>(R.id.id_EtUser)
                    usuario.hint = user.username
                    val contrasena = findViewById<EditText>(R.id.id_etPassword)
                    contrasena.hint = user.password
                } else {
                    val usernameView = findViewById<TextView>(R.id.id_tvUsername)
                    usernameView.text = "user101_"
                }
            }
        }
    }

    data class UserModify(
        val idPhoto: ImageButton?,
        val name: EditText?,
        val lastName: EditText?,
        val username: EditText,
        val email: EditText,
        val password: EditText,
        val phone: EditText?,
        val direction: EditText?
    )
    private fun provisionalname(){
        var modUser: UserModify = UserModify(
            idPhoto = findViewById<ImageButton>(R.id.id_ivProfileImage),
            name = findViewById<EditText>(R.id.id_etName),
            lastName = findViewById<EditText>(R.id.id_etLastName),
            email = findViewById<EditText>(R.id.id_etMail),
            phone = findViewById<EditText>(R.id.id_etPhone),
            direction = findViewById<EditText>(R.id.id_etDirection),
            username = findViewById<EditText>(R.id.id_EtUser),
            password = findViewById<EditText>(R.id.id_etPassword)
        )
        Toast.makeText(this, validate(modUser), Toast.LENGTH_LONG).show()

    }
    private fun validate(user: UserModify): String? {
        var msj: String? = "No hubo ninguna excepción"
        when{
            !user.name?.text.isNullOrEmpty() && !user.lastName?.text.isNullOrEmpty()->{
                msj = DataValidator.idDuplicateFullname(user.name.text.toString(),user.lastName.text.toString())
            }
            !user.name?.text.isNullOrEmpty() && user.lastName?.text.isNullOrEmpty()->{
                msj = DataValidator.idDuplicateFullname(user.name.text.toString(),user.lastName?.hint.toString())
            }
            user.name?.text.isNullOrEmpty() && !user.lastName?.text.isNullOrEmpty()->{
                msj = DataValidator.idDuplicateFullname(user.name?.hint.toString(),user.lastName.text.toString())
            }
            !user.email.text.isNullOrEmpty()-> msj = DataValidator.isDuplicateEmail(user.email.text.toString())
            !user.phone?.text.isNullOrEmpty()-> msj = DataValidator.validatePhone(user.phone.text.toString())
            !user.username.text.isNullOrEmpty()-> msj = DataValidator.isDuplicateUser(user.username.text.toString())
            !user.password.text.isNullOrEmpty()-> msj = DataValidator.validatePassword(user.password.text.toString())
        }
        return msj
    }
}

/*
    Cosas que se tiene que hacer aqui, rellenar datos desde los datos locales (listo)
    Al confirmar hacer el update a la base de datos con todos los datos nuevos
    *El update at se hara directamente en MySQL*

*/