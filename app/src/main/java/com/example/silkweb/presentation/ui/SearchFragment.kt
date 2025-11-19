package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.controller.postController
import com.example.silkweb.presentation.adapter.FeedAdapter
import com.example.silkweb.data.model.PostModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private val posts = mutableListOf<PostModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.id_recyclerFeed)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FeedAdapter(
            posts = posts,
            onPostClick = { post ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                    .putExtra("post_id", post.id)
                startActivity(intent)
            },
            onLikeClick = { post ->
                Toast.makeText(requireContext(), "Like post ${post.id}", Toast.LENGTH_SHORT).show()
            },
            onCommentClick = { post ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                    .putExtra("post_id", post.id)
                    .putExtra("open_comments", true)
                startActivity(intent)
            },
            onBookmarkClick = { post ->
                Toast.makeText(requireContext(), "Bookmark post ${post.id}", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.adapter = adapter

        cargarResultadosDeBusqueda()

        return view
    }

    private fun cargarResultadosDeBusqueda() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lista = postController.getFeed()  // usa el VIEW real

                withContext(Dispatchers.Main) {
                    posts.clear()
                    posts.addAll(lista)
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
