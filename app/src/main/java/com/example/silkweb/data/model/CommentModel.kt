package com.example.silkweb.data.model

//data class para crear un comentario
data class CreateComment(
    val idPost: Int, //Post al que pertenece
    val username: String, //Usuario que hizo el comentario
    val idComment: Int?, //Id del comentario al que respondió
    val body: String //Descripción del cometario
)

//Aqui va la data class para traer todos los comentarios de un post

data class CommentModel(
    val commentId: Int,
    val postId: Int,
    val userId: Int,
    val username: String,
    val userName: String,
    val userLastName: String,
    val userPhotoFile: ByteArray?,
    val userPhotoName: String?,
    val userPhotoRoute: String?,
    val body: String,
    val createdAt: String,
    val parentCommentId: Int?,   // null = comentario normal
    val isReply: Boolean         // true = respuesta
)
