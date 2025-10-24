package com.example.silkweb.presentation.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class PostActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private val imageUris = mutableListOf<Uri>()
    private val PICK_IMAGES_CODE = 100
    private var dots = arrayListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        viewPager = findViewById(R.id.viewPagerImages)
        dotsLayout = findViewById(R.id.llDots)

        val btnAddImage = findViewById<MaterialButton>(R.id.id_btnImage)
        val btnClose = findViewById<ImageButton>(R.id.id_back)

        btnAddImage.setOnClickListener { openGallery() }
        btnClose.setOnClickListener { finish() }
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
}
