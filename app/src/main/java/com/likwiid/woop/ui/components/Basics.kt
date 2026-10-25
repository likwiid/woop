// SPDX-License-Identifier: GPL-3.0-or-later

@file:OptIn(ExperimentalMaterial3Api::class)

package com.likwiid.woop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.likwiid.woop.ui.theme.WoopStep
import com.likwiid.woop.ui.theme.colors
import kotlin.math.roundToInt

/**
 * Four-segment progress bar. [reached] = number of segments filled (0–4). Filled segments take
 * the W·O·O·P letter colours unless [tint] is set (for flows whose steps aren't W/O/O/P).
 */
@Composable
fun WoopProgress(reached: Int, modifier: Modifier = Modifier, tint: Color? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .semantics { contentDescription = "Step ${reached.coerceIn(0, 4)} of 4" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WoopStep.entries.forEachIndexed { i, step ->
            val color by animateColorAsState(
                if (i < reached) tint ?: step.colors().accent else MaterialTheme.colorScheme.outlineVariant,
                label = "progress",
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
fun LetterBadge(step: WoopStep, size: Dp = 28.dp) {
    val c = step.colors()
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(c.accent)
            // Decorative: TalkBack would read "W, Wish". The step name beside every badge says it already.
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        // The circle is a fixed size, so the letter ignores font scaling or it would overflow; the step
        // name beside every badge carries the meaning and does scale.
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            Text(
                step.letter,
                style = if (size >= 32.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                color = c.onAccent,
            )
        }
    }
}

/** Letter badge + step name above a flow question. */
@Composable
fun StepHeader(step: WoopStep?, title: String, body: String? = null, overline: String? = null) {
    Column {
        if (step != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LetterBadge(step)
                Spacer(Modifier.width(10.dp))
                Text(overline ?: step.title, style = MaterialTheme.typography.titleSmall, color = step.colors().accent)
            }
            Spacer(Modifier.height(14.dp))
        } else if (overline != null) {
            Text(overline, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(14.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (body != null) {
            Spacer(Modifier.height(10.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Earlier answers echoed back so nothing has to be held in working memory. */
@Composable
fun ContextNote(step: WoopStep, text: String, modifier: Modifier = Modifier, label: String = step.title) {
    if (text.isBlank()) return
    val c = step.colors()
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.container)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        LetterBadge(step, 24.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.onContainer.copy(alpha = 0.75f))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = c.onContainer,
                // About three lines' worth of text at any font size: large text needs more lines, not less content.
                maxLines = (3 * LocalDensity.current.fontScale).roundToInt().coerceAtLeast(3),
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Big, single-choice answer card (≥ 64 dp tall). */
@Composable
fun ChoiceCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    selectedContainer: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = if (selected) selectedContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = if (selected) BorderStroke(2.dp, accent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.titleMedium)
                if (supporting != null) {
                    Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = accent)
            }
        }
    }
}

/** Full-width primary action, 64 dp tall, meant to sit in the thumb zone. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Summary card for one WOOP letter, tinted in that letter's colour. */
@Composable
fun LetterCard(
    step: WoopStep,
    modifier: Modifier = Modifier,
    title: String = step.title,
    onEdit: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = step.colors()
    Surface(
        color = c.container,
        contentColor = c.onContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onEdit != null) Modifier.clip(RoundedCornerShape(24.dp)).clickable(onClickLabel = "Edit ${step.title}", onClick = onEdit) else Modifier),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
                LetterBadge(step)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.onContainer, modifier = Modifier.weight(1f))
                if (onEdit != null) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = c.onContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Column(Modifier.padding(end = 8.dp), content = content)
        }
    }
}

/** "If … then I will …" rendered with the two halves visually distinct. */
@Composable
fun IfThenText(ifCue: String, thenAction: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Column(modifier) {
        Text("If", style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
        Text(ifCue.ifBlank { "…" }, style = MaterialTheme.typography.bodyLarge, color = color)
        Spacer(Modifier.height(6.dp))
        Text("then I will", style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
        Text(thenAction.ifBlank { "…" }, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        trailing?.invoke()
    }
}

/** Calm info/tip box used for science notes and gentle nudges. */
@Composable
fun TipCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: Color = MaterialTheme.colorScheme.onSurface,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    if (title != null) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (actions != null) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
            }
        }
    }
}

/** The four coloured dots that make up the app's logo. */
@Composable
fun WoopDots(size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val dot = size * 0.44f
    val gap = size * 0.12f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Box(Modifier.size(dot).clip(CircleShape).background(WoopStep.WISH.colors().accent))
            Box(Modifier.size(dot).clip(CircleShape).background(WoopStep.OUTCOME.colors().accent))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Box(Modifier.size(dot).clip(CircleShape).background(WoopStep.OBSTACLE.colors().accent))
            Box(Modifier.size(dot).clip(CircleShape).background(WoopStep.PLAN.colors().accent))
        }
    }
}
