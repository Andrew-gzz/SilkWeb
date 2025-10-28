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
        val btnClose = findViewById<ImageButton>(R.id.id_back)
        val btnPublish = findViewById<Button>(R.id.id_btnPost)

        btnAddImage.setOnClickListener { openGallery() }
        btnClose.setOnClickListener { finish() }
        btnPublish.setOnClickListener { publish() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, PICK_IMAGES_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            imageUris.clear()

            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    imageUris.add(data.clipData!!.getItemAt(i).uri)
                }
            } else if (data?.data != null) {
                imageUris.add(data.data!!)
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
}
/*
* -----------------Cosas por hacer----------------
* Obtener las imagenes adjuntadas asi como el titulo y body
* Mandar los datos a la funcion createPost del postController
* */