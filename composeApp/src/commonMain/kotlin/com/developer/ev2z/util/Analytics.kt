package com.developer.ev2z.util

expect object Analytics {
    fun logEvent(
        name: String,
        params: Map<String, Any?> = emptyMap()
    )
}


// commonMain
expect object MetaAnalytics {
    fun logEvent(
        name: String,
        params: Map<String, Any?> = emptyMap()
    )
}