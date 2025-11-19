package com.example.silkweb.presentation.ui

import android.app.Activity
import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private val imageUris = mutableListOf<Uri>()
    private val PICK_IMAGES_CODE = 100
    private var dots = arrayListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        setData()

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

        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            imageUris.clear()

            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    // conservar permisos para usar el Uri en el borrador
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
    }

    private fun setupViewPager() {
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

    //--------Lógica para publicar--------------//
    private fun publish(){
        val etTitle = findViewById<EditText>(R.id.id_etTitle)
        val etBody  = findViewById<EditText>(R.id.id_etDescription)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PostActivity)
            val userDao = db.userDaoLocal()
            val user = userDao.getUser()

            if (user == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PostActivity, "No hay usuario local", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val title = etTitle.text?.toString()?.trim().orEmpty()
            val body  = etBody.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PostActivity, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

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
                        Toast.makeText(this@PostActivity, "Publicado (id=$newId)", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@PostActivity, "No se pudo publicar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PostActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    //-------Logica para guardar como borrador de manera local----------//
    private fun hayContenidoParaBorrador(): Boolean {
        val etTitle = findViewById<EditText>(R.id.id_etTitle)
        val etBody  = findViewById<EditText>(R.id.id_etDescription)
        val titleHasText = !etTitle.text.isNullOrBlank()
        val bodyHasText  = !etBody.text.isNullOrBlank()
        val hasImages    = imageUris.isNotEmpty()
        return titleHasText || bodyHasText || hasImages
    }

    private fun intentarGuardarBorradorYSalir() {
        if (!hayContenidoParaBorrador()) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Guardar como borrador")
            .setMessage("Tienes contenido sin publicar. ¿Deseas guardarlo como borrador?")
            .setPositiveButton("Guardar") { _, _ -> guardarBorrador() }
            .setNegativeButton("Descartar") { _, _ -> finish() }
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

    private fun obtenerNombreArchivo(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
        }
        return name
    }
}
