package com.developer.ev2z.response


import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads an id that the backend may send either as a JSON number (e.g. `48`)
 * or a quoted string (e.g. `"48"`), always producing a String. The vault
 * list endpoint returns a numeric id while other endpoints quote it, so a
 * plain `String` field would fail to deserialize the list.
 */
internal object IdAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("VaultId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String =
        if (decoder is JsonDecoder) decoder.decodeJsonElement().jsonPrimitive.content
        else decoder.decodeString()

    override fun serialize(encoder: Encoder, value: String) =
        encoder.encodeString(value)
}

@Serializable
data class VaultDocumentDto(
    @Serializable(with = IdAsStringSerializer::class)
    val id: String,

    @SerialName("file_name")
    val fileName: String,

    @SerialName("file_type")
    val fileType: String,

    @SerialName("created_at")
    val createdAt: String,

    // Server-detected document type (e.g. "dl", "rc", "puc", "insurance").
    // Optional — only present once the backend has validated the document.
    @SerialName("document_type")
    val documentType: String? = null,

    // Server-computed expiry date in "yyyy-MM-dd" form, when available.
    @SerialName("expiry_date")
    val expiryDate: String? = null,

    // Absolute (or relative) URL of the stored image, when available.
    @SerialName("image_url")
    val imageUrl: String? = null
)