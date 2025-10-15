package com.example.silkweb.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R

data class PostModel(
    val username: String,
    val title: String,
    val description: String,
    val likes: String,
    val comments: String
)

class FeedAdapter(private val posts: List<PostModel>) :
    RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.id_ivProfileImage)
        val username: TextView = itemView.findViewById(R.id.id_tvTitle)
        val title: TextView = itemView.findViewById(R.id.id_etTitle)
        val description: TextView = itemView.findViewById(R.id.id_etDescription)
        val likesCount: TextView = itemView.findViewById(R.id.id_tvFavoritesCount)
        val commentsCount: TextView = itemView.findViewById(R.id.id_tvCommentsCount)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.id_btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.username.text = post.username
        holder.title.text = post.title
        holder.description.text = post.description
        holder.likesCount.text = post.likes
        holder.commentsCount.text = post.comments
    }

    override fun getItemCount() = posts.size
}
