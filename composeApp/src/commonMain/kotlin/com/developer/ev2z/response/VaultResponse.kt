package com.developer.ev2z.response

import kotlinx.serialization.Serializable



@Serializable
data class VaultResponse(
    val vault_token: String,
    val expires_at: String
)