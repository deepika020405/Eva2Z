package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for `GET document/{id}` — full detail of a single vault document,
 * including the server-side OCR extraction result.
 *
 * ```
 * { "success": true, "data": { "id": 51, "document_type": "puc", ... } }
 * ```
 */
@Serializable
data class VaultDocumentDetailResponse(
    val success: Boolean = false,
    val data: VaultDocumentDetail? = null,
)

@Serializable
data class VaultDocumentDetail(
    val id: Int? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("document_type")
    val documentType: String? = null,

    @SerialName("issue_date")
    val issueDate: String? = null,

    @SerialName("expiry_date")
    val expiryDate: String? = null,

    @SerialName("extraction_confidence")
    val extractionConfidence: String? = null,

    @SerialName("extraction_status")
    val extractionStatus: String? = null,

    @SerialName("file_name")
    val fileName: String? = null,

    @SerialName("file_type")
    val fileType: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("document_number")
    val documentNumber: String? = null,

    @SerialName("holder_name")
    val holderName: String? = null,

    @SerialName("father_name")
    val fatherName: String? = null,

    val address: String? = null,
    val dob: String? = null,

    @SerialName("vehicle_class")
    val vehicleClass: String? = null,

    @SerialName("issuing_authority")
    val issuingAuthority: String? = null,

    @SerialName("vehicle_number")
    val vehicleNumber: String? = null,

    @SerialName("puc_number")
    val pucNumber: String? = null,

    @SerialName("licence_number")
    val licenceNumber: String? = null,
) {
    /** The single most relevant identifier for this document, if any. */
    val primaryIdentifier: String?
        get() = listOfNotNull(
            documentNumber, vehicleNumber, pucNumber, licenceNumber,
        ).firstOrNull { it.isNotBlank() }

    /** Confidence as a percentage string, e.g. "100%". */
    val confidencePercent: String?
        get() = extractionConfidence
            ?.toDoubleOrNull()
            ?.let { "${(it * 100).toInt()}%" }
}