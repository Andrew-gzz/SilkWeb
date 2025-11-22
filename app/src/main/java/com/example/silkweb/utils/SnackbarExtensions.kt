package com.example.silkweb.utils

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.silkweb.R
import com.google.android.material.snackbar.Snackbar

fun Fragment.showCustomSnackbar(message: String, icon: Int = R.drawable.ic_error) {
    val snackbar = Snackbar.make(requireView(), "", Snackbar.LENGTH_SHORT)

    val customView = layoutInflater.inflate(R.layout.custom_snackbar, null)

    customView.findViewById<TextView>(R.id.snackbar_text).text = message
    customView.findViewById<ImageView>(R.id.snackbar_icon).setImageResource(icon)

    val snackbarLayout = snackbar.view as ViewGroup
    snackbarLayout.setBackgroundColor(Color.TRANSPARENT)
    snackbarLayout.background = null
    snackbarLayout.setPadding(0, 0, 0, 0)
    snackbarLayout.removeAllViews()
    snackbarLayout.addView(customView)

    snackbar.show()
}
fun AppCompatActivity.showCustomSnackbar(message: String, icon: Int = R.drawable.ic_error) {
    val root = findViewById<View>(android.R.id.content)
    val snackbar = Snackbar.make(root, "", Snackbar.LENGTH_SHORT)

    val customView = layoutInflater.inflate(R.layout.custom_snackbar, null)

    customView.findViewById<TextView>(R.id.snackbar_text).text = message
    customView.findViewById<ImageView>(R.id.snackbar_icon).setImageResource(icon)

    val layout = snackbar.view as ViewGroup
    layout.setBackgroundColor(Color.TRANSPARENT)
    layout.background = null
    layout.setPadding(0, 0, 0, 0)
    layout.removeAllViews()
    layout.addView(customView)

    snackbar.show()
}