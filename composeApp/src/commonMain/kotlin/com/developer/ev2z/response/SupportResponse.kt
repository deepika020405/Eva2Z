package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupportResponse(
    val status: String,
    val data: SupportData? = null,
    val message: String? = null
)

@Serializable
data class SupportData(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    val subject: String,
    val message: String,
    val status: String,
    val priority: String,
    val name: String,
    val phone: String,
    val email: String,
    val product: String,
    @SerialName("issue_type") val issueType: String,
    val comment: String,
    @SerialName("appointment_date") val appointmentDate: String? = null,
    @SerialName("appointment_time") val appointmentTime: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)