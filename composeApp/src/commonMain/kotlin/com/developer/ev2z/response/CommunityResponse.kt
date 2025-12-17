package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class CommunityResponse(
    val current_page: Int,
    val data: List<Post>,
    val total: Int
)
@Serializable
data class Post(
    val id: Int,
    val title: String?,
    val body: String?,
    val media_urls: List<String>,
    val created_at: String,
    val comments_count: Int,
    val likes_count: Int,
    val user: Users
)

@Serializable
data class Users(
    val id: Int,
    val name: String,
    val photo: String? = null
)
