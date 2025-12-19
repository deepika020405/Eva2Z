package com.developer.ev2z.data.local.model

data class UpiApp(
    val name: String,
    val packageName: String,   // Android
    val scheme: String         // iOS
)