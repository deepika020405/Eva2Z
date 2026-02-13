package com.developer.ev2z.ui.screens.CriminalVerification


import kotlinx.serialization.json.*

fun buildCandidateRequest(form: CriminalForm): JsonObject {
    return JsonObject(
        mapOf(
            "candidate" to JsonObject(
                mapOf(
                    "name" to Json.encodeToJsonElement(form.name),
                    "email" to Json.encodeToJsonElement(form.email),
                    "phone" to Json.encodeToJsonElement(form.phone),
                    "employee_id" to Json.encodeToJsonElement(form.employeeId),
                    "invite" to Json.encodeToJsonElement(false),
                    "is_consent_undertaking_letter" to Json.encodeToJsonElement(form.consent),
                    "category_id" to Json.encodeToJsonElement(200),
                    "meta_data" to JsonObject(
                        mapOf("uuid" to Json.encodeToJsonElement(generateUUID()))
                    )
                )
            ),
            "package" to JsonObject(
                mapOf(
                    "package_id" to Json.encodeToJsonElement(2289),
                    "subtype_id" to Json.encodeToJsonElement(3542),
                    "config" to JsonObject(emptyMap())
                )
            )
        )
    )
}

fun generateUUID(): String = kotlin.random.Random.nextLong().toString()