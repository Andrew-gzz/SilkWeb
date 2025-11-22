package com.example.silkweb.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.silkweb.R
import com.example.silkweb.presentation.adapter.ImagePagerAdapter
import com.google.android.material.button.MaterialButton
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.silkweb.data.controller.postController
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.model.CreatePostModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import android.provider.OpenableColumns
import android.view.View
import com.example.silkweb.data.local.MediaEntity
import com.example.silkweb.data.local.PostEntity
import com.example.silkweb.utils.ConnectionUtils
import com.example.silkweb.utils.showCustomSnackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private val imageUris = mutableListOf<Uri>()
    private val PICK_IMAGES_CODE = 100
    private var dots = arrayListOf<ImageView>()
    private var currentDraftId: Int? = null

    //Para tomar fotos
    private lateinit var btnAddPhoto: MaterialButton

    //Para tomar fotos
    private val CAMERA_REQUEST_CODE = 200
    private var cameraImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        // Rellenar información basica Username y foto de perfil //
        setData()

        // Borradores //
        val draftId = intent.getIntExtra("draft_id", -1)
        if (draftId != -1) {
            currentDraftId = draftId
            cargarDraft(draftId)
        }


        viewPager = findViewById(R.id.viewPagerImages)
        dotsLayout = findViewById(R.id.llDots)

        val btnAddImage = findViewById<MaterialButton>(R.id.id_btnImage)
        btnAddImage.setOnClickListener { openGallery() }

        val btnPublish = findViewById<Button>(R.id.id_btnPost)
        btnPublish.setOnClickListener {  publish() }

        // Intercepta el botón "atrás" del sistema
        onBackPressedDispatcher.addCallback(this) {
            intentarGuardarBorradorYSalir()
        }

        val btnClose = findViewById<ImageButton>(R.id.id_back)
        btnClose.setOnClickListener { intentarGuardarBorradorYSalir() }

        btnAddPhoto = findViewById(R.id.id_btnAddPhoto)
        btnAddPhoto.setOnClickListener { openCamera() }

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        // flags para poder persistir permisos
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, PICK_IMAGES_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {

            PICK_IMAGES_CODE -> {   // GALERÍA
                imageUris.clear()

                if (data?.clipData != null) {
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        val uri = data.clipData!!.getItemAt(i).uri
                        contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        imageUris.add(uri)
                    }
                } else if (data?.data != null) {
                    val uri = data.data!!
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    imageUris.add(uri)
                }

                setupViewPager()
            }

            CAMERA_REQUEST_CODE -> {   // CÁMARA
                cameraImageUri?.let { uri ->
                    imageUris.add(uri)
                    setupViewPager()
                }
            }
        }
    }

    private fun setupViewPager() {
        val cardView = findViewById<androidx.cardview.widget.CardView>(R.id.cardViewImages)
        val dotsLayout = findViewById<LinearLayout>(R.id.llDots)

        // Si no hay imágenes → OCULTAR
        if (imageUris.isEmpty()) {
            cardView.visibility = View.GONE
            dotsLayout.visibility = View.GONE
            return
        }

        // Si hay imágenes → MOSTRAR
        cardView.visibility = View.VISIBLE
        dotsLayout.visibility = View.VISIBLE

        viewPager.adapter = ImagePagerAdapter(imageUris)
        addDots(imageUris.size)
        selectDot(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectDot(position)
            }
        })
    }


    private fun addDots(count: Int) {
        dotsLayout.removeAllViews()
        dots.clear()

        for (i in 0 until count) {
            val dot = ImageView(this)
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_inactive))
            val params = LinearLayout.LayoutParams(20, 20)
            params.setMargins(8, 0, 8, 0)
            dotsLayout.addView(dot, params)
            dots.add(dot)
        }
    }

    private fun selectDot(position: Int) {
        for (i in dots.indices) {
            val drawableId = if (i == position) R.drawable.dot_active else R.drawable.dot_inactive
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, drawableId))
        }
    }

    //--------Rellenado de datos---------/
    private fun setData(){
        //Setear el nombre y la foto de perfil del usuario activo
        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDaoLocal()
        val mediaDao = db.mediaDaoLocal()
        val usernameView = findViewById<TextView>(R.id.id_tvTitle)
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

//-----------------------------------------------------
//              Logica al publicar el post
//-----------------------------------------------------
    private fun publish() {
        val etTitle = findViewById<EditText>(R.id.id_etTitle)
        val etBody  = findViewById<EditText>(R.id.id_etDescription)

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PostActivity)
            val userDao = db.userDaoLocal()
            val user = userDao.getUser()

            if (user == null) {
                withContext(Dispatchers.Main) {
                    showCustomSnackbar("No hay usuario local")
                }
                return@launch
            }

            val title = etTitle.text?.toString()?.trim().orEmpty()
            val body  = etBody.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                withContext(Dispatchers.Main) {
                    showCustomSnackbar("El título es obligatorio")
                }
                return@launch
            }

            // ----------------------------------------------------------------------
            //  MODO OFFLINE: si no hay internet, guardar como pendiente de publicar
            // ----------------------------------------------------------------------
            if (!ConnectionUtils.hayInternet(this@PostActivity)) {

                // Si venía de un borrador → ELIMINAR BORRADOR ANTES DE GUARDAR EL PENDIENTE
                if (currentDraftId != null) {

                    // eliminar borrador y sus imágenes
                    db.postDaoLocal().deletePostById(currentDraftId!!)
                    db.mediaDaoLocal().deleteMediaByPostId(currentDraftId!!)
                }

                // guardar la publicación como pendiente
                guardarComoPendiente(user.id, title, body)

                withContext(Dispatchers.Main) {
                    showCustomSnackbar("Guardado sin conexión. Se publicará cuando haya internet.")
                    finish()
                }
                return@launch
            }

            // ----------------------------------------------------------------------
            //  PUBLICACIÓN NORMAL (con internet)
            // ----------------------------------------------------------------------
            val model = CreatePostModel(
                username = user.username,
                title = title,
                body = body,
                status = 1
            )

            try {
                val newId = postController.createPost(this@PostActivity, model, imageUris)

                withContext(Dispatchers.Main) {
                    if (newId > 0) {

                        // Si venía de un borrador → eliminarlo completamente
                        if (currentDraftId != null) {
                            eliminarBorradorDirecto(currentDraftId!!)
                        }

                        showCustomSnackbar("Publicado (id=$newId)")
                        finish()

                    } else {
                        showCustomSnackbar("No se pudo publicar")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showCustomSnackbar("Error: ${e.message}")
                }
            }
        }
    }

    private fun eliminarBorradorDirecto(id: Int) { //Elimina el borrador una vez se publica
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PostActivity)

            // eliminar post local
            db.postDaoLocal().deletePostById(id)

            // eliminar imágenes locales del borrador
            db.mediaDaoLocal().deleteMediaByPostId(id)
        }
    }

//-----------------------------------------------------
//         Logica para guardar borrador local
//-----------------------------------------------------
    private fun hayContenidoParaBorrador(): Boolean {
        val etTitle = findViewById<EditText>(R.id.id_etTitle)
        val etBody  = findViewById<EditText>(R.id.id_etDescription)
        val titleHasText = !etTitle.text.isNullOrBlank()
        val bodyHasText  = !etBody.text.isNullOrBlank()
        val hasImages    = imageUris.isNotEmpty()
        return titleHasText || bodyHasText || hasImages
    }

    private fun intentarGuardarBorradorYSalir() {
        // Si no hay contenido → salir directo
        if (!hayContenidoParaBorrador()) {
            finish()
            return
        }

        // Si NO es un borrador (post nuevo)
        if (currentDraftId == null) {
            AlertDialog.Builder(this)
                .setTitle("Guardar como borrador")
                .setMessage("Tienes contenido sin publicar. ¿Deseas guardarlo como borrador?")
                .setPositiveButton("Guardar") { _, _ -> guardarBorrador() }
                .setNegativeButton("Descartar") { _, _ -> finish() }
                .setNeutralButton("Cancelar", null)
                .show()
            return
        }

        // SI ES un borrador cargado
        AlertDialog.Builder(this)
            .setTitle("Borrador existente")
            .setMessage("Este es un borrador guardado. ¿Qué deseas hacer?")
            .setPositiveButton("Guardar cambios") { _, _ ->
                actualizarBorrador(currentDraftId!!)
            }
            .setNegativeButton("Eliminar borrador") { _, _ ->
                eliminarBorrador(currentDraftId!!)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun guardarBorrador() {
        val etTitle = findViewById<EditText>(R.id.id_etTitle)
        val etBody  = findViewById<EditText>(R.id.id_etDescription)

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PostActivity)
            val user = db.userDaoLocal().getUser()

            if (user == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PostActivity, "No hay usuario local", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // fecha-hora como String (tu entidad guarda String)
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val draft = com.example.silkweb.data.local.PostEntity(
                id = 0,               // autogen
                idPost = null,        // no hay id de backend, es borrador local
                userId = user.id,     // tu PK local de usuario
                title  = etTitle.text?.toString()?.trim().orEmpty(),
                body   = etBody.text?.toString()?.trim().orEmpty(),
                isDraft = true,
                createdAt = now
            )

            // 1) Insertar post y obtener id local
            val draftId = db.postDaoLocal().insert(draft).toInt()

            // 2) Insertar imágenes asociadas al borrador
            val mediaDao = db.mediaDaoLocal()
            for (uri in imageUris) {
                val fileName = obtenerNombreArchivo(uri) ?: "image_${System.currentTimeMillis()}.jpg"
                mediaDao.insert(
                    com.example.silkweb.data.local.MediaEntity(
                        id = 0,                // autogen
                        idPost = draftId,      // vincula a este borrador
                        fileName = fileName,
                        route = null,
                        localUri = uri.toString()
                    )
                )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@PostActivity, "Borrador guardado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun actualizarBorrador(id: Int) {
        val etTitle = findViewById<EditText>(R.id.id_etTitle)
        val etBody  = findViewById<EditText>(R.id.id_etDescription)

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PostActivity)
            val user = db.userDaoLocal().getUser()
            val mediaDao = db.mediaDaoLocal()

            if (user == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PostActivity, "No hay usuario local", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // 1. Actualizar el borrador
            db.postDaoLocal().updatePostData(
                id = id,
                userId = user.id,
                title = etTitle.text?.toString()?.trim().orEmpty(),
                body = etBody.text?.toString()?.trim().orEmpty(),
                isDraft = true,
                createdAt = now
            )

            // 2. Guardar nuevas imágenes (se borran las anteriores)
            mediaDao.deleteMediaByPostId(id)

            for (uri in imageUris) {
                val fileName = obtenerNombreArchivo(uri) ?: "image_${System.currentTimeMillis()}.jpg"
                mediaDao.insert(
                    com.example.silkweb.data.local.MediaEntity(
                        id = 0,
                        idPost = id,
                        fileName = fileName,
                        route = null,
                        localUri = uri.toString()
                    )
                )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@PostActivity, "Borrador actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun eliminarBorrador(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PostActivity)

            // eliminar post
            db.postDaoLocal().deletePostById(id)

            // eliminar fotos
            db.mediaDaoLocal().deleteMediaByPostId(id)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@PostActivity, "Borrador eliminado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun obtenerNombreArchivo(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
        }
        return name
    }
    private fun cargarDraft(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PostActivity)

            val draft = db.postDaoLocal().getDraftById(id)
            val mediaDao = db.mediaDaoLocal()

            if (draft != null) {
                // 1. Obtener las imágenes del borrador
                val mediaList = mediaDao.getAllMedia().filter { it.idPost == draft.id }

                // 2. Convertimos esas rutas (localUri) a Uri
                val uris = mediaList.mapNotNull { it.localUri?.let { uri -> Uri.parse(uri) } }

                withContext(Dispatchers.Main) {
                    // 3. Cargar título y cuerpo
                    findViewById<EditText>(R.id.id_etTitle).setText(draft.title)
                    findViewById<EditText>(R.id.id_etDescription).setText(draft.body)

                    // 4. Cargar imágenes
                    imageUris.clear()
                    imageUris.addAll(uris)

                    if (uris.isNotEmpty()) {
                        setupViewPager()
                    }
                }
            }
        }
    }

    private fun hayInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    private fun guardarComoPendiente(userId: Int, title: String, body: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PostActivity)
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val pendingPost = PostEntity(
                id = 0,
                idPost = null,
                userId = userId,
                title = title,
                body = body,
                isDraft = false,
                createdAt = now,
                isPendingPublish = true
            )

            val postId = db.postDaoLocal().insert(pendingPost).toInt()

            val mediaDao = db.mediaDaoLocal()
            for (uri in imageUris) {
                val fileName = obtenerNombreArchivo(uri) ?: "image_${System.currentTimeMillis()}.jpg"
                mediaDao.insert(
                    MediaEntity(
                        id = 0,
                        idPost = postId,
                        fileName = fileName,
                        route = null,
                        localUri = uri.toString()
                    )
                )
            }
        }
    }
    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)

        // Crear archivo temporal para guardar la foto
        val photoFile = kotlin.io.path.createTempFile(prefix = "photo_", suffix = ".jpg").toFile()

        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

}
