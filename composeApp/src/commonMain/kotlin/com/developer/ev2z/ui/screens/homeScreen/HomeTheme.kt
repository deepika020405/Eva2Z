package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Palette ─────────────────────────────────────────────────────────────────
data class HomePalette(
    val pageBg: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSoft: Color,
    val heroGradStart: Color,
    val heroGradEnd: Color,
    val softShadow: Color,
    val isDark: Boolean,
)

val LightPalette = HomePalette(
    pageBg        = Color(0xFFF7F9FC),
    cardBg        = Color.White,
    textPrimary   = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    textMuted     = Color(0xFF94A3B8),
    borderSoft    = Color(0xFFE2E8F0),
    heroGradStart = Color(0xFFFFFFFF),
    heroGradEnd   = Color(0xFFEEF4FF),
    softShadow    = Color(0x14000000),
    isDark        = false,
)

// Dark theme uses the user-specified navy #0B1E3D
val DarkPalette = HomePalette(
    pageBg        = Color(0xFF0B1E3D),
    cardBg        = Color(0xFF132B52),
    textPrimary   = Color.White,
    textSecondary = Color(0xFFB6C2D6),
    textMuted     = Color(0xFF7A8AA8),
    borderSoft    = Color(0xFF1E3A66),
    heroGradStart = Color(0xFF0B1E3D),
    heroGradEnd   = Color(0xFF163561),
    softShadow    = Color(0x55000000),
    isDark        = true,
)

// ─── State container exposed via CompositionLocal ─────────────────────────────
data class HomeThemeState(
    val palette: HomePalette,
    val isDark: Boolean,
    val toggle: () -> Unit,
)

val LocalHomeTheme = staticCompositionLocalOf {
    HomeThemeState(LightPalette, false, {})
}

@Composable
fun rememberHomeThemeState(): HomeThemeState {
    var isDark by rememberSaveable { mutableStateOf(false) }
    val palette = if (isDark) DarkPalette else LightPalette
    return HomeThemeState(palette = palette, isDark = isDark) { isDark = !isDark }
}