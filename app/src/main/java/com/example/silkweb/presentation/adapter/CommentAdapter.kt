package com.example.silkweb.presentation.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.model.CommentModel

class CommentAdapter(
    private val comments: List<CommentModel>,
    private val onCommentClick: (CommentModel) -> Unit,
    private val onReplyClick: (CommentModel) -> Unit,
    private val onLikeCommentClick: (CommentModel) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.id_ivProfileImage)
        val username: TextView = view.findViewById(R.id.id_tvUsername)
        val body: TextView = view.findViewById(R.id.id_etDescription)
        val btnFavorite: ImageButton = view.findViewById(R.id.id_btnFavorite)
        val btnReply: TextView = view.findViewById(R.id.id_tvReply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        // FOTO
        if (comment.userPhotoFile != null) {
            val bmp = BitmapFactory.decodeByteArray(comment.userPhotoFile, 0, comment.userPhotoFile.size)
            holder.profileImage.setImageBitmap(bmp)
        } else {
            holder.profileImage.setImageResource(R.drawable.silkweb)
        }

        holder.username.text = comment.username
        holder.body.text = comment.body

        // REFERENCIA A QUIÉN RESPONDIÓ
        val tvReplyTo = holder.itemView.findViewById<TextView>(R.id.id_tvReplyComment)

        if (comment.isReply && comment.parentCommentId != null) {
            // Buscar al usuario al que está respondiendo
            val parent = comments.firstOrNull { it.commentId == comment.parentCommentId }

            if (parent != null) {
                tvReplyTo.visibility = View.VISIBLE
                tvReplyTo.text = "Respondiendo a @${parent.username}"
            } else {
                tvReplyTo.visibility = View.GONE
            }
        } else {
            tvReplyTo.visibility = View.GONE
        }

        // INDENTACIÓN SI ES RESPUESTA
        val params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = if (comment.isReply) 70 else 0
        holder.itemView.layoutParams = params

        // CLICK EN TODA LA CARD
        holder.itemView.setOnClickListener { onCommentClick(comment) }

        // CLICK EN RESPONDER
        holder.btnReply.setOnClickListener { onReplyClick(comment) }

        // LIKE DEL COMENTARIO
        holder.btnFavorite.setOnClickListener { onLikeCommentClick(comment) }
    }

}
