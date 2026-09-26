package com.developer.ev2z.util

/**
 * What to greet the account holder by, given whatever the server has stored as
 * their name.
 *
 * Some accounts were created with an e-mail address in the name field — the signup
 * form used to accept one — so the stored name can be "mukeshk86382@gmail.com" or,
 * once the old form had stripped it, "Mukeshkgmailcom". Neither should be printed
 * on the home screen as if it were a name, so an address is reduced to its local
 * part with the separators and digits taken out: mukeshk86382@gmail.com → Mukeshk.
 *
 * This only changes what is displayed. The name on the account stays wrong until
 * it is corrected in Edit Profile — which is the only real fix for one that is
 * already mangled beyond recognition.
 */
fun displayNameFrom(raw: String?, fallback: String = "User"): String {
    val name = raw?.trim().orEmpty()
    if (name.isEmpty()) return fallback

    // Everything after "@" is a mail host, never part of a person's name.
    val local = name.substringBefore('@')

    val cleaned = local
        .replace(Regex("[._+\\-]+"), " ")
        .filter { it.isLetter() || it == ' ' || it == '\'' }
        .replace(Regex("\\s+"), " ")
        .trim()

    // A local part that was all digits leaves nothing to show; the raw one is still
    // better than "User", so fall back through rather than dropping the name.
    val best = cleaned.ifBlank { local.trim() }.ifBlank { return fallback }

    return best.split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }
    }
}