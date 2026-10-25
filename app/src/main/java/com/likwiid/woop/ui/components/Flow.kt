// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.likwiid.woop.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/**
 * Full-screen step layout: back/close on top, progress bar, scrollable question, and one
 * primary action pinned to the bottom that rides above the keyboard.
 */
@Composable
fun FlowScaffold(
    progress: Int,
    onClose: () -> Unit,
    onBack: (() -> Unit)?,
    primaryText: String?,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    primaryContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    showProgress: Boolean = true,
    progressTint: Color? = null,
    closeLabel: String = "Save and close",
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = closeLabel)
                    }
                }
                if (showProgress) WoopProgress(progress, Modifier.padding(horizontal = 20.dp), tint = progressTint)
            }
        },
        bottomBar = {
            if (primaryText != null || secondaryText != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        if (secondaryText != null && onSecondary != null) {
                            SecondaryButton(secondaryText, onSecondary, color = primaryColor)
                            Spacer(Modifier.height(4.dp))
                        }
                        if (primaryText != null) {
                            PrimaryButton(
                                text = primaryText,
                                onClick = onPrimary,
                                enabled = primaryEnabled,
                                color = primaryColor,
                                contentColor = primaryContentColor,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            content = content,
        )
    }
}

enum class ImaginePhase { IDLE, RUNNING, DONE }

@Stable
class ImagineTimerState(val totalSeconds: Int) {
    var phase by mutableStateOf(ImaginePhase.IDLE)
    var remaining by mutableIntStateOf(totalSeconds)
    fun start() {
        remaining = totalSeconds
        phase = ImaginePhase.RUNNING
    }
    fun finish() {
        phase = ImaginePhase.DONE
    }
}

@Composable
fun rememberImagineTimer(seconds: Int = 30): ImagineTimerState {
    val state = remember { ImagineTimerState(seconds) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.phase) {
        if (state.phase == ImaginePhase.RUNNING) {
            while (state.remaining > 0) {
                delay(1000)
                state.remaining -= 1
            }
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            state.finish()
        }
    }
    return state
}

/**
 * A slow "breathing" circle (about six breaths a minute) inside a countdown ring.
 */
@Composable
fun ImagineVisual(state: ImagineTimerState, color: Color, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val track = MaterialTheme.colorScheme.outlineVariant
    val breathing = rememberInfiniteTransition(label = "breath")
    val scale by breathing.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val effectiveScale = if (reduceMotion || state.phase != ImaginePhase.RUNNING) 0.9f else scale
    val fraction = when (state.phase) {
        ImaginePhase.IDLE -> 0f
        ImaginePhase.RUNNING -> 1f - state.remaining / state.totalSeconds.toFloat()
        ImaginePhase.DONE -> 1f
    }
    Box(modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawCircle(color.copy(alpha = 0.18f), radius = size.minDimension / 2 * 0.78f * effectiveScale)
            drawCircle(color.copy(alpha = 0.32f), radius = size.minDimension / 2 * 0.56f * effectiveScale)
        }
        Text(
            text = when (state.phase) {
                ImaginePhase.IDLE -> "${state.totalSeconds}s"
                ImaginePhase.RUNNING -> "${state.remaining}"
                ImaginePhase.DONE -> "Done"
            },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
