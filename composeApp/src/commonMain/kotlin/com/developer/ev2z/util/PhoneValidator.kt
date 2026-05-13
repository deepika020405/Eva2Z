package com.developer.ev2z.utils

object PhoneValidator {
    fun isValid(phoneNumber: String, dialCode: String, phoneLength: Int): Boolean {
        return if (dialCode == "+91") {
            phoneNumber.length == phoneLength && phoneNumber.firstOrNull() in listOf('6', '7', '8', '9')
        } else {
            phoneNumber.length == phoneLength
        }
    }

    fun errorMessage(dialCode: String, phoneLength: Int): String {
        return if (dialCode == "+91") {
            "India phone number must be 10 digits and start with 6, 7, 8, or 9"
        } else {
            "Enter a valid $phoneLength-digit phone number"
        }
    }
}