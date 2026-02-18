package com.developer.ev2z.request

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CandidateRequest(
    val candidate: CandidateDto,
    val `package`: PackageDto
)

@Serializable
data class CandidateDto(
    val name: String,
    val email: String,
    val phone: String,
    val employee_id: String,
    val invite: Boolean,
    val is_consent_undertaking_letter: Boolean,
    val category_id: Int,
    val meta_data: JsonObject = JsonObject(emptyMap()) // ✅ FIX
)

@Serializable
data class PackageDto(
    val package_id: Int,
    val subtype_id: Int,
    val config: JsonObject = JsonObject(emptyMap()) // ✅ FIX
)