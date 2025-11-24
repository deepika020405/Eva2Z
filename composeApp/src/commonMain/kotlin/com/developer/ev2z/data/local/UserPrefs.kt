package com.developer.ev2z.data.local

class UserPrefs(private val storage: StorageManager) {  // ✅ Storage → StorageManager

    fun saveUser(
        token: String,
        name: String,
        phone: String,
        email: String?,
        photo: String?
    ) {
        storage.saveString("token", token)
        storage.saveString("name", name)
        storage.saveString("phone", phone)
        email?.let { storage.saveString("email", it) }
        photo?.let { storage.saveString("photo", it) }
    }

    fun getToken() = storage.getString("token", "")
    fun getName() = storage.getString("name", "")
    fun getPhone() = storage.getString("phone", "")
    fun getEmail() = storage.getString("email", "")
    fun getPhoto() = storage.getString("photo", "")

    fun isLoggedIn() = getToken().isNotEmpty()

    fun clear() = storage.clear()
}