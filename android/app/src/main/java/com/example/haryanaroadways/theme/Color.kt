package com.example.haryanaroadways.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Luxury Brand Palette (State Express Emerald & Warm Saffron) ───────
val PrimaryTeal = Color(0xFF0F766E)       // Emerald Teal 700
val PrimaryTealDark = Color(0xFF042F2C)   // Deep Pine 950
val PrimaryTealMedium = Color(0xFF0D5C54) // Forest 800
val PrimaryTealLight = Color(0xFF14B8A6)  // Vibrant Mint 500
val PrimaryTealUltraLight = Color(0xFFCCFBF1) // Pale Cyan 100

val AccentSaffron = Color(0xFFF59E0B)     // Amber / Saffron 500
val AccentSaffronDark = Color(0xFFB45309) // Deep Bronze 700
val AccentSaffronLight = Color(0xFFFEF3C7)// Warm Cream 100
val AccentGold = Color(0xFFEAB308)

// ── Neutral Surfaces & Backgrounds ────────────────────────────────────
val BgLight = Color(0xFFF8FAFC)           // Slate 50
val BgDark = Color(0xFF0B1320)            // Slate 950
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF131D2E)
val SurfaceCardElevated = Color(0xFFFFFFFF)
val BorderSubtle = Color(0xFFE2E8F0)      // Slate 200
val BorderFocus = Color(0xFF0F766E)

// ── Typography Colors ─────────────────────────────────────────────────
val TextPrimaryLight = Color(0xFF0F172A)  // Slate 900
val TextSecondaryLight = Color(0xFF475569)// Slate 600
val TextMutedLight = Color(0xFF94A3B8)    // Slate 400

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)

// ── Semantic Colors ───────────────────────────────────────────────────
val SuccessGreen = Color(0xFF059669)      // Emerald 600
val SuccessGreenLight = Color(0xFFD1FAE5) // Emerald 100
val ErrorRed = Color(0xFFDC2626)          // Red 600
val ErrorRedLight = Color(0xFFFEE2E2)     // Red 100
val WarningYellow = Color(0xFFD97706)     // Amber 600
val WarningYellowLight = Color(0xFFFEF3C7)
val InfoBlue = Color(0xFF2563EB)          // Blue 600
val InfoBlueLight = Color(0xFFDBEAFE)     // Blue 100

// ── Premium Gradient Brushes ──────────────────────────────────────────
val BrandHeroGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF032623),
        Color(0xFF064E3B),
        Color(0xFF0F766E)
    )
)

val BrandCardGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF064E3B),
        Color(0xFF0F766E)
    )
)

val GoldAccentGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFF59E0B),
        Color(0xFFD97706)
    )
)

val ShimmerGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF1F5F9),
        Color(0xFFE2E8F0),
        Color(0xFFF1F5F9)
    )
)
