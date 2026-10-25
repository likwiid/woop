// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.likwiid.woop.data.Momentum
import com.likwiid.woop.data.PlanUse
import com.likwiid.woop.reminders.Schedule
import com.likwiid.woop.ui.appViewModel
import com.likwiid.woop.ui.components.*
import com.likwiid.woop.ui.create.WoopSummaryCards
import com.likwiid.woop.ui.theme.LocalReduceMotion
import com.likwiid.woop.ui.theme.WoopStep
import com.likwiid.woop.ui.theme.colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

@Composable
fun ReviewScreen(onClose: () -> Unit) {
    val vm = appViewModel { c, saved -> ReviewViewModel(c.repository, c.scheduler, saved) }
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler { if (!vm.back()) onClose() }
    LaunchedEffect(state.missing) { if (state.missing) onClose() }
    if (state.loading || state.details == null) return

    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            if (reduceMotion) EnterTransition.None togetherWith ExitTransition.None
            else fadeIn(tween(220)) togetherWith fadeOut(tween(150))
        },
        label = "reviewStep",
    ) { step ->
        when (step) {
            ReviewStep.REREAD -> RereadStep(state, vm, onClose)
            ReviewStep.PLAN_USE -> PlanUseStep(state, vm, onClose)
            ReviewStep.MOMENTUM -> MomentumStep(state, vm, onClose)
            ReviewStep.TUNE -> TuneStep(state, vm, onClose)
            ReviewStep.DOABLE -> DoableStep(state, vm, onClose)
            ReviewStep.NOTE -> NoteStep(state, vm, onClose)
            ReviewStep.DONE -> DoneStep(state, onClose)
        }
    }
}

@Composable
private fun ReviewScaffold(
    state: ReviewState,
    vm: ReviewViewModel,
    onClose: () -> Unit,
    primaryText: String? = null,
    onPrimary: () -> Unit = {},
    primaryEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    FlowScaffold(
        progress = state.progress,
        onClose = onClose,
        onBack = if (state.history.isEmpty()) null else ({ vm.back() }),
        primaryText = primaryText,
        onPrimary = onPrimary,
        primaryEnabled = primaryEnabled && !state.saving,
        // One colour: check-in steps are re-read / reflect / tune / note, not W·O·O·P.
        progressTint = MaterialTheme.colorScheme.primary,
        closeLabel = "Close check-in",
        content = content,
    )
}

/** Tapping an answer shows the tick for a beat, then moves on: no extra "Next" tap needed. */
@Composable
private fun rememberAutoAdvance(): (() -> Unit) -> Unit {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        { action ->
            scope.launch {
                delay(220)
                action()
            }
        }
    }
}

@Composable
private fun RereadStep(state: ReviewState, vm: ReviewViewModel, onClose: () -> Unit) {
    val d = state.details ?: return
    ReviewScaffold(state, vm, onClose, primaryText = "I've read it", onPrimary = vm::reread) {
        StepHeader(
            step = null,
            overline = "Check-in · about a minute",
            title = "Re-read your WOOP",
            body = "Read it slowly. Picture the best outcome for a moment, then the obstacle.",
        )
        Spacer(Modifier.height(20.dp))
        WoopSummaryCards(
            wish = d.goal.wish,
            outcome = d.goal.outcome,
            obstacles = d.obstacles.map { Triple(it.text, it.ifCue, it.thenAction) },
        )
    }
}

@Composable
private fun PlanUseStep(state: ReviewState, vm: ReviewViewModel, onClose: () -> Unit) {
    val plan = WoopStep.PLAN.colors()
    val advance = rememberAutoAdvance()
    var picked by remember { mutableStateOf(state.planUse) }
    ReviewScaffold(state, vm, onClose) {
        StepHeader(step = WoopStep.PLAN, title = "Since last time, did the obstacle show up?")
        Spacer(Modifier.height(16.dp))
        state.details?.firstPlan?.let { ContextNote(WoopStep.PLAN, "If ${it.ifCue}, then I will ${it.thenAction}.", label = "Your plan") }
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple(PlanUse.USED, "Yes, and I used my plan", Icons.Rounded.Bolt),
                Triple(PlanUse.MISSED, "Yes, but the plan didn't happen", Icons.Rounded.Tune),
                Triple(PlanUse.NOT_YET, "It hasn't come up yet", Icons.Rounded.Visibility),
            ).forEach { (use, text, icon) ->
                ChoiceCard(
                    text = text,
                    selected = picked == use,
                    onClick = { picked = use; advance { vm.pickPlanUse(use) } },
                    icon = icon,
                    accent = plan.accent,
                    selectedContainer = plan.container,
                )
            }
        }
    }
}

@Composable
private fun MomentumStep(state: ReviewState, vm: ReviewViewModel, onClose: () -> Unit) {
    val wish = WoopStep.WISH.colors()
    val advance = rememberAutoAdvance()
    var picked by remember { mutableStateOf(state.momentum) }
    ReviewScaffold(state, vm, onClose) {
        StepHeader(step = WoopStep.WISH, title = "How is the wish going?")
        Spacer(Modifier.height(16.dp))
        ContextNote(WoopStep.WISH, state.details?.goal?.wish.orEmpty())
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple(Momentum.MOVING, "Moving forward", null),
                Triple(Momentum.STUCK, "Feeling stuck", null),
                Triple(Momentum.DONE, "I've achieved it", Icons.Rounded.EmojiEvents),
            ).forEach { (m, text, icon) ->
                ChoiceCard(
                    text = text,
                    selected = picked == m,
                    onClick = { picked = m; advance { vm.pickMomentum(m) } },
                    icon = icon,
                    accent = wish.accent,
                    selectedContainer = wish.container,
                )
            }
        }
    }
}

@Composable
private fun TuneStep(state: ReviewState, vm: ReviewViewModel, onClose: () -> Unit) {
    val body = if (state.planUse == PlanUse.MISSED) {
        "A missed plan is information, not failure. Usually the \"if\" was too vague or the \"then\" too big. Name an exact moment, and make the action tiny."
    } else {
        "Is something new in the way? Add it as an obstacle, or sharpen the plan you have."
    }
    ReviewScaffold(
        state, vm, onClose,
        primaryText = "Save the plan",
        onPrimary = vm::tuned,
        primaryEnabled = state.plans.any { it.text.isNotBlank() && it.thenAction.isNotBlank() },
    ) {
        StepHeader(step = WoopStep.PLAN, title = "Let's tune the plan", body = body)
        Spacer(Modifier.height(20.dp))
        PlanEditor(state.plans, onChange = vm::setPlans)
    }
}

@Composable
private fun DoableStep(state: ReviewState, vm: ReviewViewModel, onClose: () -> Unit) {
    val wish = WoopStep.WISH.colors()
    val advance = rememberAutoAdvance()
    var picked by remember { mutableStateOf(state.doable) }
    val smaller = picked == Doable.SMALLER
    ReviewScaffold(
        state, vm, onClose,
        primaryText = if (smaller) "Keep the smaller wish" else null,
        onPrimary = vm::doableDone,
        primaryEnabled = state.wish.isNotBlank(),
    ) {
        StepHeader(
            step = WoopStep.WISH,
            title = "Is the wish still within reach?",
            body = "WOOP works best on wishes that feel challenging but doable. Shrinking a wish or letting it go are both smart moves.",
        )
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceCard(
                text = "Yes, keep going",
                selected = picked == Doable.YES,
                onClick = { picked = Doable.YES; advance { vm.pickDoable(Doable.YES) } },
                icon = Icons.Rounded.CheckCircle,
                accent = wish.accent,
                selectedContainer = wish.container,
            )
            ChoiceCard(
                text = "Make it smaller",
                supporting = "A first step you can take this week",
                selected = smaller,
                onClick = { picked = Doable.SMALLER; vm.pickDoable(Doable.SMALLER) },
                icon = Icons.Rounded.Tune,
                accent = wish.accent,
                selectedContainer = wish.container,
            )
            if (smaller) {
                WoopTextField(
                    value = state.wish,
                    onValueChange = vm::setWish,
                    placeholder = "A smaller version of your wish",
                    accent = wish.accent,
                    autoFocus = true,
                )
            }
            ChoiceCard(
                text = "Let it go",
                supporting = "Free up energy for wishes you can reach",
                selected = picked == Doable.LET_GO,
                onClick = { picked = Doable.LET_GO; advance { vm.pickDoable(Doable.LET_GO) } },
                icon = Icons.Rounded.Spa,
                accent = wish.accent,
                selectedContainer = wish.container,
            )
        }
    }
}

@Composable
private fun NoteStep(state: ReviewState, vm: ReviewViewModel, onClose: () -> Unit) {
    ReviewScaffold(
        state, vm, onClose,
        primaryText = if (state.note.isBlank()) "Finish without a note" else "Finish check-in",
        onPrimary = vm::finish,
    ) {
        StepHeader(
            step = null,
            overline = "Last step",
            title = "Anything worth remembering?",
            body = "Optional. What helped, what got in the way, or what you'll try next.",
        )
        Spacer(Modifier.height(20.dp))
        WoopTextField(
            value = state.note,
            onValueChange = vm::setNote,
            placeholder = "A few words is plenty",
            accent = MaterialTheme.colorScheme.primary,
            minLines = 3,
        )
    }
}

private data class DoneCopy(val icon: ImageVector, val step: WoopStep, val title: String, val body: String)

@Composable
private fun DoneStep(state: ReviewState, onClose: () -> Unit) {
    val copy = when {
        state.momentum == Momentum.DONE -> DoneCopy(
            Icons.Rounded.EmojiEvents, WoopStep.OUTCOME, "You did it",
            "Moved to Achieved. Take a moment to notice how it feels, then WOOP the next wish when you're ready.",
        )
        state.doable == Doable.LET_GO -> DoneCopy(
            Icons.Rounded.Spa, WoopStep.WISH, "Let go, with intent",
            "Stepping back from a wish that isn't feasible right now frees your energy for the ones that are.",
        )
        state.planUse == PlanUse.USED -> DoneCopy(
            Icons.Rounded.Bolt, WoopStep.PLAN, "Your plan fired",
            "Each time an if-then runs, the link between the moment and the action gets stronger.",
        )
        state.planUse == PlanUse.MISSED -> DoneCopy(
            Icons.Rounded.Tune, WoopStep.PLAN, "Plan tuned",
            "A sharper cue and a smaller step. That's how plans get better.",
        )
        else -> DoneCopy(
            Icons.Rounded.Refresh, WoopStep.PLAN, "Plan refreshed",
            "Re-reading keeps the cue ready, so you'll notice the moment when it comes.",
        )
    }
    val c = copy.step.colors()
    val is24 = rememberIs24Hour()
    val reduceMotion = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) { shown = true }
    FlowScaffold(
        progress = 4,
        onClose = onClose,
        onBack = null,
        closeLabel = "Close",
        showProgress = false,
        primaryText = "Done",
        onPrimary = onClose,
    ) {
        Spacer(Modifier.height(40.dp))
        AnimatedVisibility(
            visible = shown,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Box(
                Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(c.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(copy.icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(60.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(copy.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text(
            copy.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        state.nextCheckIn?.let { next ->
            Spacer(Modifier.height(20.dp))
            Text(
                "Next check-in ${Schedule.describeDay(next, ZonedDateTime.now())} at ${formatTime(next.hour, next.minute, is24)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
