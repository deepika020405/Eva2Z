// CommunityRepository.kt
package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Post

class CommunityRepository(
    private val apiService: ApiService
) {

    suspend fun fetchPosts(): List<Post> {
        return apiService
            .getCommunityPosts()
            .data
    }


    suspend fun createPost(
        title: String,
        body: String,
        images: List<ByteArray>
    ) {
        apiService.createCommunityPost(title, body, images)
    }


    suspend fun toggleLike(postId: Int): Boolean {
        return apiService.togglePostLike(postId)
    }
}