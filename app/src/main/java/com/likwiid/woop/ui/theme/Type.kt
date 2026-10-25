// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.likwiid.woop.R

/**
 * Atkinson Hyperlegible (Braille Institute) keeps look-alike letters (I l 1, O 0, b d) distinct,
 * which helps skim-reading and low-vision users.
 */
val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = Atkinson,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

val WoopTypography = Typography(
    displayLarge = style(44, 52, FontWeight.Bold),
    displayMedium = style(38, 46, FontWeight.Bold),
    displaySmall = style(32, 40, FontWeight.Bold),
    headlineLarge = style(30, 38, FontWeight.Bold),
    headlineMedium = style(27, 34, FontWeight.Bold),
    headlineSmall = style(23, 30, FontWeight.Bold),
    titleLarge = style(21, 28, FontWeight.Bold),
    titleMedium = style(18, 26, FontWeight.Bold),
    titleSmall = style(15, 22, FontWeight.Bold),
    bodyLarge = style(18, 28),
    bodyMedium = style(16, 24),
    bodySmall = style(14, 20),
    labelLarge = style(16, 22, FontWeight.Bold),
    labelMedium = style(14, 20, FontWeight.Bold),
    labelSmall = style(12, 16, FontWeight.Bold, 0.3),
)
