package com.developer.ev2z.data.local

class UserPrefs(private val storage: Storage) {

    suspend fun saveUser(
        token: String,
        name: String,
        phone: String,
        email: String?,
        photo: String?
    ) {
        storage.putString("token", token)
        storage.putString("name", name)
        storage.putString("phone", phone)
        storage.putString("email", email)
        storage.putString("photo", photo)
    }

    suspend fun getToken() = storage.getString("token")
    suspend fun getName() = storage.getString("name")
    suspend fun getPhone() = storage.getString("phone")
    suspend fun getEmail() = storage.getString("email")
    suspend fun getPhoto() = storage.getString("photo")

    suspend fun clear() = storage.clear()
}