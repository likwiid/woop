// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.theme

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class WoopStep(val letter: String, val title: String) {
    WISH("W", "Wish"),
    OUTCOME("O", "Outcome"),
    OBSTACLE("O", "Obstacle"),
    PLAN("P", "Plan"),
}

val LocalWoopPalette = staticCompositionLocalOf { LightPalette }

/** True when the system "Remove animations" accessibility setting is on. */
val LocalReduceMotion = staticCompositionLocalOf { false }

val WoopShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
@ReadOnlyComposable
fun WoopStep.colors(): StepColors {
    val palette = LocalWoopPalette.current
    return when (this) {
        WoopStep.WISH -> palette.wish
        WoopStep.OUTCOME -> palette.outcome
        WoopStep.OBSTACLE -> palette.obstacle
        WoopStep.PLAN -> palette.plan
    }
}

@Composable
fun WoopTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(
        LocalWoopPalette provides if (darkTheme) DarkPalette else LightPalette,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = WoopTypography,
            shapes = WoopShapes,
            content = content,
        )
    }
}
