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
import com.example.silkweb.data.controller.interactionController
import com.example.silkweb.presentation.adapter.FeedAdapter
import com.example.silkweb.data.controller.postController
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.model.InteractionModel
import com.example.silkweb.data.model.PostModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private val posts = mutableListOf<PostModel>()

    // PAGINACIÓN
    private var offset = 0
    private val limit = 5
    private var isLoading = false
    private var allLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        recyclerView = view.findViewById(R.id.id_recyclerFeed)
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        adapter = FeedAdapter(
            posts = posts,
            onPostClick = { post ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                intent.putExtra("post_id", post.id)
                startActivity(intent)
            },
            onLikeClick = { post ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(requireContext())
                        val user = db.userDaoLocal().getUser()
                        if (user == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        val data = InteractionModel(
                            idPost = post.id,
                            username = user.username,
                            idComment = null,
                            type = 1
                        )

                        val message = interactionController.interact(data)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onCommentClick = { post ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                intent.putExtra("post_id", post.id)
                intent.putExtra("open_comments", true)
                startActivity(intent)
            },
            onBookmarkClick = { post ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(requireContext())
                        val user = db.userDaoLocal().getUser()

                        if (user == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        val data = InteractionModel(
                            idPost = post.id,
                            username = user.username,
                            idComment = null,
                            type = 2
                        )

                        val message = interactionController.interact(data)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        recyclerView.adapter = adapter

        // Cargar primera página
        cargarFeed()

        // Listener para scroll infinito
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && !isLoading && !allLoaded) {
                    val visible = layoutManager.childCount
                    val total = layoutManager.itemCount
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()

                    if (visible + firstVisible >= total - 2) {
                        cargarFeed()
                    }
                }
            }
        })

        return view
    }

    private fun cargarFeed() {
        if (isLoading || allLoaded) return
        isLoading = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val feedList = postController.getFeed(limit, offset)

                // Si ya no hay más posts
                if (feedList.isEmpty()) {
                    allLoaded = true
                    isLoading = false
                    return@launch
                }

                // Cargar BLOBs por post
                for (post in feedList) {
                    val mediaFiles = postController.getPostMedia(post.id)
                    post.mediaFiles = mediaFiles
                }

                offset += limit

                withContext(Dispatchers.Main) {
                    adapter.addPosts(feedList)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isLoading = false
            }
        }
    }
}
