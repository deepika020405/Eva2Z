package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.request.LoginRequest

class AuthRepository(private val api: ApiService) {
    suspend fun login(request: LoginRequest) = api.login(request)
    // suspend fun signup(request: SignupRequest) = api.signup(request)
}