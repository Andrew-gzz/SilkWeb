package com.example.silkweb.presentation.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.silkweb.R
import com.example.silkweb.data.controller.postController
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.presentation.adapter.ImagePagerAdapter
import com.example.silkweb.utils.ConnectionUtils
import com.example.silkweb.utils.showCustomSnackbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModifyActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private val imageUris = mutableListOf<Uri>()
    private val PICK_IMAGES_CODE = 100
    private var dots = arrayListOf<ImageView>()

    private var postId = -1

    private lateinit var tvUsername: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnModify: Button
    private lateinit var btnAddImage: MaterialButton
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var btnEliminate: MaterialButton

    //Para tomar fotos
    private val CAMERA_REQUEST_CODE = 200
    private var cameraImageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify)

        postId = intent.getIntExtra("post_id", -1)
        if (postId == -1) finish()

        initViews()
        cargarInfoPost()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPagerImages)
        dotsLayout = findViewById(R.id.llDots)

        tvUsername = findViewById(R.id.id_tvTitle)  //Nombre de usuario
        ivProfile = findViewById(R.id.id_ivProfileImage)
        etTitle = findViewById(R.id.id_etTitle) //Titulo
        etDescription = findViewById(R.id.id_etDescription) //Descripcion

        btnBack = findViewById(R.id.id_back)
        btnBack.setOnClickListener { finish() }

        btnAddImage = findViewById(R.id.id_btnImage)
        btnAddImage.setOnClickListener { openGallery() }

        btnModify = findViewById(R.id.id_btnPost)
        btnModify.setOnClickListener {  modify() }

        btnAddPhoto = findViewById(R.id.id_btnAddPhoto)
        btnAddPhoto.setOnClickListener { openCamera() }

        /*DANGER BOTON PARA ELIMINAR*/
        btnEliminate = findViewById(R.id.id_btnDestroy)
        btnEliminate.setOnClickListener { eliminate() }
    }

    // ----------------------------------------------------
    //      Cargar datos
    // ----------------------------------------------------
    private fun cargarInfoPost() {
        lifecycleScope.launch(Dispatchers.IO) {
            val post = postController.getPostDetails(postId)
            if (post == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ModifyActivity, "Error cargando post", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Cargar imágenes del post
            val mediaMap = postController.getPostMedia(postId)
            val tempUris = mediaMap.map { (_, bytes) ->
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val uri = saveTempFile(bmp)
                uri
            }

            withContext(Dispatchers.Main) {

                // Usuario
                tvUsername.text = post.username

                // Cargar foto de perfil
                if (post.userPhotoFile != null) {
                    val bmp = BitmapFactory.decodeByteArray(post.userPhotoFile, 0, post.userPhotoFile!!.size)
                    ivProfile.setImageBitmap(bmp)
                } else {
                    ivProfile.setImageResource(R.drawable.silkweb)
                }

                // Título y descripción
                etTitle.setText(post.title)
                etDescription.setText(post.body)

                // Fotos
                imageUris.clear()
                imageUris.addAll(tempUris)
                setupViewPager()
            }
        }
    }

    private fun setupViewPager() {
        val cardView = findViewById<androidx.cardview.widget.CardView>(R.id.cardViewImages)
        val dotsLayout = findViewById<LinearLayout>(R.id.llDots)

        if (imageUris.isEmpty()) {
            // No hay imágenes → ocultar
            cardView.visibility = View.GONE
            dotsLayout.visibility = View.GONE
            return
        }

        // SI hay imágenes → mostrar
        cardView.visibility = View.VISIBLE
        dotsLayout.visibility = View.VISIBLE

        // Configurar ViewPager
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
            dot.setImageDrawable(resources.getDrawable(R.drawable.dot_inactive, null))
            val params = LinearLayout.LayoutParams(20, 20)
            params.setMargins(8, 0, 8, 0)
            dotsLayout.addView(dot, params)
            dots.add(dot)
        }
    }

    private fun selectDot(position: Int) {
        for (i in dots.indices) {
            val drawableId = if (i == position) R.drawable.dot_active else R.drawable.dot_inactive
            dots[i].setImageDrawable(resources.getDrawable(drawableId, null))
        }
    }

    // Guardar imagen en cache para crear Uri
    private fun saveTempFile(bitmap: android.graphics.Bitmap): Uri {
        val file = kotlin.io.path.createTempFile().toFile()
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file)
    }

    // ----------------------------------------------------
    //     Relacionado con Galeria y las Tomar fotos
    // ----------------------------------------------------
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


    // ----------------------------------------------------
    //     Logica para Modificar la publicación
    // ----------------------------------------------------
    private fun modify() {
        // VALIDAR INTERNET
        if (!ConnectionUtils.hayInternet(this)) {
            runOnUiThread {
                showCustomSnackbar("No hay conexión a internet")
            }
            return
        }

        val title = etTitle.text.toString().trim()
        val body = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            runOnUiThread { etTitle.error = "El título no puede estar vacío" }
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {

            // 1) Construir media_json usando tu controlador oficial
            val mediaJson = postController.buildMediaJson(this@ModifyActivity, imageUris)
                ?: "[]" // si no hay imágenes

            // 2) Obtener usuario local
            val db = AppDatabase.getDatabase(this@ModifyActivity)
            val user = db.userDaoLocal().getUser()
            val username = user?.username ?: ""

            // 3) Llamar a backend
            val success = postController.updatePost(
                postId = postId,
                username = username,
                title = title,
                body = body,
                mediaJson = mediaJson
            )

            // 4) Respuesta UI
            withContext(Dispatchers.Main) {
                if (success) {
                    showCustomSnackbar("Publicación modificada correctamente")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    showCustomSnackbar("Error al modificar la publicación")
                }
            }
        }
    }

    // ----------------------------------------------------
    //     Logica para ELIMINAR la publicación
    // ----------------------------------------------------
    private fun eliminate() {
        // VALIDAR INTERNET
        if (!ConnectionUtils.hayInternet(this)) {
            showCustomSnackbar("No hay conexión a internet")
            return
        }

        // Preguntar si está seguro
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar publicación")
            .setMessage(
                "¿Estás seguro de que deseas eliminar esta publicación?\n\n" +
                        "Esta acción eliminará también todos sus comentarios e interacciones " +
                        "y no se puede deshacer."
            )
            .setPositiveButton("Eliminar") { _, _ ->
                // Si confirma, ahora sí eliminamos
                lifecycleScope.launch(Dispatchers.IO) {
                    val success = postController.deletePost(postId = postId)

                    withContext(Dispatchers.Main) {
                        if (success) {
                            showCustomSnackbar("Publicación eliminada")
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            showCustomSnackbar("Error al eliminar la publicación")
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

}

