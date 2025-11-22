package com.example.silkweb.presentation.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.model.CommentModel

class CommentAdapter(
    private val comments: List<CommentModel>,
    private val onCommentClick: (CommentModel) -> Unit,
    private val onReplyClick: (CommentModel) -> Unit,
    private val onLikeCommentClick: (CommentModel, Int) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.id_ivProfileImage)
        val username: TextView = view.findViewById(R.id.id_tvUsername)
        val body: TextView = view.findViewById(R.id.id_etDescription)
        val btnFavorite: ImageButton = view.findViewById(R.id.id_btnFavorite)
        val btnReply: TextView = view.findViewById(R.id.id_tvReply)
        val date: TextView = view.findViewById(R.id.id_tvDate)
        val hideButton: ImageButton = view.findViewById(R.id.id_btnHide)
        val replyTo: TextView = view.findViewById(R.id.id_tvReplyComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        comment.position = position

        // FOTO
        if (comment.userPhotoFile != null) {
            val bmp = BitmapFactory.decodeByteArray(comment.userPhotoFile, 0, comment.userPhotoFile.size)
            holder.profileImage.setImageBitmap(bmp)
        } else {
            holder.profileImage.setImageResource(R.drawable.silkweb)
        }

        holder.username.text = comment.username
        holder.body.text = comment.body
        holder.date.text = formatDate(comment.createdAt)

        // 🔥 Colores del like
        val ctx = holder.itemView.context
        val likedColor = ContextCompat.getColor(ctx, R.color.secondary)
        val normalColor = ContextCompat.getColor(ctx, R.color.primaryLight)

        // Estado inicial según userLiked
        holder.btnFavorite.setColorFilter(
            if (comment.userLiked) likedColor else normalColor
        )

        // OCULTAR / MOSTRAR
        if (comment.isHidden) {
            holder.body.visibility = View.GONE
            holder.btnFavorite.visibility = View.GONE
            holder.btnReply.visibility = View.GONE
            holder.hideButton.setImageResource(R.drawable.ic_show)
        } else {
            holder.body.visibility = View.VISIBLE
            holder.btnFavorite.visibility = View.VISIBLE
            holder.btnReply.visibility = View.VISIBLE
            holder.hideButton.setImageResource(R.drawable.ic_hide)
        }

        // REFERENCIA A QUIÉN RESPONDIÓ
        if (comment.isReply && comment.parentCommentId != null) {
            val parent = comments.firstOrNull { it.commentId == comment.parentCommentId }
            if (parent != null) {
                holder.replyTo.visibility = View.VISIBLE
                holder.replyTo.text = "Respondiendo a @${parent.username}"
            } else {
                holder.replyTo.visibility = View.GONE
            }
        } else {
            holder.replyTo.visibility = View.GONE
        }

        // INDENTACIÓN PARA RESPUESTA
        val params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = if (comment.isReply) 70 else 0
        holder.itemView.layoutParams = params

        // OCULTAR
        holder.hideButton.setOnClickListener {
            comment.isHidden = !comment.isHidden
            notifyItemChanged(holder.bindingAdapterPosition)
        }

        // CLICK AL CARD
        holder.itemView.setOnClickListener { onCommentClick(comment) }

        // RESPONDER
        holder.btnReply.setOnClickListener { onReplyClick(comment) }

        // LIKE DEL COMENTARIO
        holder.btnFavorite.setOnClickListener {
            // Invertir estado local
            comment.userLiked = !comment.userLiked

            // Actualizar color inmediatamente
            holder.btnFavorite.setColorFilter(
                if (comment.userLiked) likedColor else normalColor
            )

            // Avisar al Activity (para llamar al backend)
            onLikeCommentClick(comment, holder.bindingAdapterPosition)
        }
    }

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        return try {
            val input = java.time.LocalDateTime.parse(raw.replace("Z", ""))
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy")
            input.format(formatter)
        } catch (e: Exception) {
            raw
        }
    }
}
