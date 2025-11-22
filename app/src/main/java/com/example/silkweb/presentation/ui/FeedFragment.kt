package com.example.silkweb.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.google.android.material.snackbar.Snackbar
import com.example.silkweb.data.controller.interactionController
import com.example.silkweb.presentation.adapter.FeedAdapter
import com.example.silkweb.data.controller.postController
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.model.InteractionModel
import com.example.silkweb.data.model.PostModel
import com.example.silkweb.utils.ConnectionUtils
import com.example.silkweb.utils.showCustomSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private val posts = mutableListOf<PostModel>()
    // Variables para el manejo de la carga
    private lateinit var loadingContainer: FrameLayout
    private lateinit var ivLoading: ImageView
    private lateinit var ivOffline: ImageView

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

        loadingContainer = view.findViewById(R.id.id_loadingContainer)
        ivLoading = view.findViewById(R.id.id_ivLoading)
        ivOffline = view.findViewById(R.id.id_ivOffline)

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
                                showCustomSnackbar("Usuario no encontrado")
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
                            showCustomSnackbar(message)
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showCustomSnackbar("${e.message}")
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
                                showCustomSnackbar("Usuario no encontrado")
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
                            showCustomSnackbar(message)
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showCustomSnackbar(e.message.toString())
                        }
                    }
                }
            }
        )

        recyclerView.adapter = adapter
        //Validar offline y loading
        if (!ConnectionUtils.hayInternet(requireContext())) {
            mostrarOffline()
        } else {
            mostrarLoading()
        }

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
        mostrarLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val feedList = postController.getFeed(limit, offset)

                // Si ya no hay más posts
                if (feedList.isEmpty()) {
                    allLoaded = true

                    withContext(Dispatchers.Main) {
                        ocultarLoading()
                    }

                    isLoading = false
                    return@launch
                }

                // Cargar BLOBs por post
                val db = AppDatabase.getDatabase(requireContext())
                val user = db.userDaoLocal().getUser()

                for (post in feedList) {
                    // cargar imágenes
                    val mediaFiles = postController.getPostMedia(post.id)
                    post.mediaFiles = mediaFiles

                    if (user != null) {
                        val isFav = interactionController.isFavorite(user.username, post.id)
                        post.userLiked = isFav

                        val isSaved = interactionController.isSaved(user.username, post.id)
                        post.userBookmarked = isSaved
                    } else {
                        // Sin usuario → no marcar likes ni guardados
                        post.userLiked = false
                        post.userBookmarked = false
                    }
                }


                offset += limit

                withContext(Dispatchers.Main) {
                    adapter.addPosts(feedList)
                    ocultarLoading()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarOffline()
                    showCustomSnackbar("${e.message}")
                }
            } finally {
                isLoading = false
            }
        }
    }

    //Metodos para el load y el offline icon
    private fun mostrarLoading() {
        loadingContainer.visibility = View.VISIBLE
        ivOffline.visibility = View.GONE

        ivLoading.visibility = View.VISIBLE
        ivLoading.animate().cancel()

        ivLoading.animate()
            .rotationBy(360f)
            .setDuration(1200)
            .setInterpolator(LinearInterpolator())
            .withEndAction { mostrarLoading() } // animación infinita
            .start()
    }
    private fun mostrarOffline() {
        loadingContainer.visibility = View.VISIBLE
        ivLoading.visibility = View.GONE

        ivOffline.visibility = View.VISIBLE
    }
    private fun ocultarLoading() {
        loadingContainer.visibility = View.GONE
        ivLoading.animate().cancel()
    }
}