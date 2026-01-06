package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class StoryResponse(
    val auth_user_id: Int,
    val stories: List<Story>
)

@Serializable
data class Story(
    val id: Int,
    val user_id: Int,
    val title: String? = null,
    val body: String? = null,
    val media: List<String>,
    val created_at: String,
    val expires_at: String,
    val view_count: Int,
    val user: Users
)

@Serializable
data class AddStoryResponse(
    val story: Story
)
@Serializable
data class StoryViewersResponse(
    val story_id: Int= 0,
    val total_views: Int= 0,
    val viewers: List<StoryViewer> = emptyList()
)
@Serializable
data class StoryViewer(
    val id: Int,
    val user: Users,
    val created_at: String
)

