package com.example.silkweb.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.controller.interactionController
import com.example.silkweb.data.controller.postController
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.model.InteractionModel
import com.example.silkweb.presentation.adapter.FeedAdapter
import com.example.silkweb.data.model.PostModel
import com.example.silkweb.utils.ConnectionUtils
import com.example.silkweb.utils.showCustomSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter

    // Lista que se muestra actualmente
    private val posts = mutableListOf<PostModel>()
    private val allPosts = mutableListOf<PostModel>()

    // Variables para el manejo de carga
    private lateinit var loadingContainer: FrameLayout
    private lateinit var ivLoading: ImageView
    private lateinit var ivOffline: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_search, container, false)

        loadingContainer = view.findViewById(R.id.id_loadingContainer)
        ivLoading = view.findViewById(R.id.id_ivLoading)
        ivOffline = view.findViewById(R.id.id_ivOffline)

        recyclerView = view.findViewById(R.id.id_recyclerFeed)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FeedAdapter(
            posts = posts,
            onPostClick = { post ->
                startActivity(
                    Intent(requireContext(), PostDetailActivity::class.java)
                        .putExtra("post_id", post.id)
                )
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
                            showCustomSnackbar(message)
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onCommentClick = { post ->
                startActivity(
                    Intent(requireContext(), PostDetailActivity::class.java)
                        .putExtra("post_id", post.id)
                        .putExtra("open_comments", true)
                )
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
                            showCustomSnackbar(message)
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

        //Validar offline y loading
        if (!ConnectionUtils.hayInternet(requireContext())) {
            mostrarOffline()
        } else {
            mostrarLoading()
        }

        // Cargar publicaciones disponibles
        cargarResultadosDeBusqueda()

        // Buscador
        val etSearch = view.findViewById<EditText>(R.id.id_etSearch)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarResultados(s.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return view
    }

    // Cargar todos los posts sin imágenes (más rápido)
    private fun cargarResultadosDeBusqueda() {
        mostrarLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lista = postController.getFeedFull()

                val db = AppDatabase.getDatabase(requireContext())
                val user = db.userDaoLocal().getUser()

                for (post in lista) {
                    post.userLiked = interactionController.isFavorite(user!!.username, post.id)
                    post.userBookmarked = interactionController.isSaved(user.username, post.id)
                }

                allPosts.clear()
                allPosts.addAll(lista)

                withContext(Dispatchers.Main) {
                    posts.clear()
                    posts.addAll(lista)
                    adapter.notifyDataSetChanged()

                    ocultarLoading()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ocultarLoading()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Filtrar mientras escribe
    private fun filtrarResultados(query: String) {
        posts.clear()

        if (query.isEmpty()) {
            posts.addAll(allPosts)
        } else {
            val q = query.lowercase()

            posts.addAll(
                allPosts.filter { post ->
                    post.title.lowercase().contains(q) ||
                            post.body.lowercase().contains(q) ||
                            post.username.lowercase().contains(q)
                }
            )
        }

        adapter.notifyDataSetChanged()
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