package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    val success: Boolean,
    val total: Int,
    val comments: List<Comment>
)

@Serializable
data class Comment(
    val id: Int,
    val post_id: Int,
    val parent_id: Int? = null,
    val comment: String,
    val created_at: String,
    val likes_count: Int = 0,
    val is_liked: Boolean = false,
    val liked: Boolean = false,
    val user: CommentUser
)

@Serializable
data class CommentUser(
    val id: Int,
    val name: String,
    val photo: String? = null
)
