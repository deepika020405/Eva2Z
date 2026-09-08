package com.developer.ev2z.ui.screens.fastag

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen-size adaptation for the FASTag screens.
 *
 * Same idea as `fuelDimensFor` in FuelPriceScreen: a 375dp-wide phone is the design
 * baseline, everything else scales off it and is clamped so an iPhone SE stays legible
 * and an iPad does not balloon.
 *
 * The scale is applied by overriding [LocalDensity] for the whole screen rather than by
 * threading a dimens object through every private composable — the FASTag screens carry
 * a few hundred fixed `dp`/`sp` literals and one density override moves all of them
 * consistently. Window insets are unaffected: `statusBarsPadding()` and friends work in
 * platform pixels, so the safe-area gaps stay physically correct on a notched iPhone.
 *
 * Every field below is already expressed in the *scaled* unit space, so it can be used
 * directly inside [FastagResponsiveRoot] without further conversion.
 */
internal data class FastagDimens(
    /** Density multiplier applied to the subtree. */
    val scale: Float,
    /** Horizontal page gutter. */
    val hPad: Dp,
    /** Content column cap — keeps line lengths sane on tablets and landscape. */
    val contentMaxWidth: Dp,
    /** Ceiling for the issuer list inside the bottom sheet. */
    val sheetListMaxHeight: Dp,
    /** Amount presets per row — four chips do not fit on a 320dp-wide phone. */
    val presetsPerRow: Int,
    val isCompact: Boolean,
    val isTablet: Boolean
)

internal val LocalFastagDimens = staticCompositionLocalOf {
    FastagDimens(
        scale = 1f,
        hPad = 16.dp,
        contentMaxWidth = Dp.Infinity,
        sheetListMaxHeight = 320.dp,
        presetsPerRow = 4,
        isCompact = false,
        isTablet = false
    )
}

internal fun fastagDimensFor(maxWidth: Dp, maxHeight: Dp): FastagDimens {
    // 375dp ≈ a standard phone. Clamped on both ends: below ~0.88 the payment figures
    // stop being readable, above ~1.25 a tablet just looks like a zoomed phone.
    val scale = (maxWidth / 375.dp).coerceIn(0.88f, 1.25f)

    val isTablet = maxWidth >= 600.dp
    val isCompact = maxWidth < 360.dp

    // Raw (unscaled) values first, then divided by the scale because they are consumed
    // inside the density-scaled subtree.
    val rawHPad = when {
        isTablet -> 28.dp
        isCompact -> 14.dp
        else -> 18.dp
    }
    val rawContentMax = if (isTablet) 620.dp else Dp.Infinity

    return FastagDimens(
        scale = scale,
        hPad = rawHPad / scale,
        contentMaxWidth = if (rawContentMax == Dp.Infinity) Dp.Infinity else rawContentMax / scale,
        // Never more than ~45% of the window: the sheet also carries a title, a hint
        // line and a search field, and on a short screen a taller list pushed its rows
        // off the bottom edge with no way to reach them.
        sheetListMaxHeight = (maxHeight * 0.45f) / scale,
        presetsPerRow = if (isCompact) 3 else 4,
        isCompact = isCompact,
        isTablet = isTablet
    )
}

/**
 * Measures the window, derives [FastagDimens] and installs the scaled density for the
 * subtree. Callers get the dimens back so they can size the few things that are decided
 * per-screen rather than per-widget (page gutter, content cap, sheet height).
 */
@Composable
internal fun FastagResponsiveRoot(
    modifier: Modifier = Modifier,
    content: @Composable (FastagDimens) -> Unit
) {
    BoxWithConstraints(modifier) {
        val dimens = fastagDimensFor(maxWidth, maxHeight)
        val base = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * dimens.scale, base.fontScale),
            LocalFastagDimens provides dimens
        ) {
            content(dimens)
        }
    }
}

/** `widthIn(max = …)` that tolerates an unbounded cap. */
internal fun Modifier.fastagContentWidth(dimens: FastagDimens): Modifier =
    if (dimens.contentMaxWidth == Dp.Infinity) this
    else this.widthIn(max = dimens.contentMaxWidth)