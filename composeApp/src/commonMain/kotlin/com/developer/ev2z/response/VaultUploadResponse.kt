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
 * { "success": true, "id": "ed014dfd-b778-4191-b24f-82ec0b66022a" }
 * ```
 * The id comes back as a JSON number on older builds and as a quoted UUID on
 * the current server, so it is read through [IdAsStringSerializer] — a plain
 * `Int` field made the whole response fail to parse, silently discarding the
 * new document's id, type and expiry.
 * Rejection (document failed server-side validation). The server emits two
 * different shapes for this — the older `document_rejected` form:
 * ```
 * { "success": false, "error": "document_rejected",
 *   "reason": "wrong_document_type:expected_dl_got_puc",
 *   "detected_type": "puc", "message": "Document validation failed." }
 * ```
 * and the current `document_type_mismatch` form, which drops `reason` and names
 * the expected type in its own field (types come back upper-case here):
 * ```
 * { "success": false, "error": "document_type_mismatch",
 *   "message": "Please upload a valid RC document.",
 *   "requested_type": "RC", "detected_type": "UNKNOWN" }
 * ```
 * Both are handled — see [isWrongDocumentType] and [expectedType].
 */
@Serializable
data class VaultUploadResponse(
    val success: Boolean = false,

    @Serializable(with = IdAsStringSerializer::class)
    val id: String? = null,

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

    @SerialName("requested_type")
    val requestedType: String? = null,
) {
    /**
     * The type the server expected: from `requested_type` on the current
     * contract, else parsed out of `reason` (`wrong_document_type:expected_dl_got_puc`)
     * on the older one.
     */
    val expectedType: String?
        get() = requestedType?.takeIf { it.isNotBlank() }
            ?: reason
                ?.substringAfter("expected_", "")
                ?.substringBefore("_got_", "")
                ?.takeIf { it.isNotBlank() }

    val isWrongDocumentType: Boolean
        get() = error == "document_type_mismatch" ||
                (error == "document_rejected" && reason?.startsWith("wrong_document_type") == true)

    /**
     * True when the server rejected the upload without recognising what the
     * photo actually is — `detected_type: "UNKNOWN"`, or absent. Worth telling
     * apart from a real mismatch: the user needs a clearer photo, not a
     * different document.
     */
    val detectedTypeIsUnknown: Boolean
        get() = detectedType.isNullOrBlank() || detectedType.equals("unknown", ignoreCase = true)
}