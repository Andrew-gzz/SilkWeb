package com.example.silkweb.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silkweb.R
import com.example.silkweb.data.controller.commentController
import com.example.silkweb.data.controller.interactionController
import com.example.silkweb.presentation.adapter.FeedAdapter
import com.example.silkweb.data.controller.postController
import com.example.silkweb.data.local.AppDatabase
import com.example.silkweb.data.model.CreateComment
import com.example.silkweb.data.model.InteractionModel
import com.example.silkweb.data.model.PostModel
import com.example.silkweb.presentation.adapter.CommentAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PostDetailActivity : AppCompatActivity() {

    private lateinit var rvPost: RecyclerView
    private lateinit var rvComments: RecyclerView   // ← Para comentarios
    private var openComments: Boolean = false       // ← Por si viene desde el botón comentarios
    private var replyToCommentId: Int? = null // ← Para responder a un comentario



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val postId = intent.getIntExtra("post_id", -1)
        openComments = intent.getBooleanExtra("open_comments", false)

        if (postId <= 0) {
            Toast.makeText(this, "Post inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Botón Back
        findViewById<ImageButton>(R.id.id_back)?.setOnClickListener { finish() }

        // Recycler principal (post)
        rvPost = findViewById(R.id.id_recyclerFeed)
        rvPost.layoutManager = LinearLayoutManager(this)

        // Recycler de comentarios (por ahora vacío)
        rvComments = findViewById(R.id.id_recyclerComment)
        rvComments.layoutManager = LinearLayoutManager(this)

        // Cargar información real del post
        cargarPost(postId)

        // Comentar algo
        val etComment = findViewById<EditText>(R.id.id_etComment)
        val btnSend = findViewById<ImageButton>(R.id.id_btnSendComment)

        btnSend.setOnClickListener {
            val text = etComment.text.toString().trim()

            if (text.isEmpty()) {
                Toast.makeText(this, "Escribe un comentario primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Obtener usuario local
                    val db = AppDatabase.getDatabase(this@PostDetailActivity)
                    val user = db.userDaoLocal().getUser()

                    if (user == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PostDetailActivity,
                                "Usuario no encontrado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }

                    // Crear modelo CreateComment
                    val newComment = CreateComment(
                        idPost = postId,
                        username = user.username,
                        idComment = replyToCommentId,   // puede ser null si no estás respondiendo
                        body = text
                    )

                    // Llamar al SP CREATE COMMENT
                    val newId = commentController.createComment(newComment)

                    // Limpiar caja
                    withContext(Dispatchers.Main) {
                        etComment.setText("")
                        Toast.makeText(this@PostDetailActivity, "Comentario enviado", Toast.LENGTH_SHORT).show()
                    }

                    // Refrescar lista de comentarios
                    refreshComments(postId)

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@PostDetailActivity,
                            "Error al enviar: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            replyToCommentId = null
        }


    }

    private fun cargarPost(postId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                // Obtener datos del post (VIEW + datos del usuario)
                val post: PostModel? = postController.getPostDetails(postId)

                if (post == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PostDetailActivity, "No se encontró el post", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                // Cargar imágenes BLOB
                val mediaFiles = postController.getPostMedia(post.id)
                post.mediaFiles = mediaFiles

                val db = AppDatabase.getDatabase(this@PostDetailActivity)
                val user = db.userDaoLocal().getUser()

                // determinar si es favorito
                val isFav = interactionController.isFavorite(user!!.username, post.id)
                post.userLiked = isFav

                val isSaved = interactionController.isSaved(user.username, post.id)
                post.userBookmarked = isSaved

                //--------------------------------------------------------------//
                //-------------- Seccion de eventos en comentarios--------------//
                //--------------------------------------------------------------//
                val comments = commentController.getComments(post.id)

                val userLocal = user  //Optener usuario activo

                comments.forEachIndexed { index, c -> //Buscar si el comentario es favorito
                    c.position = index
                    c.userLiked = interactionController.isCommentFav(userLocal.username, c.commentId)
                }

                withContext(Dispatchers.Main) {
                    rvComments.adapter = CommentAdapter(
                        comments = comments,
                        onCommentClick = { comment ->
                            // Abre un Activity con detalles del comentario

                        },
                        onReplyClick = { comment ->
                            // Rellenar caja de texto con @username
                            replyToCommentId = comment.commentId

                            val etComment = findViewById<EditText>(R.id.id_etComment)
                            etComment.setText("@${comment.username} ")
                            etComment.requestFocus()

                            // OPCIONAL: al abrir el teclado, hacer scroll hasta abajo
                            rvComments.post {
                                rvComments.smoothScrollToPosition(comments.size - 1)
                            }
                        },
                        onLikeCommentClick = { comment, pos ->

                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val db = AppDatabase.getDatabase(this@PostDetailActivity)
                                    val user = db.userDaoLocal().getUser()

                                    if (user == null) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@PostDetailActivity, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                                        }
                                        return@launch
                                    }

                                    val data = InteractionModel(
                                        idPost = comment.postId,
                                        username = user.username,
                                        idComment = comment.commentId,
                                        type = 1
                                    )

                                    val message = interactionController.interact(data)

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@PostDetailActivity, message, Toast.LENGTH_SHORT).show()
                                    }

                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@PostDetailActivity, "${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
                //--------------------------------------------------------------//
                //-------------- Seccion de eventos en publicación--------------//
                //--------------------------------------------------------------//
                withContext(Dispatchers.Main) {
                    rvPost.adapter = FeedAdapter(
                        posts = listOf(post),
                        onPostClick = {}, // ya estás en el detalle
                        onLikeClick = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val db = AppDatabase.getDatabase(this@PostDetailActivity)
                                    val user = db.userDaoLocal().getUser()

                                    if (user == null) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                this@PostDetailActivity,
                                                "Usuario no encontrado",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                                        Toast.makeText(
                                            this@PostDetailActivity,
                                            message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@PostDetailActivity,
                                            e.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        onCommentClick = {
                            Toast.makeText(this@PostDetailActivity, "Comentarios próximamente", Toast.LENGTH_SHORT).show()
                        },
                        onBookmarkClick = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val db = AppDatabase.getDatabase(this@PostDetailActivity)
                                    val user = db.userDaoLocal().getUser()

                                    if (user == null) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                this@PostDetailActivity,
                                                "Usuario no encontrado",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                                        Toast.makeText(
                                            this@PostDetailActivity,
                                            message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@PostDetailActivity,
                                            e.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    )
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PostDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun refreshComments(postId: Int) {
        val comments = commentController.getComments(postId)

        withContext(Dispatchers.Main) {
            rvComments.adapter = CommentAdapter(
                comments = comments,
                onCommentClick = { comment ->
                    // Abrir detalle del comentario
                },
                onReplyClick = { comment ->
                    replyToCommentId = comment.commentId
                    val etComment = findViewById<EditText>(R.id.id_etComment)
                    etComment.setText("@${comment.username} ")
                    etComment.requestFocus()
                },
                onLikeCommentClick = { c, pos ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getDatabase(this@PostDetailActivity)
                            val user = db.userDaoLocal().getUser()

                            val data = InteractionModel(
                                idPost = postId,
                                username = user!!.username,
                                idComment = c.commentId,
                                type = 1
                            )

                            val msg = interactionController.interact(data)

                            withContext(Dispatchers.Main) {
                                c.userLiked = !c.userLiked
                                rvComments.adapter?.notifyItemChanged(pos)
                                Toast.makeText(this@PostDetailActivity, msg, Toast.LENGTH_SHORT).show()
                            }

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@PostDetailActivity, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

            )
        }
    }

}
