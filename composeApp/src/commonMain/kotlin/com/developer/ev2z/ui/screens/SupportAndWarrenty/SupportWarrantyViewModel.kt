package com.developer.ev2z.ui.screens.SupportAndWarrenty

data class SupportWarrantyUi(
    val productName: String,
    val deviceId: String,          // IMEI / Serial
    val warrantyStatus: WarrantyStatus,
    val warrantyExpiryDate: String
)

enum class WarrantyStatus {
    ACTIVE,
    EXPIRED
}