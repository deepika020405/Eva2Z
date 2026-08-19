package com.developer.ev2z.ui.screens.DriverScore

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.developer.ev2z.theme.AppPageBg

// ─────────────────────────────────────────────────────────────────────────────
//  Eva2z Design System — Professional Minimal
//  Rule: ONE accent color (green). Everything else is neutral.
//  No rainbow. No heavy gradients. Clean. Sharp. Modern.
// ─────────────────────────────────────────────────────────────────────────────

object Eva2zColors {

    // ─── ONE Accent — App's green, nothing else ───────────────────────────────
    val Green              = Color(0xFF1BAF6B)   // primary — buttons, active states
    val GreenSubtle        = Color(0xFF22C57A)   // hero gradient only
    val GreenLight         = Color(0xFFF0FAF5)   // chip / tag bg
    val GreenMid           = Color(0xFFD6F0E5)   // chip border / divider
    val GreenDark          = Color(0xFF159558)   // pressed, "View Details" text
    // Aliases kept for compatibility
    val GreenBright        = Color(0xFF5DD39E)
    val GreenGradientStart = Color(0xFF22C57A)
    val GreenGradientEnd   = Color(0xFF1BAF6B)

    // ─── Backgrounds — pure white world ──────────────────────────────────────
    val Background  = AppPageBg   // page bg — barely off-white
    val Surface     = Color(0xFFFFFFFF)   // card bg — pure white
    val SurfaceAlt  = Color(0xFFFBFBFB)   // table alt row

    // ─── Text — neutral, not black ────────────────────────────────────────────
    val TextPrimary   = Color(0xFF18181B)   // zinc-900 — sharp but not #000
    val TextSecondary = Color(0xFF71717A)   // zinc-500 — mid grey
    val TextMuted     = Color(0xFFA1A1AA)   // zinc-400 — soft labels

    // ─── Borders — invisible unless needed ───────────────────────────────────
    val Border       = Color(0xFFE4E4E7)   // zinc-200
    val BorderStrong = Color(0xFFD4D4D8)   // zinc-300

    // ─── Semantic — all DESATURATED, no rainbow ───────────────────────────────

    // Success = same as green accent
    val Success      = Color(0xFF1BAF6B)
    val SuccessLight = Color(0xFFF0FAF5)

    // Warning = warm grey-amber (NOT bright yellow)
    val Warning      = Color(0xFFB45309)   // amber-700 — dark enough to read on white
    val WarningLight = Color(0xFFFFFBEB)   // amber-50

    // Error = muted rose (NOT bright red)
    val Error        = Color(0xFFDC2626)   // red-600 — clear but not neon
    val ErrorLight   = Color(0xFFFEF2F2)   // red-50

    // Info = slate-blue (NOT electric blue)
    val Info         = Color(0xFF3B5BDB)   // indigo
    val InfoLight    = Color(0xFFEEF2FF)

    // ─── Grade — MONOCHROME FIRST, accent only for good/excellent ─────────────
    // Excellent → Green (reward best behavior with brand color)
    // Good      → Green (same — both positive)
    // Average   → Amber (neutral warning)
    // Poor      → Red (clear danger)
    fun gradeColor(grade: ScoreGrade): Color = when (grade) {
        ScoreGrade.EXCELLENT -> Color(0xFF1BAF6B)   // green
        ScoreGrade.GOOD      -> Color(0xFF1BAF6B)   // green
        ScoreGrade.AVERAGE   -> Color(0xFFD97706)   // amber-600
        ScoreGrade.POOR      -> Color(0xFFDC2626)   // red-600
    }

    fun gradeColorLight(grade: ScoreGrade): Color = when (grade) {
        ScoreGrade.EXCELLENT -> Color(0xFFF0FAF5)
        ScoreGrade.GOOD      -> Color(0xFFF0FAF5)
        ScoreGrade.AVERAGE   -> Color(0xFFFFFBEB)
        ScoreGrade.POOR      -> Color(0xFFFEF2F2)
    }

    // ─── Dark mode (future) ───────────────────────────────────────────────────
    val DarkBg      = Color(0xFF0B1220)
    val DarkSurface = Color(0xFF111827)
    val DarkBorder  = Color(0xFF1F2937)
}

object Eva2zSpacing {
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
}

object Eva2zRadius {
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val pill = 50.dp
}

object Eva2zElevation {
    val card    = 0.dp   // flat — no shadow, border instead
    val modal   = 4.dp
    val overlay = 8.dp
}