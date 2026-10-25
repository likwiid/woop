// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.reminders.Schedule
import com.likwiid.woop.ui.appViewModel
import com.likwiid.woop.ui.components.*
import com.likwiid.woop.ui.theme.WoopStep
import com.likwiid.woop.ui.theme.colors
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class ListFilter(val label: String) { ACTIVE("Active"), ACHIEVED("Achieved"), LET_GO("Let go") }

@Composable
fun TodayScreen(
    onNewWoop: () -> Unit,
    onContinueDraft: (Long) -> Unit,
    onOpenGoal: (Long) -> Unit,
    onCheckIn: (Long) -> Unit,
    onSettings: () -> Unit,
    onLearn: () -> Unit,
) {
    val vm = appViewModel { c, _ -> TodayViewModel(c.repository, c.scheduler) }
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    var filter by rememberSaveable { mutableStateOf(ListFilter.ACTIVE) }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        floatingActionButton = {
            if (!state.loading && !state.isEmpty) {
                ExtendedFloatingActionButton(
                    onClick = onNewWoop,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("New WOOP", style = MaterialTheme.typography.titleMedium) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    // The extended FAB's text doesn't reach the accessibility tree, so TalkBack only said "Button".
                    modifier = Modifier
                        .heightIn(min = 60.dp)
                        .semantics { contentDescription = "New WOOP" },
                )
            }
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item { Greeting(onSettings) }

            if (state.isEmpty) {
                item { EmptyToday(onNewWoop, onLearn) }
                return@LazyColumn
            }

            state.draft?.let { draft ->
                item {
                    TipCard(
                        icon = Icons.Rounded.EditNote,
                        title = "Continue your draft",
                        text = draft.wish.ifBlank { "Untitled WOOP" },
                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                        actions = {
                            TextButton(onClick = { vm.discardDraft(draft.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Discard")
                            }
                            Spacer(Modifier.width(4.dp))
                            FilledTonalButton(onClick = { onContinueDraft(draft.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Continue")
                            }
                        },
                    )
                }
            }

            state.focus?.let { focus ->
                item { FocusCard(focus, onOpen = { onOpenGoal(focus.details.goal.id) }, onCheckIn = { onCheckIn(focus.details.goal.id) }) }
            }

            if (state.due.isNotEmpty()) {
                item { SectionTitle("Ready for a check-in", Modifier.padding(top = 12.dp)) }
                items(state.due, key = { "due-${it.details.goal.id}" }) { card ->
                    GoalRow(card, onClick = { onCheckIn(card.details.goal.id) }, actionLabel = "Check in")
                }
            }

            item {
                Column(Modifier.padding(top = 12.dp)) {
                    // A running total, never a streak: nothing to lose by missing a day.
                    SectionTitle("Your WOOPs") {
                        if (state.checkIns > 0) {
                            Text(
                                if (state.checkIns == 1) "1 check-in done" else "${state.checkIns} check-ins done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val largeText = LocalDensity.current.fontScale > 1.3f
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ListFilter.entries.forEachIndexed { i, f ->
                            val count = when (f) {
                                ListFilter.ACTIVE -> state.active.size + if (state.focus != null) 1 else 0
                                ListFilter.ACHIEVED -> state.achieved.size
                                ListFilter.LET_GO -> state.letGo.size
                            }
                            SegmentedButton(
                                selected = filter == f,
                                onClick = { filter = f },
                                shape = SegmentedButtonDefaults.itemShape(i, ListFilter.entries.size),
                                modifier = Modifier.heightIn(min = 48.dp),
                                icon = {},
                            ) {
                                // At large font sizes the count drops to its own line, and the label shrinks a
                                // little rather than breaking mid-word ("Achieve / d") when a third of the width is too narrow.
                                val style = MaterialTheme.typography.labelMedium
                                BasicText(
                                    text = if (largeText) "${f.label}\n$count" else "${f.label} $count",
                                    style = style.copy(color = LocalContentColor.current, textAlign = TextAlign.Center),
                                    maxLines = if (largeText) 2 else 1,
                                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize),
                                )
                            }
                        }
                    }
                }
            }

            val list = when (filter) {
                ListFilter.ACTIVE -> state.active
                ListFilter.ACHIEVED -> state.achieved
                ListFilter.LET_GO -> state.letGo
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        when (filter) {
                            ListFilter.ACTIVE -> if (state.focus != null) "Your only active WOOP is in focus above." else "No active WOOPs right now."
                            ListFilter.ACHIEVED -> "Achieved WOOPs will collect here."
                            ListFilter.LET_GO -> "Wishes you let go of land here. Letting go is a valid result."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
            } else {
                items(list, key = { "goal-${it.details.goal.id}" }) { card ->
                    GoalRow(card, onClick = { onOpenGoal(card.details.goal.id) })
                }
            }

        }
    }
}

@Composable
private fun Greeting(onSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun EmptyToday(onNewWoop: () -> Unit, onLearn: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
    ) {
        WoopDots(88.dp)
        Spacer(Modifier.height(32.dp))
        Text(
            "Turn a wish into a plan",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Your first WOOP takes about 5 minutes. One question at a time, and it saves as you go.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Start my first WOOP", onClick = onNewWoop, icon = Icons.Rounded.Add)
        Spacer(Modifier.height(4.dp))
        SecondaryButton("How does it work?", onClick = onLearn)
    }
}

@Composable
private fun FocusCard(card: GoalCard, onOpen: () -> Unit, onCheckIn: () -> Unit) {
    val goal = card.details.goal
    val plan = card.details.firstPlan
    val wish = WoopStep.WISH.colors()
    val planColors = WoopStep.PLAN.colors()
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PushPin, contentDescription = null, tint = wish.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Focus", style = MaterialTheme.typography.labelLarge, color = wish.accent, modifier = Modifier.weight(1f))
                if (card.due) DueBadge()
            }
            Spacer(Modifier.height(10.dp))
            Text(goal.wish, style = MaterialTheme.typography.headlineSmall)
            if (plan != null) {
                Spacer(Modifier.height(16.dp))
                Surface(color = planColors.container, contentColor = planColors.onContainer, shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(16.dp)) {
                        LetterBadge(WoopStep.PLAN, 24.dp)
                        Spacer(Modifier.width(12.dp))
                        IfThenText(plan.ifCue, plan.thenAction, Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // Label over value, so a long day name breaks where it reads well, not mid-phrase.
            val (label, value) = checkInLine(card)
            val checkInText = @Composable { modifier: Modifier ->
                Column(modifier) {
                    if (label != null) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
            val checkInButton = @Composable { modifier: Modifier ->
                Button(
                    onClick = onCheckIn,
                    shape = RoundedCornerShape(16.dp),
                    modifier = modifier.heightIn(min = 52.dp),
                ) {
                    Text("Check in", style = MaterialTheme.typography.labelLarge)
                }
            }
            // Side by side at normal sizes; stacked once large text would squeeze it. The stacked button
            // stays narrow and left-aligned so the New WOOP button never covers it.
            if (LocalDensity.current.fontScale > 1.3f) {
                checkInText(Modifier)
                Spacer(Modifier.height(12.dp))
                checkInButton(Modifier)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    checkInText(Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    checkInButton(Modifier)
                }
            }
        }
    }
}

@Composable
private fun DueBadge() {
    val c = WoopStep.OUTCOME.colors()
    Text(
        "Check-in due",
        style = MaterialTheme.typography.labelMedium,
        color = c.onContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun checkInLine(card: GoalCard): Pair<String?, String> {
    val is24 = rememberIs24Hour()
    val next = card.nextCheckIn
    return when {
        card.due -> "About a minute" to "Re-read and tune your plan"
        next != null -> "Next check-in" to
            "${Schedule.describeDay(next, ZonedDateTime.now()).replaceFirstChar { it.uppercase() }}, ${formatTime(next.hour, next.minute, is24)}"
        else -> null to "Re-read it whenever motivation dips"
    }
}

@Composable
private fun GoalRow(card: GoalCard, onClick: () -> Unit, actionLabel: String? = null) {
    val goal = card.details.goal
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            StepDots(
                filled = listOf(
                    goal.wish.isNotBlank(),
                    goal.outcome.isNotBlank(),
                    card.details.obstacles.isNotEmpty(),
                    card.details.firstPlan != null,
                ),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.wish, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val sub = when (goal.status) {
                    GoalStatus.ACHIEVED -> "Achieved"
                    GoalStatus.LET_GO -> "Let go"
                    else -> if (card.due) "Check-in due" else goal.timeframe.label
                }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (actionLabel != null) {
                Spacer(Modifier.width(8.dp))
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Tiny 2×2 W·O·O·P dots: a glanceable "is this WOOP complete?" signal. */
@Composable
private fun StepDots(filled: List<Boolean>) {
    val steps = WoopStep.entries
    val empty = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (col in 0..1) {
                    val i = row * 2 + col
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (filled[i]) steps[i].colors().accent else empty),
                    )
                }
            }
        }
    }
}
