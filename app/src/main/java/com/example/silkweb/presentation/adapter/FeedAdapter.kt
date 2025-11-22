package com.example.silkweb.presentation.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.silkweb.R
import com.example.silkweb.data.model.PostModel
import org.json.JSONArray

class FeedAdapter(
    private val posts: List<PostModel>,
    private val onPostClick: (PostModel) -> Unit,
    private val onLikeClick: (PostModel) -> Unit,
    private val onCommentClick: (PostModel) -> Unit,
    private val onBookmarkClick: (PostModel) -> Unit
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView

        // Datos del usuario
        val profileImage: ImageView = itemView.findViewById(R.id.id_ivProfileImage)
        val username: TextView = itemView.findViewById(R.id.id_tvTitle)
        val date: TextView = itemView.findViewById(R.id.id_tvDate)

        // Contenido del post
        val title: TextView = itemView.findViewById(R.id.id_etTitle)
        val description: TextView = itemView.findViewById(R.id.id_etDescription)

        // Interacciones
        val likesCount: TextView = itemView.findViewById(R.id.id_tvFavoritesCount)
        val commentsCount: TextView = itemView.findViewById(R.id.id_tvCommentsCount)

        val btnFavorite: ImageButton = itemView.findViewById(R.id.id_btnFavorite)
        val btnComments: ImageButton = itemView.findViewById(R.id.id_btnComments)
        val btnBookMarks: ImageButton = itemView.findViewById(R.id.id_btnBookMarks)

        // Carrusel de imágenes
        val viewPager: ViewPager2 = itemView.findViewById(R.id.viewPagerImages)
        val dotsLayout: LinearLayout = itemView.findViewById(R.id.llDots)
        val cardImages: View = itemView.findViewById(R.id.cardViewImages)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // ───────────────────────────────
        // USUARIO
        // ───────────────────────────────
        holder.username.text = post.username

        // foto de perfil
        if (post.userPhotoFile != null) {
            val bmp = BitmapFactory.decodeByteArray(post.userPhotoFile, 0, post.userPhotoFile.size)
            holder.profileImage.setImageBitmap(bmp)
        } else {
            holder.profileImage.setImageResource(R.drawable.silkweb)
        }

        // ───────────────────────────────
        // CONTENIDO DEL POST
        // ───────────────────────────────
        holder.title.text = post.title
        holder.description.text = post.body
        holder.date.text = formatDate(post.createdAt)
        holder.likesCount.text = post.likeCount.toString()
        holder.commentsCount.text = post.commentCount.toString()

        // ───────────────────────────────
        // CARRUSEL: JSON → LISTA DE BITMAPS
        // ───────────────────────────────
        // Construir lista de imágenes solo si existen
        val imageList = mutableListOf<Bitmap>()

        try {
            if (!post.mediaListJson.isNullOrEmpty()) {
                val array = JSONArray(post.mediaListJson)

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val hasFile = item.optInt("has_file", 0) == 1

                    if (hasFile) {
                        val mediaId = item.optInt("media_id", -1)
                        val blob = post.mediaFiles?.get(mediaId)

                        if (blob != null) {
                            val bmp = BitmapFactory.decodeByteArray(blob, 0, blob.size)
                            imageList.add(bmp)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Por si viene un JSON corrupto
            e.printStackTrace()
        }



        // ───────────────────────────────
        // Mostrar u ocultar carrusel
        // ───────────────────────────────
        if (imageList.isNotEmpty()) {
            holder.cardImages.visibility = View.VISIBLE

            val imgAdapter = ImagePagerAdapter(imageList)
            holder.viewPager.adapter = imgAdapter

            setupDots(holder, imageList.size)
            updateDots(holder, 0)

            holder.viewPager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updateDots(holder, position)
                    }
                }
            )

        } else {
            // Limpia completamente el ViewPager
            holder.viewPager.adapter = null
            holder.dotsLayout.removeAllViews()
            holder.cardImages.visibility = View.GONE
        }


        // ───────────────────────────────
        // EVENTOS DE CLICK
        // ───────────────────────────────
// LIKE
        if (post.userLiked) {
            holder.btnFavorite.setColorFilter(
                holder.btnFavorite.context.getColor(R.color.secondary)
            )
        } else {
            holder.btnFavorite.setColorFilter(
                holder.btnFavorite.context.getColor(R.color.primaryLight)
            )
        }

// BOOKMARK
        if (post.userBookmarked) {
            holder.btnBookMarks.setColorFilter(
                holder.btnBookMarks.context.getColor(R.color.primary)
            )
        } else {
            holder.btnBookMarks.setColorFilter(
                holder.btnBookMarks.context.getColor(R.color.primaryLight)
            )
        }

        holder.root.setOnClickListener { onPostClick(post) }
        holder.btnFavorite.setOnClickListener {
            // INVERTIR ESTADO LOCAL
            post.userLiked = !post.userLiked

            // ACTUALIZAR UI
            if (post.userLiked) {
                holder.btnFavorite.setColorFilter(holder.btnFavorite.context.getColor(R.color.secondary))
            } else {
                holder.btnFavorite.setColorFilter(holder.btnFavorite.context.getColor(R.color.primaryLight))
            }
            // MANDAR LA ACCIÓN REAL
            onLikeClick(post)
        }
        holder.btnComments.setOnClickListener { onCommentClick(post) }
        holder.btnBookMarks.setOnClickListener {

            // Cambiar estado local
            post.userBookmarked = !post.userBookmarked

            // UI inmediata
            if (post.userBookmarked) {
                holder.btnBookMarks.setColorFilter(
                    holder.btnBookMarks.context.getColor(R.color.primary)
                )
            } else {
                holder.btnBookMarks.setColorFilter(
                    holder.btnBookMarks.context.getColor(R.color.primaryLight)
                )
            }

            // Llamar a la API real (sp_interact con type = 2)
            onBookmarkClick(post)
        }

    }

    override fun getItemCount() = posts.size


    // ───────────────────────────────
    // Dots del carrusel
    // ───────────────────────────────

    private fun setupDots(holder: PostViewHolder, count: Int) {
        holder.dotsLayout.removeAllViews()

        for (i in 0 until count) {
            val dot = ImageView(holder.itemView.context)
            dot.setImageResource(R.drawable.dot_inactive)

            val params = LinearLayout.LayoutParams(20, 20)
            params.marginEnd = 8
            dot.layoutParams = params

            holder.dotsLayout.addView(dot)
        }
    }

    private fun updateDots(holder: PostViewHolder, selectedIndex: Int) {
        for (i in 0 until holder.dotsLayout.childCount) {
            val dot = holder.dotsLayout.getChildAt(i) as ImageView
            dot.setImageResource(
                if (i == selectedIndex) R.drawable.dot_active
                else R.drawable.dot_inactive
            )
        }
    }
    fun addPosts(newPosts: List<PostModel>) {
        val posInicio = posts.size
        (posts as MutableList).addAll(newPosts)
        notifyItemRangeInserted(posInicio, newPosts.size)
    }
    private fun formatDate(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""

        return try {
            // Entrada: 2024-12-01T04:35:11.000Z
            val input = java.time.LocalDateTime.parse(raw.replace("Z",""))
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy")
            input.format(formatter)
        } catch (e: Exception) {
            raw
        }
    }

}
