package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the token currently in storage belongs to a session the server still accepts.
 *
 * A token restored from storage at app start is *unverified* — it may have been revoked while
 * the app was closed. Registering for push on an unverified session is what kept re-arming this
 * device after a logout: `fcm/register` answers 200 even when every other endpoint answers 401,
 * so a dead session was still enough to put the phone back on the server's push list.
 */
object SessionState {

    private val _verified = MutableStateFlow(false)

    /** True only once an authenticated call has come back 2xx for the token in use. */
    val verified: StateFlow<Boolean> = _verified.asStateFlow()

    private var _generation = 0

    /**
     * Bumped on every logout / session expiry. Background work that started under an older
     * generation must not be allowed to land — see [FcmRegistrar].
     */
    val generation: Int get() = _generation

    fun markVerified() {
        _verified.value = true
    }

    fun invalidate() {
        _verified.value = false
        _generation += 1
    }
}