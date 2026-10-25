// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.likwiid.woop.ui.components.LetterBadge
import com.likwiid.woop.ui.components.SectionTitle
import com.likwiid.woop.ui.components.TipCard
import com.likwiid.woop.ui.theme.LocalReduceMotion
import com.likwiid.woop.ui.theme.WoopStep
import com.likwiid.woop.ui.theme.colors

@Composable
fun LearnScreen() {
    var expanded by rememberSaveable { mutableStateOf(setOf<Int>()) }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text("The science", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text(
                    "Why WOOP works, and how sure we are about each part.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { MethodCard() }
        item { SectionTitle("What the research says", Modifier.padding(top = 12.dp)) }
        item { StrengthLegend() }
        itemsIndexed(evidence, key = { _, e -> e.title }) { i, e ->
            EvidenceCard(
                evidence = e,
                expanded = i in expanded,
                onToggle = { expanded = if (i in expanded) expanded - i else expanded + i },
            )
        }
        item {
            TipCard(
                icon = Icons.Rounded.Info,
                modifier = Modifier.padding(top = 12.dp),
                text = "WOOP is a self-help method, not medical or psychological treatment. Based on Gabriele Oettingen's WOOP research and Learning Protocol 2 in Andrew Huberman's Protocols. This app isn't affiliated with either.",
            )
        }
    }
}

@Composable
private fun MethodCard() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("The method in four steps", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Mental contrasting with implementation intentions, or WOOP for short.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                methodSteps.forEach { m ->
                    Row(verticalAlignment = Alignment.Top) {
                        LetterBadge(m.step, 32.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.step.title, style = MaterialTheme.typography.titleMedium, color = m.step.colors().accent)
                            Text(m.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun strengthColors(s: Strength): Pair<Color, Color> {
    val c = when (s) {
        Strength.STRONG -> WoopStep.PLAN.colors()
        Strength.GOOD -> WoopStep.WISH.colors()
        Strength.EARLY -> WoopStep.OUTCOME.colors()
        Strength.HYPOTHESIS, Strength.PRACTITIONER -> null
    }
    return if (c != null) c.container to c.onContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
}

@Composable
private fun StrengthBadge(s: Strength) {
    val (bg, fg) = strengthColors(s)
    Text(
        s.label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun StrengthLegend() {
    var open by rememberSaveable { mutableStateOf(false) }
    Column {
        TextButton(onClick = { open = !open }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (open) "Hide what the labels mean" else "What do the labels mean?", style = MaterialTheme.typography.labelLarge)
        }
        AnimatedVisibility(open) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                strengthLegend.forEach { (s, meaning) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.widthIn(min = 116.dp)) { StrengthBadge(s) }
                        Text(meaning, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceCard(evidence: Evidence, expanded: Boolean, onToggle: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
    ) {
        Column(
            Modifier
                .then(if (reduceMotion) Modifier else Modifier.animateContentSize())
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StrengthBadge(evidence.strength)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(evidence.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(evidence.takeaway, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                Text(evidence.detail, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("In this app", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(evidence.inApp, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Sources", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                evidence.sources.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
