// CommunityRepository.kt
package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Post
import com.developer.ev2z.response.StoryResponse
import com.developer.ev2z.response.StoryViewersResponse
import kotlinx.serialization.json.JsonObject

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
    ): JsonObject {
        return apiService.createCommunityPost(title, body, images)
    }


    suspend fun toggleLike(postId: Int): Boolean {
        return apiService.togglePostLike(postId)
    }



        suspend fun getComments(postId: Int) =
            apiService.getPostComments(postId).comments

        suspend fun addComment(
            postId: Int,
            text: String,
            parentId: Int?
        ) = apiService.postComment(postId, text, parentId)

        suspend fun likeComment(commentId: Int) =
            apiService.likeComment(commentId)


    suspend fun fetchStories(): StoryResponse {
        return apiService.getStories()
    }

    // ---------------- STORIES ----------------
    suspend fun addStory(
        body: String,
        media: List<ByteArray>
    ): JsonObject {
        return apiService.addStory(body, media)
    }


    // ---------------- STORY VIEW ----------------
    suspend fun viewStory(storyId: Int) {
        apiService.viewStory(storyId)
    }

    // ---------------- STORY VIEWERS ----------------
    suspend fun getStoryViewers(storyId: Int): StoryViewersResponse {
        return apiService.getStoryViewers(storyId)
    }

}