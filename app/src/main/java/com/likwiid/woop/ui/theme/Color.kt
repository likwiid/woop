// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Each WOOP letter owns one hue, used everywhere for wayfinding. */
@Immutable
data class StepColors(val accent: Color, val onAccent: Color, val container: Color, val onContainer: Color)

@Immutable
data class WoopPalette(
    val wish: StepColors,
    val outcome: StepColors,
    val obstacle: StepColors,
    val plan: StepColors,
)

// Accent colours meet WCAG AA (≥ 4.5:1) against both white and their own container.
val LightPalette = WoopPalette(
    wish = StepColors(Color(0xFF4B44D0), Color.White, Color(0xFFECEBFF), Color(0xFF231E7A)),
    outcome = StepColors(Color(0xFF9A5B00), Color.White, Color(0xFFFFF1D6), Color(0xFF5C3700)),
    obstacle = StepColors(Color(0xFFB23F22), Color.White, Color(0xFFFFE7DF), Color(0xFF6E2410)),
    plan = StepColors(Color(0xFF0B7A5E), Color.White, Color(0xFFDCF4EA), Color(0xFF064A39)),
)

val DarkPalette = WoopPalette(
    wish = StepColors(Color(0xFFB3AEFF), Color(0xFF1E1A66), Color(0xFF2C2866), Color(0xFFE6E4FF)),
    outcome = StepColors(Color(0xFFFFC56B), Color(0xFF3D2A00), Color(0xFF46320C), Color(0xFFFFE9C2)),
    obstacle = StepColors(Color(0xFFFF9B7F), Color(0xFF4A1405), Color(0xFF4F2216), Color(0xFFFFDCD2)),
    plan = StepColors(Color(0xFF6FD8B5), Color(0xFF00382A), Color(0xFF10392E), Color(0xFFC9F2E3)),
)

val LightScheme = lightColorScheme(
    primary = Color(0xFF4B44D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E4FF),
    onPrimaryContainer = Color(0xFF1E1A66),
    secondary = Color(0xFF0B7A5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCF4EA),
    onSecondaryContainer = Color(0xFF064A39),
    tertiary = Color(0xFF9A5B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF1D6),
    onTertiaryContainer = Color(0xFF5C3700),
    background = Color(0xFFF7F5F1),
    onBackground = Color(0xFF1D1B19),
    surface = Color(0xFFF7F5F1),
    onSurface = Color(0xFF1D1B19),
    surfaceVariant = Color(0xFFECE8E2),
    onSurfaceVariant = Color(0xFF5C5751),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF9F6),
    surfaceContainer = Color(0xFFF2EFEA),
    surfaceContainerHigh = Color(0xFFECE8E2),
    surfaceContainerHighest = Color(0xFFE6E1DA),
    outline = Color(0xFF8A847C),
    outlineVariant = Color(0xFFDDD7CE),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF32302D),
    inverseOnSurface = Color(0xFFF5F0EA),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFFB3AEFF),
    onPrimary = Color(0xFF1E1A66),
    primaryContainer = Color(0xFF36309A),
    onPrimaryContainer = Color(0xFFE6E4FF),
    secondary = Color(0xFF6FD8B5),
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF10392E),
    onSecondaryContainer = Color(0xFFC9F2E3),
    tertiary = Color(0xFFFFC56B),
    onTertiary = Color(0xFF3D2A00),
    tertiaryContainer = Color(0xFF46320C),
    onTertiaryContainer = Color(0xFFFFE9C2),
    background = Color(0xFF131315),
    onBackground = Color(0xFFE6E2DD),
    surface = Color(0xFF131315),
    onSurface = Color(0xFFE6E2DD),
    surfaceVariant = Color(0xFF29292D),
    onSurfaceVariant = Color(0xFFBDB7AF),
    surfaceContainerLowest = Color(0xFF1B1B1E),
    surfaceContainerLow = Color(0xFF1A1A1D),
    surfaceContainer = Color(0xFF1F1F22),
    surfaceContainerHigh = Color(0xFF29292D),
    surfaceContainerHighest = Color(0xFF343438),
    outline = Color(0xFF8F8981),
    outlineVariant = Color(0xFF3F3D3A),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = Color(0xFFE6E2DD),
    inverseOnSurface = Color(0xFF32302D),
)
