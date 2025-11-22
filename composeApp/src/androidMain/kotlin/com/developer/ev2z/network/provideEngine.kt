package com.developer.ev2z.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.*

actual fun provideEngine(): HttpClientEngine = OkHttp.create()