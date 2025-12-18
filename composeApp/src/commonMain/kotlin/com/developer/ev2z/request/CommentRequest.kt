// CommentRequest.kt
package com.developer.ev2z.request

import kotlinx.serialization.Serializable

@Serializable
data class CommentRequest(
    val comment: String,
    val parent_id: Int? = null
)