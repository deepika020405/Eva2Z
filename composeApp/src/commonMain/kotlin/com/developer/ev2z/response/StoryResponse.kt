package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class StoryResponse(
    val success: Boolean,
    val total_users: Int,
    val data: List<StoryGroupResponse>
)

@Serializable
data class StoryGroupResponse(
    val user: Users,
    val is_own_story: Boolean,
    val stories: List<Story>
)

@Serializable
data class Story(
    val id: Int,
    val media_urls: List<String>,
    val view_count: Int,
    val created_at: String,
    val expires_at: String
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

@Serializable
data class StoryGroup(
    val user: Users,
    val stories: List<Story>,
    val isOwnStory: Boolean
)
