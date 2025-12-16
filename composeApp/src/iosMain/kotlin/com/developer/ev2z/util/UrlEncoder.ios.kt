// iosMain/utils/UrlEncoder.ios.kt
package com.developer.ev2z.util

import platform.Foundation.NSCharacterSet
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.Foundation.NSString
import platform.Foundation.create

actual fun encodeUrl(value: String): String {
    val nsString = NSString.create(string = value)

    return nsString
        .stringByAddingPercentEncodingWithAllowedCharacters(
            NSCharacterSet.URLQueryAllowedCharacterSet
        ) ?: value
}
