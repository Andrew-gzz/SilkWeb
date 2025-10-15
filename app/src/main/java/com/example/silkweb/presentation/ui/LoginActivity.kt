package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
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
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.local.UserEntity
import kotlinx.coroutines.launch

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
        val etUser = findViewById<EditText>(R.id.id_EtUser)   // 👈 tu id correcto
        val etPassword = findViewById<EditText>(R.id.id_etPassword)

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
                        }
                    }
                    if (user != null) {
                        val userData = UserDao.userData(username)
                        if (userData != null) {
                            saveUserLocal(userData)
                            lifecycleScope.launch {
                                val db = AppDatabase.getDatabase(this@LoginActivity)
                                val userLocal = db.userDaoLocal().getUser()
                                if (userLocal != null) {
                                    runOnUiThread {
                                        Toast.makeText(this@LoginActivity, "Usuario local: ${userLocal.username}", Toast.LENGTH_SHORT).show()
                                    }
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }

                        }else throw Exception("Error al guardar los datos del usuario de manera local")
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
            //Validar Nombre
            if(name.isEmpty()) throw Exception("Nombre vacío")
            //Validar Apellido
            if(lastName.isEmpty()) throw Exception("Apellido vacío")
            //Email
            val emailCheck = isDuplicateEmail(email)
            if (emailCheck != null) throw Exception(emailCheck)
            // Teléfono (opcional)
            if (!phone.isNullOrEmpty()) {
                val phoneError = validatePhone(phone)
                if (phoneError != null) throw Exception(phoneError)
            } else {
                phone = null
            }
            // Dirección (opcional)
            if (direction.isNullOrEmpty()) direction = null
            //Usuario
            val userCheck = isDuplicateUser(username)
            if (userCheck != null)  throw Exception(userCheck)
            //Contraseña
            val passwordError = validatePassword(password)
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

    private fun validatePhone(phone: String): String? {
        val cleanPhone = phone.replace("\\s|-".toRegex(), "")

        return when {
            cleanPhone.isEmpty() -> null
            !cleanPhone.matches(Regex("^\\+?[0-9]{8,15}$")) ->
                "El número de teléfono debe contener entre 8 y 15 dígitos, y solo puede incluir '+' al inicio"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        val pass = password.trim()
        return when{
            pass.isEmpty() -> "La contraseña no puede estar vacía"
            pass.length < 10 -> "Debe tener al menos 10 caracteres"
            !pass.any { it.isUpperCase() } -> "Debe contener al menos una letra mayúscula"
            !pass.any { it.isLowerCase() } -> "Debe contener al menos una letra minúscula"
            !pass.any { it.isDigit() } -> "Debe contener al menos un número"
            else->null
        }
    }
    
    //Validacion con base de datos
    private fun isDuplicateUser(username: String): String? {
        var msj: String? = null
        val userTrimmed = username.trim()

        // Validaciones básicas
        when {
            userTrimmed.isEmpty() -> return "El nombre de usuario no puede estar vacío"
            userTrimmed.length < 3 -> return "Debe tener al menos 3 caracteres"
            userTrimmed.length > 20 -> return "No puede tener más de 20 caracteres"
            userTrimmed.contains(" ") -> return "No puede contener espacios"
        }

        // Validación en segundo plano
        val thread = Thread {
            try {
                val user = UserDao.checkUserExists(username)
                if (user != null) {
                    msj = "El nombre de usuario no está disponible"
                }
            } catch (e: Exception) {
                msj = "Error de conexión: ${e.message}"
            }
        }

        thread.start()
        thread.join() // Espera a que termine el hilo (⚠️ bloquea el hilo actual si es el UI thread)

        return msj
    }

    private fun isDuplicateEmail(email: String): String? {
        var msj: String? = null
        val emailTrimmed = email.trim()

        // Validaciones básicas
        when {
            !(email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches())-> return "El email no es valido"
        }

        // Validación en segundo plano
        val thread = Thread {
            try {
                val user = UserDao.checkEmailExists(emailTrimmed)
                if (user != null) {
                    msj = "El email no está disponible"
                }
            } catch (e: Exception) {
                msj = "Error de conexión: ${e.message}"
            }
        }

        thread.start()
        thread.join()

        return msj
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

