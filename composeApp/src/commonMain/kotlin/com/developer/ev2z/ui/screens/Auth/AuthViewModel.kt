package com.developer.ev2z.viewmodel

import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.repository.AuthRepository
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.util.mapServerMessage
import com.developer.ev2z.util.mapToUserError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    private val storage: StorageManager
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private val userPrefs = UserPrefs(storage)

    // Loading state
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // Login success state
    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    // Signup success state
    private val _signupSuccess = MutableStateFlow(false)
    val signupSuccess: StateFlow<Boolean> = _signupSuccess

    // Phone OTP verified state
    private val _otpVerified = MutableStateFlow(false)
    val otpVerified: StateFlow<Boolean> = _otpVerified

    // Email OTP verified state
    private val _emailOtpVerified = MutableStateFlow(false)
    val emailOtpVerified: StateFlow<Boolean> = _emailOtpVerified

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Rate-limit countdown (seconds remaining before user can resend OTP)
    private val _rateLimitCountdown = MutableStateFlow(0)
    val rateLimitCountdown: StateFlow<Int> = _rateLimitCountdown

    // User info state
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName



    // ============================================
    // LOGIN
    // ============================================
    fun login(request: LoginRequest) {
        println("🔥 Login started with phone: ${request.phone}")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                println("🔥 Making API call...")
                val result = repository.login(request)

                println("🔥 Login successful!")
                println("🔥 Token: ${result.token}")
                println("🔥 User: ${result.user.name}")

                // Save token and user data
                userPrefs.saveUser(
                    token = result.token,
                    name = result.user.name,
                    phone = result.user.phone,
                    email = result.user.email ?: "",
                    photo = result.user.photo ?: ""
                )

                println("🔥 Token saved to storage")

                _loginSuccess.value = true
                println("🔥 Login success set to true")

                _userName.value = result.user.name

            } catch (e: Exception) {
                println("❌ Error: ${e.message}") // 👈 keep raw error ONLY for logs
                e.printStackTrace()

                _error.value = mapToUserError(e) // 👈 SAFE message for UI
            } finally {
                _loading.value = false
                println("🔥 Loading set to false")
            }
        }
    }

    // ============================================
    // CHECK IF PHONE IS REGISTERED - ✅ USING REGISTER API
    // ============================================


//    fun checkPhoneRegistered(phone: String, onResult: (Boolean) -> Unit) {
//        viewModelScope.launch {
//            try {
//                repository.login(LoginRequest(phone, "wrong_password"))
//                // login chal gaya → user exists
//                onResult(true)
//            } catch (e: Exception) {
//                // login fail → user not registered
//                onResult(false)
//            }
//        }
//    }

    fun checkPhoneRegistered(phone: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.login(LoginRequest(phone, "wrong_password"))
                onResult(true)

            } catch (e: Exception) {

                val msg = e.message?.lowercase() ?: ""

                when {
                    // Network/connectivity errors — don't treat as "registered"
                    msg.contains("unable to resolve host") ||
                            msg.contains("no address associated") ||
                            msg.contains("failed to connect") ||
                            msg.contains("econnrefused") ||
                            msg.contains("timeout") ||
                            msg.contains("unknownhost") ||
                            msg.contains("no route to host") -> {
                        _error.value = "No internet connection. Please check your connection and try again."
                    }

                    msg.contains("invalid password") ||
                            msg.contains("wrong password") ||
                            msg.contains("incorrect password") -> {
                        onResult(true) // user exists
                    }

                    msg.contains("user not registered") ||
                            msg.contains("not registered") -> {
                        onResult(false)
                    }

                    else -> {
                        // Any other server error (e.g. 401 with unknown message) →
                        // assume the user exists (signup API will confirm if not)
                        onResult(true)
                    }
                }
            }
        }
    }




    // ============================================
    // SIGN UP
    // ============================================
    fun signUp(name: String, phone: String, password: String, email: String) {
        println("🔥 SignUp - Sending OTP for: $phone")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                println("🔥 Calling registerUser API...")
                val result = repository.signUp(name, phone, "", email) // Empty password

                if (result.success) {
                    println("🔥 OTP sent successfully! Message: ${result.message}")
                    _signupSuccess.value = true
                } else {
                    println("❌ SignUp failed: ${result.message}")
                    _error.value = mapServerMessage(result.message, fallback = "SignUp failed. Please try again.")
                }

            } catch (e: Exception) {
                println("❌ SignUp error: ${e.message}")
                e.printStackTrace()
//                _error.value = e.message ?: "SignUp failed"
                _error.value = mapToUserError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================
    // SAVE PASSWORD & AUTO-LOGIN (After OTP verification)
    // ============================================
    fun savePasswordAndLogin(name: String, phone: String, password: String, email: String) {
        println("🔥 Saving password and auto-logging in...")

        if (_loading.value) return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                // Step 1: Save password
                // 🔥 Wait before savePassword
                kotlinx.coroutines.delay(500)

                val saveResult = repository.savePassword(phone, password)

                if (saveResult.status) {
                    println("✅ Password saved! Now auto-logging in...")

                    // Step 2: Auto-login
                    kotlinx.coroutines.delay(500)

                    try {
                        val loginResult = repository.login(LoginRequest(phone, password))

                        println("✅ Auto-login successful!")
                        println("Token: ${loginResult.token}")

                        // Save user data
                        userPrefs.saveUser(
                            token = loginResult.token,
                            name = loginResult.user.name,
                            phone = loginResult.user.phone,
                            email = loginResult.user.email ?: "",
                            photo = loginResult.user.photo ?: ""
                        )

                        // This login is the completion of a brand-new registration →
                        // mark it so the app shows onboarding only to first-time users.
                        userPrefs.saveString("is_new_user", "true")

                        _loginSuccess.value = true

                    } catch (loginError: Exception) {
                        println("❌ Auto-login failed: ${loginError.message}")
                        _error.value = "Account created but login failed. Please login manually."
                    }
                } else {
                    println("❌ Password save failed: ${saveResult.message}")
                    // `save-password` answers a taken e-mail with the raw MySQL
                    // duplicate-key error, INSERT statement and all — never show that.
                    _error.value = mapServerMessage(
                        saveResult.message,
                        fallback = "We couldn't finish creating your account. Please try again.",
                    )
                }

            } catch (e: Exception) {

                val userMessage = when {
                    e.message?.contains("Failed to connect", true) == true ||
                            e.message?.contains("ECONNREFUSED", true) == true ->
                        "Unable to connect. Please check your internet connection."

                    e.message?.contains("timeout", true) == true ->
                        "Request timed out. Please try again."

                    else ->
                        "Something went wrong. Please try again."
                }

                _error.value = mapToUserError(e)

            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================
    // VERIFY OTP (for registration)
    // ============================================
    fun verifyOtp(phone: String, otp: String) {
        println("🔥 Verifying OTP for phone: $phone")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = repository.verifyOtp(phone, otp)

                if (result.success) {
                    println("🔥 OTP verified successfully!")
                    _otpVerified.value = true
                } else {
                    println("❌ OTP verification failed: ${result.message}")

                    // 🔥 MOST IMPORTANT FIX
                    _error.value = mapServerMessage(
                        result.message,
                        fallback = "Invalid OTP. Please try again.",
                    )
                }

            } catch (e: Exception) {
                println("❌ OTP verification error: ${e.message}")
                e.printStackTrace()

                // already safe
                _error.value = mapToUserError(e)

            } finally {
                _loading.value = false
            }
        }
    }


    // ============================================
    // VERIFY EMAIL OTP
    // ============================================
    fun verifyEmailOtp(phone: String, email: String, otp: String) {
        println("🔥 Verifying Email OTP for: $email (phone: $phone)")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = repository.verifyEmailOtp(phone, email, otp)

                if (result.success) {
                    println("🔥 Email OTP verified successfully!")
                    _emailOtpVerified.value = true
                } else {
                    _error.value = mapServerMessage(
                        result.message,
                        fallback = "Invalid OTP. Please try again.",
                    )
                }

            } catch (e: Exception) {
                println("❌ Email OTP verification error: ${e.message}")
                _error.value = mapToUserError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================
    // RESEND OTP
    // ============================================
    fun resendOtp(phone: String) {
        println("🔥 Resending OTP for phone: $phone")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = repository.resendOtp(phone)

                if (result.success) {
                    println("🔥 OTP resent successfully!")
                } else {
                    println("❌ Resend OTP failed: ${result.message}")
                    _error.value = mapServerMessage(result.message, fallback = "Couldn't resend the OTP. Please try again.")
                }

            } catch (e: Exception) {
                println("❌ Resend OTP error: ${e.message}")
                e.printStackTrace()
                _error.value = mapToUserError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================
    // FORGOT PASSWORD
    // ============================================
    fun forgotPassword(phone: String) {
        println("🔥 Forgot password for phone: $phone")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = repository.forgotPassword(phone)

                when {
                    result.success -> {
                        println("🔥 Forgot password OTP sent!")
                    }
                    result.rateLimited -> {
                        println("⏳ Rate limited — starting 60s countdown")
                        _loading.value = false
                        for (i in 60 downTo 1) {
                            _rateLimitCountdown.value = i
                            kotlinx.coroutines.delay(1000)
                        }
                        _rateLimitCountdown.value = 0
                        return@launch
                    }
                    else -> {
                        println("❌ Forgot password failed: ${result.message}")
                        _error.value = mapServerMessage(result.message)
                    }
                }

            } catch (e: Exception) {
                println("❌ Forgot password error: ${e.message}")
                e.printStackTrace()
                _error.value = mapToUserError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================
    // CANCEL FORGOT PASSWORD SESSION
    // ============================================
    fun cancelForgotPasswordSession(phone: String) {
        viewModelScope.launch {
            try {
                repository.cancelForgotPassword(phone)
            } catch (e: Exception) {
                println("❌ Cancel forgot password session failed: ${e.message}")
            }
        }
    }

    // ============================================
    // VERIFY FORGOT PASSWORD OTP
    // ============================================
    fun verifyForgotOtp(phone: String, otp: String) {
        println("🔥 Verifying forgot password OTP for phone: $phone")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = repository.verifyForgotOtp(phone, otp)

                if (result.success) {
                    println("🔥 Forgot password OTP verified!")
                    _otpVerified.value = true
                } else {
                    println("❌ Forgot password OTP verification failed: ${result.message}")
                    _error.value = mapServerMessage(result.message, fallback = "Invalid OTP. Please try again.")
                }

            } catch (e: Exception) {
                println("❌ Forgot password OTP verification error: ${e.message}")
                e.printStackTrace()
                _error.value = mapToUserError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================
    // RESET PASSWORD (with Auto-Login)
    // ============================================
    fun resetPassword(phone: String, password: String, confirmPassword: String) {
        println("🔥 Resetting password for phone: $phone")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = repository.resetPassword(phone, password, confirmPassword)

                if (result.success) {
                    println("🔥 Password reset successful!")

                    // ✅ AUTO-LOGIN after password reset
                    println("🔐 Starting auto-login after password reset...")
                    kotlinx.coroutines.delay(500)

                    try {
                        val loginResult = repository.login(LoginRequest(phone, password))

                        println("✅ Auto-login successful!")
                        println("Token: ${loginResult.token}")

                        // Save user data
                        userPrefs.saveUser(
                            token = loginResult.token,
                            name = loginResult.user.name,
                            phone = loginResult.user.phone,
                            email = loginResult.user.email ?: "",
                            photo = loginResult.user.photo ?: ""
                        )

                        _loginSuccess.value = true

                    } catch (loginError: Exception) {
                        println("❌ Auto-login failed: ${loginError.message}")
                        // Don't show error - password was reset successfully
                        _error.value = mapToUserError(loginError)
                    }

                } else {
                    println("❌ Password reset failed: ${result.message}")
//                    _error.value = result.message
                }

            } catch (e: Exception) {
                println("❌ Password reset error: ${e.message}")
                e.printStackTrace()
//                _error.value = e.message ?: "Failed to reset password"
                _error.value = mapToUserError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================
    // STATE RESET FUNCTIONS
    // ============================================
    fun setLoginSuccess(value: Boolean) {
        _loginSuccess.value = value
    }

    fun resetLoginSuccess() {
        _loginSuccess.value = false
    }

    fun resetSignupSuccess() {
        _signupSuccess.value = false
    }

    fun resetOtpVerified() {
        _otpVerified.value = false
    }

    fun resetEmailOtpVerified() {
        _emailOtpVerified.value = false
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }
}