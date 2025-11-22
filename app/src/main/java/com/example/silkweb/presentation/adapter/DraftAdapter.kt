package com.example.silkweb.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.local.PostEntity

class DraftAdapter(
    private val drafts: List<PostEntity>,
    private val onEditClick: (PostEntity) -> Unit
) : RecyclerView.Adapter<DraftAdapter.DraftViewHolder>() {

    class DraftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.id_tvTitleDraft)
        val btnEdit: Button = itemView.findViewById(R.id.id_btnEditDraft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_draft, parent, false)
        return DraftViewHolder(view)
    }

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        val draft = drafts[position]

        // Mostrar el título del borrador
        holder.title.text = draft.title ?: "(Sin título)"

        // Listener
        holder.btnEdit.setOnClickListener {
            onEditClick(draft)
        }

    }

    override fun getItemCount(): Int = drafts.size
}
