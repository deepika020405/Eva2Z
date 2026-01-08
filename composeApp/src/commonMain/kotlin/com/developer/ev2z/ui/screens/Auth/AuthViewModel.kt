package com.developer.ev2z.viewmodel

import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.repository.AuthRepository
import com.developer.ev2z.data.local.StorageManager
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

    // OTP verified state
    private val _otpVerified = MutableStateFlow(false)
    val otpVerified: StateFlow<Boolean> = _otpVerified

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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


    fun checkPhoneRegistered(phone: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.login(LoginRequest(phone, "wrong_password"))
                // login chal gaya → user exists
                onResult(true)
            } catch (e: Exception) {
                // login fail → user not registered
                onResult(false)
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
                    _error.value = result.message
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

                        _loginSuccess.value = true

                    } catch (loginError: Exception) {
                        println("❌ Auto-login failed: ${loginError.message}")
                        _error.value = "Account created but login failed. Please login manually."
                    }
                } else {
                    println("❌ Password save failed: ${saveResult.message}")
                    _error.value = saveResult.message
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
                    _error.value = result.message
                }

            } catch (e: Exception) {
                println("❌ OTP verification error: ${e.message}")
                e.printStackTrace()
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
                    _error.value = result.message
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

                if (result.success) {
                    println("🔥 Forgot password OTP sent!")
                } else {
                    println("❌ Forgot password failed: ${result.message}")
                    _error.value = result.message
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
                    _error.value = result.message
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

    fun setError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }
}