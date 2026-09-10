package com.developer.ev2z.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Finds the first non-blank string value stored under any of [keys], anywhere in the
 * tree — direct hits first, then nested objects/arrays.
 *
 * Order-creation endpoints on our backend are not consistent about where they put the
 * PhonePe identifiers: sometimes top level, sometimes under `data`, and sometimes only
 * inside `order.raw_response`, which is PhonePe's own reply kept as an escaped JSON
 * *string*. String values that look like JSON are therefore parsed and searched too.
 */
fun JsonElement.findStringDeep(vararg keys: String, depth: Int = 0): String? {
    if (depth > 6) return null

    when (this) {
        is JsonObject -> {
            // Prefer a direct hit at this level before descending.
            for (key in keys) {
                (this[key] as? JsonPrimitive)?.content
                    ?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { return it }
            }
            for ((_, child) in this) {
                child.findStringDeep(*keys, depth = depth + 1)?.let { return it }
            }
        }

        is JsonArray -> forEach { child ->
            child.findStringDeep(*keys, depth = depth + 1)?.let { return it }
        }

        is JsonPrimitive -> {
            if (!isString) return null
            val text = content.trim()
            if (text.startsWith("{") || text.startsWith("[")) {
                runCatching { Json.parseToJsonElement(text) }
                    .getOrNull()
                    ?.findStringDeep(*keys, depth = depth + 1)
                    ?.let { return it }
            }
        }
    }
    return null
}