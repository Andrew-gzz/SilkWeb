package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.silkweb.R
import com.example.silkweb.data.dao.UserDao
import android.util.Patterns
import com.example.silkweb.data.model.UserRegister
//For local storage
import com.example.silkweb.data.model.UserLogin
import androidx.lifecycle.lifecycleScope
import com.example.silkweb.data.dao.DataValidator
import com.example.silkweb.data.dao.ImageDao
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.local.MediaEntity
import com.example.silkweb.data.local.UserEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class LoginActivity : AppCompatActivity() {

    enum class ViewState {
        loginState,
        registerState
    }
    lateinit var state : ViewState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Referencias a los campos

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnNoAccount = findViewById<Button>(R.id.id_btnNoAccount)
        state = ViewState.loginState

        btnNoAccount.setOnClickListener {
            setVisibility()
        }
        btnLogin.setOnClickListener {
            if(state == ViewState.loginState){
                loginUsers()
            }else if(state == ViewState.registerState){
                registerUsers()
            }
        }
    }

    private fun setVisibility() { //"_" indicates is a method
        val Title = findViewById<TextView>(R.id.id_tvTitle)
        val etName = findViewById<EditText>(R.id.id_etName)
        val etLastName = findViewById<EditText>(R.id.id_etLastName)
        val etMail = findViewById<EditText>(R.id.id_etMail)
        val etPhone = findViewById<EditText>(R.id.id_etPhone)
        val etDirection = findViewById<EditText>(R.id.id_etDirection)
        val btnNoAccount = findViewById<Button>(R.id.id_btnNoAccount)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        if (state == ViewState.loginState) {
            state = ViewState.registerState

            etName.visibility = View.VISIBLE
            etLastName.visibility = View.VISIBLE
            etMail.visibility = View.VISIBLE
            etPhone.visibility = View.VISIBLE
            etDirection.visibility = View.VISIBLE

            Title.text = "Registro"
            btnNoAccount.text = "¿Ya tienes una cuenta?"
            btnLogin.text = "Registrarse"
        }else{
            state = ViewState.loginState

            etName.visibility = View.GONE
            etLastName.visibility = View.GONE
            etMail.visibility = View.GONE
            etPhone.visibility = View.GONE
            etDirection.visibility = View.GONE

            Title.text = "Iniciar Sesión"
            btnNoAccount.text = "¿No tienes una cuenta?"
            btnLogin.text = "Entrar"
        }


    }

    private fun loginUsers() {
        val etUser = findViewById<EditText>(R.id.id_EtUser)   //  correo o username
        val etPassword = findViewById<EditText>(R.id.id_etPassword)

        val username = etUser.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            try {
                val user = UserDao.loginUserSP(username, password)

                if (user != "Login exitoso") {
                    runOnUiThread {
                        Toast.makeText(this, user, Toast.LENGTH_SHORT).show()
                    }
                }else{
                    val userData = UserDao.userData(username)
                    if (userData != null) {
                        saveUserLocal(userData)

                        // 🔹 Nuevo: obtener foto de perfil del SP
                        val mediaData = ImageDao.getPhotoDataByUsername(username)
                        if (mediaData != null) {
                            val db = AppDatabase.getDatabase(this)
                            val mediaDao = db.mediaDaoLocal()

                            // Guardar el BLOB como archivo local
                            val fileName = mediaData.fileName ?: "profile_temp.jpg"
                            val file = File(this.filesDir, fileName)
                            mediaData.file?.let { file.writeBytes(it) }

                            // Guardar en Room
                            val mediaEntity = MediaEntity(
                                id = userData.idPhoto?: 0,
                                fileName = fileName,
                                route = file.absolutePath,
                                localUri = file.toURI().toString()
                            )
                            runBlocking {
                                mediaDao.insert(mediaEntity)
                            }
                        }
                        runOnUiThread {
                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(
                                Intent(this, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error 500: ${e.message}", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }.start()
    }

    private fun registerUsers() {
        val fields = validateFields()
        // Aquí puedes llamar tu DAO:
        if (fields != null) {
            Thread{
                try {
                    val result = UserDao.registrar(fields)
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error 500: Fallo de conexión con la BD\n${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
            setVisibility()
        }
    }

    private fun validateFields(): UserRegister? {

        val photo:Int? = null
        val name = findViewById<EditText>(R.id.id_etName).text.toString().trim()
        val lastName = findViewById<EditText>(R.id.id_etLastName).text.toString().trim()
        val username = findViewById<EditText>(R.id.id_EtUser).text.toString().trim()
        val email = findViewById<EditText>(R.id.id_etMail).text.toString().trim()
        val password = findViewById<EditText>(R.id.id_etPassword).text.toString().trim()
        var phone: String? = findViewById<EditText>(R.id.id_etPhone).text.toString().trim()
        var direction: String? = findViewById<EditText>(R.id.id_etDirection).text.toString().trim()
        try{
            val fullname = DataValidator.idDuplicateFullname(name, lastName)
            if (fullname != "Nombre y apellido disponibles") throw Exception(fullname)
            //Email
            val emailCheck = DataValidator.isDuplicateEmail(email)
            if (emailCheck != "El correo está disponible") throw Exception(emailCheck)
            // Teléfono (opcional)
            if (!phone.isNullOrEmpty()) {
                val phoneError = DataValidator.validatePhone(phone)
                if (phoneError != null) throw Exception(phoneError)
            } else {
                phone = null
            }
            // Dirección (opcional)
            if (direction.isNullOrEmpty()) direction = null
            //Usuario
            val userCheck = DataValidator.isDuplicateUser(username)
            if (userCheck != "El nombre de usuario está disponible")  throw Exception(userCheck)
            //Contraseña
            val passwordError = DataValidator.validatePassword(password)
            if (passwordError != null) throw Exception(passwordError)

            return UserRegister(
                photo,
                name,
                lastName,
                username,
                email,
                password,
                phone,
                direction,
            )
        }catch (e: Exception){
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            return null
        }

    }

    private fun saveUserLocal(userData: UserLogin) {
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDaoLocal()

        lifecycleScope.launch {
            val user = UserEntity(
                idPhoto = userData.idPhoto,
                name = userData.name,
                lastName = userData.lastName,
                username = userData.username,
                email = userData.email,
                password = userData.password,
                phone = userData.phone,
                direction = userData.direction
            )

            userDao.insertUser(user)

        }
    }
}

