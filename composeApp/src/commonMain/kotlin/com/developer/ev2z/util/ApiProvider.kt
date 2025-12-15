package com.developer.ev2z.util

import com.developer.ev2z.network.ApiService

object ApiProvider {
    val apiService: ApiService by lazy {
        ApiService()
    }
}