package com.developer.ev2z

import com.developer.ev2z.util.mapServerMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `save-password` answers a taken e-mail with the raw MySQL error (seen 2026-09-24),
 * which must never reach the screen.
 */
class ServerMessageTest {

    private val duplicateEmail =
        "SQLSTATE[23000]: Integrity constraint violation: 1062 Duplicate entry " +
            "'devhimanshu780@gmail.com' for key 'users.users_email_unique' " +
            "(Connection: mysql, SQL: insert into `users` (`name`, `userName`, `phone`, " +
            "`address`, `email`, `password`, `otp_verified`) values (Himanshu, ?, " +
            "+919755273822, ?, devhimanshu780@gmail.com, \$2y\$10\$qZKdCbH8ORJn, 1))"

    @Test
    fun duplicateEmailNamesTheEmail() {
        val shown = mapServerMessage(duplicateEmail)
        assertTrue(shown.startsWith("This email is already registered"), shown)
        // Nothing of the query, the hash or the driver leaks through.
        assertTrue(!shown.contains("SQL") && !shown.contains("\$2y\$"), shown)
    }

    @Test
    fun duplicatePhoneNamesThePhone() {
        val raw = "SQLSTATE[23000]: 1062 Duplicate entry '+919755273822' for key " +
            "'users.users_phone_unique' (Connection: mysql, SQL: insert into `users` " +
            "(`email`, `phone`) values (a@b.com, +919755273822))"
        assertTrue(mapServerMessage(raw).startsWith("This phone number is already registered"))
    }

    @Test
    fun otherServerInternalsBecomeTheFallback() {
        val raw = "Symfony\\Component\\HttpKernel\\Exception\\NotFoundHttpException in " +
            "/var/www/evatoz_gps_server/vendor/laravel/framework/src/Illuminate/Routing.php"
        assertEquals("Please try again.", mapServerMessage(raw, fallback = "Please try again."))
    }

    @Test
    fun ordinaryServerMessagesPassThrough() {
        assertEquals("Invalid OTP.", mapServerMessage("Invalid OTP."))
        assertEquals("fallback", mapServerMessage(null, fallback = "fallback"))
        assertEquals("fallback", mapServerMessage("   ", fallback = "fallback"))
    }
}