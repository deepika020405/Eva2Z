package com.developer.ev2z.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupportRequest(
    val name: String,
    val phone: String,
    val email: String,
    val product: String,
    val subject: String,
    val message: String,
    @SerialName("issue_type") val issueType: String,
    val comment: String,
    @SerialName("appointment_date") val appointmentDate: String? = null,
    @SerialName("appointment_time") val appointmentTime: String? = null
)