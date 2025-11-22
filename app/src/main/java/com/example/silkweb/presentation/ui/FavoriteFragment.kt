package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.controller.interactionController
import com.example.silkweb.data.controller.postController
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.model.InteractionModel
import com.example.silkweb.data.model.PostModel
import com.example.silkweb.presentation.adapter.FeedAdapter
import com.example.silkweb.utils.ConnectionUtils
import com.example.silkweb.utils.showCustomSnackbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private val posts = mutableListOf<PostModel>()

    // Loading UI
    private lateinit var loadingContainer: FrameLayout
    private lateinit var ivLoading: ImageView
    private lateinit var ivOffline: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_favorite, container, false)

        recyclerView = view.findViewById(R.id.id_recyclerFeed)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

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
                    val db = AppDatabase.getDatabase(requireContext())
                    val user = db.userDaoLocal().getUser() ?: return@launch

                    val data = InteractionModel(
                        idPost = post.id,
                        username = user.username,
                        idComment = null,
                        type = 1
                    )

                    val message = interactionController.interact(data)

                    withContext(Dispatchers.Main) { showCustomSnackbar(message) }
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
                    val db = AppDatabase.getDatabase(requireContext())
                    val user = db.userDaoLocal().getUser() ?: return@launch

                    val data = InteractionModel(
                        idPost = post.id,
                        username = user.username,
                        idComment = null,
                        type = 2
                    )

                    val message = interactionController.interact(data)

                    withContext(Dispatchers.Main) { showCustomSnackbar(message) }
                }
            }
        )

        recyclerView.adapter = adapter

        view.findViewById<MaterialButton>(R.id.id_btnFilter).setOnClickListener {
            showFilterMenu(it)
        }

        cargarGuardados()

        return view
    }

    override fun onResume() {
        super.onResume()
        cargarGuardados()
    }

    // ====================================================
    // Cargar TODOS los posts y filtrar solo los guardados
    // ====================================================
    private fun cargarGuardados() {
        mostrarLoading()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!ConnectionUtils.hayInternet(requireContext())) {
                    withContext(Dispatchers.Main) { mostrarOffline() }
                    return@launch
                }

                val db = AppDatabase.getDatabase(requireContext())
                val user = db.userDaoLocal().getUser()
                if (user == null) {
                    withContext(Dispatchers.Main) {
                        ocultarLoading()
                        Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Obtener TODAS las publicaciones
                val feedList = postController.getFeedFull()

                val savedPosts = mutableListOf<PostModel>()

                for (post in feedList) {

                    val isSaved = interactionController.isSaved(user.username, post.id)

                    if (isSaved) {
                        // cargar imágenes del post guardado
                        val mediaFiles = postController.getPostMedia(post.id)
                        post.mediaFiles = mediaFiles

                        val isFav = interactionController.isFavorite(user.username, post.id)
                        post.userLiked = isFav
                        post.userBookmarked = true

                        savedPosts.add(post)
                    }
                }

                withContext(Dispatchers.Main) {
                    posts.clear()
                    posts.addAll(savedPosts)
                    adapter.notifyDataSetChanged()
                    ocultarLoading()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarOffline()
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showFilterMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_filters, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.filter_date_asc -> posts.sortBy { it.createdAt }
                R.id.filter_date_desc -> posts.sortByDescending { it.createdAt }
                R.id.filter_title -> posts.sortBy { it.title.lowercase() }
                R.id.filter_user -> posts.sortBy { it.username.lowercase() }
            }
            adapter.notifyDataSetChanged()
            true
        }

        popup.show()
    }

    // Loading / Offline UI
    private fun mostrarLoading() {
        loadingContainer.visibility = View.VISIBLE
        ivOffline.visibility = View.GONE
        ivLoading.visibility = View.VISIBLE
        ivLoading.animate()
            .rotationBy(360f)
            .setDuration(1200)
            .setInterpolator(LinearInterpolator())
            .withEndAction { mostrarLoading() }
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
