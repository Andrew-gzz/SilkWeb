package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
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
import com.example.silkweb.api.ApiClient
import com.example.silkweb.data.dao.DataValidator
import com.example.silkweb.data.dao.ImageDao
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.local.MediaEntity
import com.example.silkweb.data.local.UserEntity
import com.example.silkweb.utils.showCustomSnackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
            showCustomSnackbar("Por favor llena todos los campos")
            return
        }
        Thread {
            try {
                Thread {
                    try {
                        val response = ApiClient.login(username, password)
                        if (response == null) {
                            runOnUiThread {
                                showCustomSnackbar("Error al conectar con el servidor")
                            }
                            return@Thread
                        }

                        // Convertimos la respuesta JSON a objeto
                        val json = JSONObject(response)

                        if (!json.getBoolean("success")) {
                            runOnUiThread {
                                showCustomSnackbar(json.getString("message"))
                            }
                            return@Thread
                        }

                        val userJson = json.getJSONObject("user")
                        //Esto es para validar si tiene foto o no
                        val idPhoto = if (userJson.isNull("id_photo")) null else userJson.getInt("id_photo")

                        val userData = UserLogin(
                            idPhoto,
                            userJson.getString("name"),
                            userJson.getString("lastname"),
                            userJson.getString("username"),
                            userJson.getString("email"),
                            password,
                            userJson.getString("phone"),
                            userJson.getString("direction")
                        )

                        saveUserLocal(userData)

                        // obtener foto de perfil
                        val responsePhoto = ApiClient.getProfilePhoto(username)

                        if (responsePhoto != null) {
                            val jsonPhoto = JSONObject(responsePhoto)
                            if (jsonPhoto.getBoolean("success")) {

                                val photoObj = jsonPhoto.getJSONObject("photo")

                                val base64 = photoObj.getString("file")
                                val fileBytes = Base64.decode(base64, Base64.DEFAULT)
                                val fileName = photoObj.getString("fileName")

                                // Guardar archivo local
                                val file = File(this.filesDir, fileName)
                                file.writeBytes(fileBytes)

                                // Guardar en Room
                                val db = AppDatabase.getDatabase(this)
                                val mediaDao = db.mediaDaoLocal()

                                val mediaEntity = MediaEntity(
                                    id = userData.idPhoto ?: 0,
                                    fileName = fileName,
                                    route = file.absolutePath,
                                    localUri = file.toURI().toString()
                                )

                                runBlocking {
                                    mediaDao.insert(mediaEntity)
                                }
                            }
                        }

                        runOnUiThread {
                            showCustomSnackbar("Inicio de sesión exitoso")
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }

                    } catch (e: Exception) {
                        runOnUiThread {
                            showCustomSnackbar("Error: ${e.message}")
                        }
                    }
                }.start()

            } catch (e: Exception) {
                runOnUiThread {
                    showCustomSnackbar("Error 500: ${e.message}")
                }
                e.printStackTrace()
            }
        }.start()
    }

    private fun registerUsers() {
        val fields = validateFields()

        if (fields != null) {
            Thread {
                try {
                    val response = ApiClient.register(fields)

                    if (response == null) {
                        runOnUiThread {
                            showCustomSnackbar("Error de servidor")
                        }
                        return@Thread
                    }

                    val json = JSONObject(response)

                    if (!json.getBoolean("success")) {
                        runOnUiThread {
                            showCustomSnackbar(json.getString("message"))
                        }
                        return@Thread
                    }

                    runOnUiThread {
                        showCustomSnackbar("Registro exitoso")
                        setVisibility() // Regresar a login
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        showCustomSnackbar("Error: ${e.message}")
                    }
                }
            }.start()

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
            showCustomSnackbar(e.message.toString())
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

