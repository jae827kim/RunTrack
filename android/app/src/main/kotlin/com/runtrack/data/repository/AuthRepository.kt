package com.runtrack.data.repository

import com.runtrack.data.api.*
import com.runtrack.data.models.*

class AuthRepository(private val service: UserService = RetrofitClient.userService) {
    suspend fun login(username: String, password: String) {
        val token = service.login(LoginRequest(username, password)).requiredBody()
        val user = service.getProfile("Bearer ${token.accessToken}").requiredBody()
        ApiSession.start(token, user)
    }

    suspend fun register(username: String, password: String, email: String, weight: Float) {
        service.register(RegisterRequest(username, email, password, weightKg = weight)).requiredBody()
    }
}
