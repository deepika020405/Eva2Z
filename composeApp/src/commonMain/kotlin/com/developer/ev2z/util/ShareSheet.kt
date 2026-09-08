package com.developer.ev2z.util

/**
 * Opens the OS share sheet with [text] as the payload.
 *
 * [subject] is only used by targets that have one (email, some note apps) and is
 * ignored by the rest — the body always carries the full message, so nothing is
 * lost when a target drops it.
 *
 * Distinct from [com.developer.ev2z.ui.screens.ReferAndEarn.shareViaOther], which
 * hardcodes referral wording in its subject and chooser title.
 */
expect fun shareText(text: String, subject: String)

/**
 * Public store listings for this app, safe to paste into a shared message.
 *
 * These are deliberately HTTPS and not the `market://` / `itms-apps://` schemes
 * that [com.developer.ev2z.ui.screens.Profile.getPlatformStoreUrl] returns — those
 * only resolve on a device that already has the matching store app, so they break
 * the moment the link travels through WhatsApp to someone else.
 */
const val PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=com.developer.ev2z"
const val APP_STORE_LINK = "https://apps.apple.com/app/id6760705704"

/**
 * The install footer appended to anything shared out of the app. Both stores are
 * listed because the recipient's platform is unknowable from here.
 */
val INSTALL_FOOTER: String = buildString {
    appendLine("📲 Get Eva2z Connect for live fuel prices, GPS tracking & more:")
    appendLine("Android: $PLAY_STORE_LINK")
    append("iPhone: $APP_STORE_LINK")
}