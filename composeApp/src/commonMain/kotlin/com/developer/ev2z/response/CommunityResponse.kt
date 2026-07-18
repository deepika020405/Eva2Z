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
    val title: String? = null,
    val body: String? = null,
    val media_urls: List<String> = emptyList(),
    val created_at: String = "",
    val comments_count: Int = 0,
    val likes_count: Int = 0,
    val liked: Boolean = false,
    val user: Users? = null,   // ✅ IMPORTANT
    // 📍 Location + classification (API sends lat/long as strings, e.g. "18.55900000")
    val lat: String? = null,
    val long: String? = null,
    val city: String? = null,
    val category: String? = null,
)

@Serializable
data class Users(
    val id: Int,
    val name: String,
    val photo: String? = null
)
