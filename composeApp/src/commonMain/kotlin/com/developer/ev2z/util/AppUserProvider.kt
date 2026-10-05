package com.developer.ev2z.util

/**
 * Identity of the signed-in user, for common code that has no UserPrefs at hand.
 *
 * Wired once in `App.kt` from UserPrefs, the same way [AppTokenProvider] is.
 *
 * ⚠️ Do NOT derive the user id from the Sanctum token: "6639|abc…" carries the
 * *personal access token* id, not the user id. Using it made the community feed
 * offer Edit/Delete on other people's posts, which the API answered with
 * 403 {"error":"Not authorized"}.
 */
object AppUserProvider {
    /** Real user id from the login response; 0 when unknown (session predates storing it). */
    var getUserId: () -> Int = { 0 }

    /** Stored display name — the fallback identity when the id is unknown. */
    var getUserName: () -> String = { "" }
}