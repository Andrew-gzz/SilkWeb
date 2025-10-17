package com.example.silkweb.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.presentation.adapter.FeedAdapter.PostViewHolder

data class DraftModel(
    val username: String,
    val title: String,
    val id_draft: String
)
class DraftAdapter(private val drafts:List<DraftModel>):
    RecyclerView.Adapter<DraftAdapter.DraftViewHolder>(){
    class DraftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.id_tvTitleDraft)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_draft, parent, false)
        return DraftViewHolder(view)
    }
    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        val post = drafts[position]
        holder.title.text = post.title
    }
    override fun getItemCount() = drafts.size
}