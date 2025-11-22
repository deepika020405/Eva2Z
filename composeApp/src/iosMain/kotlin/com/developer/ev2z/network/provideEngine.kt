package com.developer.ev2z.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.*

actual fun provideEngine(): HttpClientEngine = Darwin.create()