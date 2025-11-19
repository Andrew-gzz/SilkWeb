package com.example.silkweb.presentation.adapter

import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R

class ImagePagerAdapter(
    private val imageUris: List<Uri>? = null,         //Viejo: Para las fotos de perfil
    private val bitmapList: List<Bitmap>? = null        //Nuevo: Para las fotos de posts
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivCarouselImage)
    }
    //Lista de imagenes del post
    constructor(bitmaps: List<Bitmap>) : this(
        imageUris = null,
        bitmapList = bitmaps
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        // SI VIENEN BITMAPS (BLOB → Bitmap)
        if (bitmapList != null) {
            holder.imageView.setImageBitmap(bitmapList[position])
            return
        }

        // SI VIENEN URIS
        if (imageUris != null) {
            holder.imageView.setImageURI(imageUris[position])
            return
        }
    }

    override fun getItemCount(): Int {
        return bitmapList?.size ?: imageUris?.size ?: 0
    }
}
