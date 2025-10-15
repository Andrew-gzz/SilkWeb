package com.example.silkweb.presentation.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.silkweb.R
import com.google.android.material.button.MaterialButton

class PostActivity : AppCompatActivity() {

    private lateinit var imageContainer: LinearLayout
    private val PICK_IMAGES_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        imageContainer = findViewById(R.id.id_llImages)

        val btnAddImage = findViewById<MaterialButton>(R.id.id_btnImage)
        btnAddImage.setOnClickListener {
            openGallery()
        }
        val btnClose = findViewById<ImageView>(R.id.id_back)
        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Permite seleccionar varias imágenes
        startActivityForResult(intent, PICK_IMAGES_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            if (data?.clipData != null) {
                // Seleccionó varias imágenes
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri: Uri = data.clipData!!.getItemAt(i).uri
                    addImageView(imageUri)
                }
            } else if (data?.data != null) {
                // Solo una imagen
                val imageUri: Uri = data.data!!
                addImageView(imageUri)
            }
        }
    }

    private fun addImageView(imageUri: Uri) {
        val imageView = ImageView(this)
        imageView.setImageURI(imageUri)
        val params = LinearLayout.LayoutParams(200, 200)
        params.setMargins(10, 0, 10, 0)
        imageView.layoutParams = params
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        imageContainer.addView(imageView)
        imageView.setOnClickListener {
            imageContainer.removeView(imageView)
        }

    }
}
