// androidMain/utils/UrlEncoder.android.kt
package com.developer.ev2z.util

import java.net.URLEncoder

actual fun encodeUrl(value: String): String {
    return URLEncoder.encode(value, "UTF-8")
}
