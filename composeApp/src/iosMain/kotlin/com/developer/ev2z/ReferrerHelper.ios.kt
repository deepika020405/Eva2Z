package com.developer.ev2z

// iosMain/com/developer/ev2z/ReferrerHelper.ios.kt
actual fun fetchReferrerIfNeeded(onComplete: () -> Unit) {
    onComplete() // iOS mein referrer nahi hota
}