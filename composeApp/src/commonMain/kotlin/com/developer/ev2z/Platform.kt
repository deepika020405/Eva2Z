package com.developer.ev2z

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform