package es.sebas1705.axiomnode.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =============================================================================
// AxiomNode Design System – Typography
// Design spec fonts: Display = Syne (bold sans), Body = Inter (clean sans)
// In KMP commonMain, Google Fonts API is not available. We use system font
// families as close match. To use actual fonts, add .ttf to composeResources/font.
// =============================================================================

/**
 * Body text font family.
 * Design spec: Inter → mapped to system Default (sans-serif).
 */
val bodyFontFamily = FontFamily.Default

/**
 * Display / headline font family.
 * Design spec: Syne → mapped to system SansSerif.
 * For the actual Syne font, bundle the .ttf file in composeResources/font.
 */
val displayFontFamily = FontFamily.SansSerif

// Default Material 3 typography baseline
private val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
)
