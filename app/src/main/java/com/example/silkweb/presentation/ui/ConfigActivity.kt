package com.example.silkweb.presentation.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.silkweb.R
import com.example.silkweb.api.ApiClient
import com.example.silkweb.data.dao.DataValidator
import com.example.silkweb.data.dao.ImageDao
import com.example.silkweb.data.dao.UserDao
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.model.UserDataForUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
            finish()
        }
        btnMod.setOnClickListener {
            val success = modifyData()
            if(success) finish()
        }
        btnImg.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cambiar foto de perfil")
                .setMessage("Se guardara en automatico la nueva foto que selecciones, deseas proceder?")
                .setPositiveButton("Aceptar") { dialog, which ->
                    //Lógica para abrir la galería y seleccionar una imagen
                    guardarNuevaFoto()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
    //Setea los datos del usuario usando la base de datos local
    private fun setUserData(){
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDaoLocal()

        lifecycleScope.launch {
            val user = userDao.getUser()
            val mediaDao = db.mediaDaoLocal()

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
                val photoView = findViewById<ImageView>(R.id.id_ivProfileImage)
                // 3) Si hay id de foto, busca media
                val media = if (user?.idPhoto != null) {
                    withContext(Dispatchers.IO) { mediaDao.getMediaById(user.idPhoto!!) }
                } else null

                // 4) Resuelve qué dibujar
                loadProfileImage(photoView, media?.localUri, media?.route)
            } else {
                val usernameView = findViewById<TextView>(R.id.id_tvUsername)
                usernameView.text = "user101_"
            }

        }
    }

    //----------------------------
    //Sección la para modificar los datos
    //----------------------------
    private data class UserModify(
        val idPhoto: ImageButton?,
        val username: EditText,
        val name: EditText?,
        val lastName: EditText?,
        val email: EditText,
        val phone: EditText?,
        val direction: EditText?,
        val password: EditText,
        val newUser: EditText
    )

    private fun modifyData(): Boolean {
        val modUser = UserModify(
            idPhoto = findViewById(R.id.id_ivProfileImage),
            name = findViewById(R.id.id_etName),
            lastName = findViewById(R.id.id_etLastName),
            email = findViewById(R.id.id_etMail),
            phone = findViewById(R.id.id_etPhone),
            direction = findViewById(R.id.id_etDirection),
            username = findViewById(R.id.id_EtUser),
            password = findViewById(R.id.id_etPassword),
            newUser = findViewById(R.id.id_EtUser)
        )

        val userForUpdate = validateFields(modUser)
        if (userForUpdate == null) return false

        var serverMessage = "Error al ejecutar el procedimiento"
        var success = false
        var newUsernameFromServer: String? = null

        // 👇 Igual que antes, pero ahora llamando a la API
        val thread = Thread {
            try {
                val response = ApiClient.updateUserData(userForUpdate)

                if (response == null) {
                    serverMessage = "Error de servidor"
                    return@Thread
                }

                val json = JSONObject(response)

                success = json.optBoolean("success", false)
                serverMessage = json.optString(
                    "message",
                    if (success) "Datos actualizados correctamente" else "Error al actualizar datos"
                )
                newUsernameFromServer = json.optString("newUsername", userForUpdate.username)

            } catch (e: Exception) {
                serverMessage = e.message ?: "Error al procesar la respuesta"
            }
        }

        thread.start()
        thread.join()

        if (!success) {
            Toast.makeText(this, serverMessage, Toast.LENGTH_SHORT).show()
            return false
        }

        // ✔ Si llegó aquí, la API dijo que todo OK → actualizamos Room
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@ConfigActivity)
            val userDao = db.userDaoLocal()
            val localUser = userDao.getUser()

            if (localUser != null) {
                userDao.updateUserData(
                    id = localUser.id,
                    name = userForUpdate.name,
                    lastName = userForUpdate.lastname,
                    username = newUsernameFromServer ?: userForUpdate.username,
                    email = userForUpdate.email,
                    password = userForUpdate.password,
                    phone = userForUpdate.phone,
                    direction = userForUpdate.direction
                )

                Toast.makeText(
                    this@ConfigActivity,
                    "Datos actualizados localmente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return true
    }


    private fun validateFields(user: UserModify): UserDataForUpdate? {
        var updatedUser: UserDataForUpdate? = null
        var msj: String? = "Todo correcto en validateFields"
        var x : Boolean = false
        var y : Boolean = false
        try {
            //Validaciones
            var nameValue:String
            //--------------Nombre Completo-------------//
            if (!user.name?.text.isNullOrEmpty()) {
                nameValue = user.name.text.toString()
            } else {
                nameValue = user.name?.hint.toString()
                x = true
            }
            var lastNameValue:String
            if (!user.lastName?.text.isNullOrEmpty()) {
                lastNameValue = user.lastName.text.toString()
            }else {
                lastNameValue = user.lastName?.hint.toString()
                y = true
            }
            if(x == false || y == false){
                msj = DataValidator.idDuplicateFullname(nameValue, lastNameValue)
                if (msj != "Nombre y apellido disponibles") throw Exception(msj)
            }
            //--------------Correo-------------//
            var emailValue: String = user.email.hint.toString()
            if(!user.email.text.isNullOrEmpty()) {
                throw Exception("No puedes modificar tu correo")
            }
            //--------------Teléfono-------------//
            var phoneValue : String? = user.phone?.hint.toString()
            if(!user.phone?.text.isNullOrEmpty()) {
                msj = DataValidator.validatePhone(user.phone.text.toString())
                if (msj != null) throw Exception(msj) else phoneValue =user.phone.text.toString()
            }
            //--------------Dirección-------------//
            var directionValue = user.direction?.hint.toString()
            if (!user.direction?.text.isNullOrEmpty()) {
                if(user.direction.text.length >= 255) throw Exception("La dirección no puede pasar de 255 caracteres")
                directionValue = user.direction.text.toString()
            }
            //--------------Usuario-------------//
            val usernameValue = user.username.hint.toString()
            var newUsernameValue: String? = user.username.hint.toString()
            if(!user.username.text.isNullOrEmpty()){
                msj = DataValidator.isDuplicateUser(user.username.text.toString())
                if (msj != "El nombre de usuario está disponible") throw Exception(msj) else newUsernameValue = user.username.text.toString()
            }
            //--------------Contraseña-------------//
            var passwordValue: String = user.password.hint.toString()
            if(!user.password.text.isNullOrEmpty()){
                msj = DataValidator.validatePassword(user.password.text.toString())
                if (msj != null) throw Exception(msj) else passwordValue = user.password.text.toString()
            }

            // Crear y retornar el objeto de actualización
            updatedUser = UserDataForUpdate(
                idPhoto = null,
                name = nameValue,
                lastname = lastNameValue,
                username = usernameValue,
                email = emailValue,
                password = passwordValue,
                phone = phoneValue,
                direction = directionValue,
                newUsername = newUsernameValue
            )

        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Error desconocido en la validación", Toast.LENGTH_SHORT).show()
        }

        return updatedUser
    }

    //----------------------------
    //Sección la para foto de perfil
    //----------------------------
     fun guardarNuevaFoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val imageUri = result.data!!.data ?: throw Exception("URI nulo o inválido")

                // Conservar permisos para usar la imagen después
                contentResolver.takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val imageButton = findViewById<ImageButton>(R.id.id_ivProfileImage)
                imageButton.setImageURI(imageUri)

                val username = findViewById<TextView>(R.id.id_tvUsername).text.toString()

                val inputStream = contentResolver.openInputStream(imageUri)
                    ?: throw Exception("No se pudo abrir el InputStream de la imagen")

                val imageBytes = inputStream.readBytes()
                inputStream.close()

                val fileName = imageUri.lastPathSegment ?: "profile_image.png"

                Thread {
                    val response = ApiClient.updateProfileImage(username, imageBytes, fileName)

                    if (response != null) {
                        val json = JSONObject(response)

                        if (json.getBoolean("success")) {
                            val newImageId = json.getInt("newImageId")

                            // --- Guardar local igual que antes ---
                            val db = AppDatabase.getDatabase(this)
                            val mediaDao = db.mediaDaoLocal()
                            val userDao = db.userDaoLocal()

                            lifecycleScope.launch(Dispatchers.IO) {

                                val mediaEntity = com.example.silkweb.data.local.MediaEntity(
                                    id = newImageId,
                                    idPost = null,
                                    fileName = fileName,
                                    route = null,
                                    localUri = imageUri.toString()
                                )

                                mediaDao.insert(mediaEntity)

                                val user = userDao.getUser()
                                if (user != null) {
                                    val oldPhotoId = user.idPhoto
                                    userDao.updateUserPhoto(user.id, newImageId)

                                    if (oldPhotoId != null && oldPhotoId != newImageId) {
                                        mediaDao.deleteMediaById(oldPhotoId)
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@ConfigActivity,
                                        "Foto actualizada correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    json.getString("message"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                }.start()
            } else {
                Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al seleccionar una foto: ${e.message}", Toast.LENGTH_LONG).show()
        }
        debugMostrarMedia()
    }
    private fun debugMostrarMedia() {
        val db = AppDatabase.getDatabase(this)
        val mediaDao = db.mediaDaoLocal()

        lifecycleScope.launch {
            val lista = mediaDao.getAllMedia()
            if (lista.isEmpty()) {
                Log.d("ROOM_MEDIA", "📭 No hay registros en la tabla media")
            } else {
                for (m in lista) {
                    Log.d(
                        "ROOM_MEDIA",
                        "🖼 ID: ${m.id} | fileName: ${m.fileName} | route: ${m.route} | localUri: ${m.localUri}"
                    )
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
