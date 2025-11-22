package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.controller.postController
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.model.PostModel
import com.example.silkweb.presentation.adapter.FeedAdapter
import com.example.silkweb.utils.ConnectionUtils
import com.example.silkweb.utils.showCustomSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyPostsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FeedAdapter
    private val posts = mutableListOf<PostModel>()

    private lateinit var loadingContainer: FrameLayout
    private lateinit var ivLoading: ImageView
    private lateinit var ivOffline: ImageView


    private val modifyPostLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Cuando ModifyActivity se cierre con éxito → recargar
            if (result.resultCode == RESULT_OK) {
                cargarMisPosts()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        findViewById<ImageButton>(R.id.id_back).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.id_recyclerFeed)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadingContainer = findViewById(R.id.id_loadingContainer)
        ivLoading = findViewById(R.id.id_ivLoading)
        ivOffline = findViewById(R.id.id_ivOffline)

        adapter = FeedAdapter(
            posts = posts,
            onPostClick = { post ->
                val intent = Intent(this, ModifyActivity::class.java)
                intent.putExtra("post_id", post.id)
                modifyPostLauncher.launch(intent)
            },
            onLikeClick = {},
            onCommentClick = {},
            onBookmarkClick = {}
        )

        recyclerView.adapter = adapter

        cargarMisPosts()
    }

    // Cargar todos mis posts
    private fun cargarMisPosts() {
        mostrarLoading()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@MyPostsActivity)
                val user = db.userDaoLocal().getUser()

                if (user == null) {
                    withContext(Dispatchers.Main) {
                        ocultarLoading()
                        showCustomSnackbar("Usuario no encontrado")
                    }
                    return@launch
                }

                if (!ConnectionUtils.hayInternet(this@MyPostsActivity)) {
                    withContext(Dispatchers.Main) {
                        mostrarOffline()
                    }
                    return@launch
                }

                // Obtener posts
                val myPosts = postController.getMyPosts(user.username)

                // Cargar imagenes
                for (post in myPosts) {
                    post.mediaFiles = postController.getPostMedia(post.id)
                }

                withContext(Dispatchers.Main) {
                    posts.clear()
                    posts.addAll(myPosts)
                    adapter.notifyDataSetChanged()
                    ocultarLoading()

                    if (myPosts.isEmpty()) {
                        showCustomSnackbar("No tienes publicaciones aún")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarOffline()
                    showCustomSnackbar("Error: ${e.message}")
                }
            }
        }
    }

    private fun mostrarLoading() {
        loadingContainer.visibility = View.VISIBLE
        ivOffline.visibility = View.GONE
        ivLoading.visibility = View.VISIBLE

        ivLoading.animate().cancel()
        ivLoading.animate()
            .rotationBy(360f)
            .setDuration(1000)
            .setInterpolator(LinearInterpolator())
            .withEndAction { ivLoading.animate().rotationBy(360f).setDuration(1000).start() }
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