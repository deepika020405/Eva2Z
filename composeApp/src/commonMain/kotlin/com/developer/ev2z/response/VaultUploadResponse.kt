package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for `vault/upload` and `vault/update/{id}`.
 *
 * Success (rich):
 * ```
 * { "success": true, "message": "Image uploaded successfully.",
 *   "id": 41, "document_type": "dl", "expiry_date": "2038-12-05" }
 * ```
 * Success (minimal):
 * ```
 * { "success": true, "id": 38 }
 * ```
 * Rejection (document failed server-side validation):
 * ```
 * { "success": false, "error": "document_rejected",
 *   "reason": "wrong_document_type:expected_dl_got_puc",
 *   "detected_type": "puc", "message": "Document validation failed." }
 * ```
 */
@Serializable
data class VaultUploadResponse(
    val success: Boolean = false,
    val id: Int? = null,
    val message: String? = null,

    @SerialName("document_type")
    val documentType: String? = null,

    @SerialName("expiry_date")
    val expiryDate: String? = null,

    @SerialName("storage_path")
    val storagePath: String? = null,

    // Relative URL of the stored image, e.g. "/storage/vault/user_x/abc.jpeg".
    // Resolve against the app host to load it (see ApiService.absoluteImageUrl).
    @SerialName("image_url")
    val imageUrl: String? = null,

    val error: String? = null,
    val reason: String? = null,

    @SerialName("detected_type")
    val detectedType: String? = null,
) {
    /** The type the server expected, parsed from `reason` e.g. `wrong_document_type:expected_dl_got_puc`. */
    val expectedType: String?
        get() = reason
            ?.substringAfter("expected_", "")
            ?.substringBefore("_got_", "")
            ?.takeIf { it.isNotBlank() }

    val isWrongDocumentType: Boolean
        get() = error == "document_rejected" && reason?.startsWith("wrong_document_type") == true
}