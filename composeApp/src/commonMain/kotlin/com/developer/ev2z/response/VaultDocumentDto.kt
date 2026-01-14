package com.developer.ev2z.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VaultDocumentDto(
    val id: String,

    @SerialName("file_name")
    val fileName: String,

    @SerialName("file_type")
    val fileType: String,

    @SerialName("created_at")
    val createdAt: String
)