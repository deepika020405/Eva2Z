package com.developer.ev2z

import com.developer.ev2z.ui.screens.Auth.formatFullName
import com.developer.ev2z.ui.screens.Auth.nameErrorFor
import com.developer.ev2z.util.displayNameFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The Full Name field used to strip every non-letter, so an e-mail address typed
 * into it was silently saved as the account name — "mukeshk86382@gmail.com"
 * became "Mukeshkgmailcom" (seen 2026-09-24).
 */
class SignupNameTest {

    @Test
    fun anEmailAddressIsRejected() {
        assertEquals(
            "Please enter your name, not your email address",
            nameErrorFor("mukeshk86382@gmail.com", submitAttempted = false),
        )
        // Even without the @ — a pasted "mukeshk gmail.com" is not a name either.
        assertEquals(
            "Please enter your name, not your email address",
            nameErrorFor("mukeshk gmail.com", submitAttempted = false),
        )
    }

    @Test
    fun digitsAndEmptyNamesAreRejected() {
        assertEquals("Your name can't contain numbers", nameErrorFor("Mukesh 86382", submitAttempted = false))
        assertEquals("Please enter a valid name", nameErrorFor("!!!", submitAttempted = false))
        assertEquals("Please enter your full name", nameErrorFor("M", submitAttempted = false))
    }

    @Test
    fun blankOnlyComplainsAfterSubmit() {
        assertNull(nameErrorFor("", submitAttempted = false))
        assertEquals("Please enter your full name", nameErrorFor("   ", submitAttempted = true))
    }

    @Test
    fun realNamesPass() {
        listOf("Mukesh Kumar", "O'Brien", "Jean-Luc Picard", "Dr. Rashmi Rani").forEach {
            assertNull(nameErrorFor(it, submitAttempted = true), it)
        }
    }

    @Test
    fun acceptedNamesAreTitleCasedWithoutLosingPunctuation() {
        assertEquals("Mukesh Kumar", formatFullName("  mukesh   kumar "))
        assertEquals("O'Brien", formatFullName("o'brien"))
        assertEquals("Jean-Luc", formatFullName("jean-luc"))
        // Capitalisation the user meant survives.
        assertEquals("McDonald", formatFullName("McDonald"))
    }

    @Test
    fun anEmailStoredAsTheAccountNameIsNotGreetedAsOne() {
        // The home header reads whatever the server has; these accounts exist.
        assertEquals("Mukeshk", displayNameFrom("mukeshk86382@gmail.com"))
        assertEquals("Rashmi Rani", displayNameFrom("rashmi.rani@example.com"))
        assertEquals("Mukesh Kumar", displayNameFrom("Mukesh Kumar"))
        assertEquals("User", displayNameFrom(null))
        assertEquals("User", displayNameFrom("   "))
        // All-digit local part: keep what there is rather than inventing "User".
        assertEquals("86382", displayNameFrom("86382@gmail.com"))
    }
}
