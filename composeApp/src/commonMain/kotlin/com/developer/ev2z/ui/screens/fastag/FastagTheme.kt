package com.developer.ev2z.ui.screens.fastag

import androidx.compose.ui.graphics.Color

/**
 * FASTag palette. Values are lifted directly from HomeScreen.kt so the feature reads
 * as part of the same app rather than a bolted-on payments flow.
 *
 * The only addition is [FastagBlue], used for the tag/toll identity so a FASTag card is
 * distinguishable at a glance from the green GPS cards and the amber e-Challan card.
 * Semantic colours (success / warn / danger) are reused from the existing set.
 */
internal val BrandGreen      = Color(0xFF00A859)
internal val BrandGreenDark  = Color(0xFF007A40)
internal val BrandGreenLight = Color(0xFFE6F7EF)

internal val CardWhite       = Color.White
internal val TextPrimary     = Color(0xFF111827)
internal val TextSecondary   = Color(0xFF6B7280)
internal val TextHint        = Color(0xFFADB5BD)

internal val ShadowColor     = Color(0x14000000)
internal val DividerSoft     = Color(0xFFEEF0F3)
internal val FieldBorder     = Color(0xFFE2E5EA)
internal val FieldBorderFocus = BrandGreen

/** FASTag / toll identity. */
internal val FastagBlue      = Color(0xFF2F6FED)
internal val FastagBlueLight = Color(0xFFEAF1FE)

/** Semantic — same values already used across HomeScreen. */
internal val WarnAmber       = Color(0xFFF59E0B)
internal val WarnAmberLight  = Color(0xFFFEF6E7)
internal val DangerRed       = Color(0xFFE53935)
internal val DangerRedLight  = Color(0xFFFDECEC)
internal val SuccessGreen    = BrandGreen
internal val SuccessGreenLight = BrandGreenLight

/** Greys for locked / disabled states, matching HomeScreen's locked palette. */
internal val MutedIcon       = Color(0xFF9CA3AF)
internal val MutedIconBg     = Color(0xFFE9ECEF)
internal val MutedText       = Color(0xFF9CA3AF)
internal val MutedCardBg     = Color(0xFFF3F4F6)