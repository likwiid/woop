// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.create

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.likwiid.woop.data.Timeframe
import com.likwiid.woop.reminders.Notifications
import com.likwiid.woop.reminders.Schedule
import com.likwiid.woop.ui.appViewModel
import com.likwiid.woop.ui.components.*
import com.likwiid.woop.ui.theme.LocalReduceMotion
import com.likwiid.woop.ui.theme.WoopStep
import com.likwiid.woop.ui.theme.colors
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

@Composable
fun CreateWoopScreen(onClose: () -> Unit, onOpenGoal: (Long) -> Unit, onStartAnother: () -> Unit) {
    val vm = appViewModel { c, saved -> CreateWoopViewModel(c.repository, saved) }
    val state by vm.state.collectAsStateWithLifecycle()

    val closeFlow = { if (state.step == CreateStep.DONE) onClose() else vm.saveAndClose(onClose) }
    BackHandler {
        if (state.step == CreateStep.DONE || !vm.back()) closeFlow()
    }

    if (state.loading) return

    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = state.step,
        transitionSpec = { stepTransition(initialState, targetState, reduceMotion) },
        label = "createStep",
    ) { step ->
        when (step) {
            CreateStep.TIMEFRAME -> TimeframeStep(state, vm, closeFlow)
            CreateStep.WISH -> WishStep(state, vm, closeFlow)
            CreateStep.LIKELIHOOD -> LikelihoodStep(state, vm, closeFlow)
            CreateStep.OUTCOME -> OutcomeStep(state, vm, closeFlow)
            CreateStep.IMAGINE_OUTCOME -> ImagineStep(
                step = WoopStep.OUTCOME,
                title = "Now picture it",
                body = "Close your eyes and imagine the best outcome as vividly as you can. Where are you? How does it feel?",
                recall = state.outcome,
                tip = null,
                onNext = vm::next,
                onBack = { vm.back() },
                onClose = closeFlow,
            )
            CreateStep.OBSTACLE -> ObstacleStep(state, vm, closeFlow)
            CreateStep.IMAGINE_OBSTACLE -> ImagineStep(
                step = WoopStep.OBSTACLE,
                title = "Picture the moment it shows up",
                body = "See the obstacle happening. When does it hit? What do you feel and think right then?",
                recall = state.obstacles.firstOrNull { it.text.isNotBlank() }?.text.orEmpty(),
                tip = "Feeling tense? Try one cyclic sigh: two short inhales through the nose, then one long, slow exhale through the mouth.",
                onNext = vm::next,
                onBack = { vm.back() },
                onClose = closeFlow,
            )
            CreateStep.PLAN -> PlanStep(state, vm, closeFlow)
            CreateStep.SUMMARY -> SummaryStep(state, vm, closeFlow)
            CreateStep.DONE -> DoneStep(state, onClose, onOpenGoal, onStartAnother)
        }
    }
}

private fun stepTransition(from: CreateStep, to: CreateStep, reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) return EnterTransition.None togetherWith ExitTransition.None
    val forward = to.ordinal > from.ordinal
    val sign = if (forward) 1 else -1
    return (slideInHorizontally(tween(260)) { sign * it / 6 } + fadeIn(tween(220))) togetherWith
        (slideOutHorizontally(tween(220)) { -sign * it / 6 } + fadeOut(tween(160)))
}

// ---------------------------------------------------------------------------------------------
// Steps
// ---------------------------------------------------------------------------------------------

@Composable
private fun TimeframeStep(state: CreateState, vm: CreateWoopViewModel, onClose: () -> Unit) {
    FlowScaffold(
        progress = 0,
        onClose = onClose,
        onBack = null,
        primaryText = if (state.timeframe != null) "Next" else null,
        onPrimary = vm::next,
    ) {
        StepHeader(
            step = null,
            overline = "New WOOP · about 5 minutes",
            title = "What's this WOOP for?",
            body = "Pick a time frame. Short ones are great for starting today.",
        )
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Timeframe.entries.forEach { tf ->
                ChoiceCard(
                    text = tf.label,
                    supporting = tf.hint,
                    selected = state.timeframe == tf,
                    onClick = { vm.pickTimeframe(tf) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Everything autosaves. You can stop at any step and pick up later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WishStep(state: CreateState, vm: CreateWoopViewModel, onClose: () -> Unit) {
    val c = WoopStep.WISH.colors()
    FlowScaffold(
        progress = 1,
        onClose = onClose,
        onBack = { vm.back() },
        primaryText = "Next",
        primaryEnabled = state.wish.isNotBlank(),
        onPrimary = vm::next,
        primaryColor = c.accent,
        primaryContentColor = c.onAccent,
    ) {
        StepHeader(
            step = WoopStep.WISH,
            title = "What do you wish for?",
            body = "Something that matters to you, challenging but doable. Name a result or a skill, not a feeling.",
        )
        Spacer(Modifier.height(24.dp))
        WoopTextField(
            value = state.wish,
            onValueChange = vm::setWish,
            placeholder = "e.g. Run 5 km without stopping",
            accent = c.accent,
            autoFocus = state.wish.isBlank(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Short is good. A few words is perfect.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        ExampleSuggestions(wishExamples(state.timeframe), c.accent, onPick = vm::setWish, modifier = Modifier.padding(top = 8.dp))
    }
}

private val likelihoodOptions = listOf(
    5 to "Very likely",
    4 to "Likely",
    3 to "About 50 / 50",
    2 to "Unlikely",
    1 to "Very unlikely",
)

@Composable
private fun LikelihoodStep(state: CreateState, vm: CreateWoopViewModel, onClose: () -> Unit) {
    val c = WoopStep.WISH.colors()
    // Auto-advance after a confident pick; low odds stop for a gentle rethink instead.
    var picked by remember { mutableStateOf(false) }
    LaunchedEffect(picked, state.likelihood) {
        if (picked && state.likelihood >= 3) {
            delay(280)
            vm.next()
        }
    }
    FlowScaffold(
        progress = 1,
        onClose = onClose,
        onBack = { vm.back() },
        primaryText = when {
            state.lowOdds -> "Keep it as it is"
            state.likelihood > 0 -> "Next"
            else -> null
        },
        onPrimary = vm::next,
        primaryColor = c.accent,
        primaryContentColor = c.onAccent,
    ) {
        StepHeader(
            step = WoopStep.WISH,
            overline = "Reality check",
            title = "How likely is it you'll make this happen?",
            body = "Go with your gut. There's no wrong answer.",
        )
        Spacer(Modifier.height(16.dp))
        ContextNote(WoopStep.WISH, state.wish)
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            likelihoodOptions.forEach { (value, label) ->
                ChoiceCard(
                    text = label,
                    selected = state.likelihood == value,
                    onClick = { vm.setLikelihood(value); picked = true },
                    accent = c.accent,
                    selectedContainer = c.container,
                )
            }
        }
        AnimatedVisibility(state.lowOdds) {
            TipCard(
                title = "Want to make it smaller?",
                text = "WOOP works best when the wish feels doable. Mental contrasting helps you commit to wishes you can reach and let go of ones you can't. Try a smaller version, like \"run 2 km\" instead of \"run 10 km\".",
                icon = Icons.Rounded.Science,
                container = c.container,
                content = c.onContainer,
                modifier = Modifier.padding(top = 20.dp),
                actions = {
                    TextButton(onClick = { vm.goTo(CreateStep.WISH) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Make it smaller", color = c.accent)
                    }
                },
            )
        }
    }
}

@Composable
private fun OutcomeStep(state: CreateState, vm: CreateWoopViewModel, onClose: () -> Unit) {
    val c = WoopStep.OUTCOME.colors()
    FlowScaffold(
        progress = 2,
        onClose = onClose,
        onBack = { vm.back() },
        primaryText = "Next",
        primaryEnabled = state.outcome.isNotBlank(),
        onPrimary = vm::next,
        primaryColor = c.accent,
        primaryContentColor = c.onAccent,
    ) {
        StepHeader(
            step = WoopStep.OUTCOME,
            title = "What's the best result? How will it feel?",
            body = "Imagine the wish came true. What's the single best thing about it?",
        )
        Spacer(Modifier.height(16.dp))
        ContextNote(WoopStep.WISH, state.wish)
        Spacer(Modifier.height(16.dp))
        WoopTextField(
            value = state.outcome,
            onValueChange = vm::setOutcome,
            placeholder = "e.g. I'll feel proud and full of energy",
            accent = c.accent,
            minLines = 3,
            autoFocus = state.outcome.isBlank(),
        )
        ExampleSuggestions(outcomeExamples, c.accent, onPick = vm::setOutcome, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ImagineStep(
    step: WoopStep,
    title: String,
    body: String,
    recall: String,
    tip: String?,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val c = step.colors()
    val timer = rememberImagineTimer(30)
    FlowScaffold(
        progress = if (step == WoopStep.OUTCOME) 2 else 3,
        onClose = onClose,
        onBack = onBack,
        primaryText = when (timer.phase) {
            ImaginePhase.IDLE -> "Start 30 seconds"
            ImaginePhase.RUNNING -> "I'm done"
            ImaginePhase.DONE -> "Continue"
        },
        onPrimary = {
            when (timer.phase) {
                ImaginePhase.IDLE -> timer.start()
                ImaginePhase.RUNNING, ImaginePhase.DONE -> onNext()
            }
        },
        primaryColor = c.accent,
        primaryContentColor = c.onAccent,
        secondaryText = if (timer.phase == ImaginePhase.IDLE) "Skip for now" else null,
        onSecondary = onNext,
    ) {
        StepHeader(step = step, overline = "${step.title} · imagine", title = title, body = body)
        Spacer(Modifier.height(16.dp))
        ContextNote(step, recall, label = if (step == WoopStep.OUTCOME) "Your best outcome" else "Your obstacle")
        // The tip goes above the timer so it is read before starting, not hidden below the fold.
        if (tip != null) {
            Spacer(Modifier.height(12.dp))
            TipCard(text = tip, icon = Icons.Rounded.Air)
        }
        Spacer(Modifier.height(28.dp))
        ImagineVisual(timer, c.accent, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun ObstacleStep(state: CreateState, vm: CreateWoopViewModel, onClose: () -> Unit) {
    val c = WoopStep.OBSTACLE.colors()
    FlowScaffold(
        progress = 3,
        onClose = onClose,
        onBack = { vm.back() },
        primaryText = "Next",
        primaryEnabled = state.obstaclesFilled,
        onPrimary = vm::next,
        primaryColor = c.accent,
        primaryContentColor = c.onAccent,
    ) {
        StepHeader(
            step = WoopStep.OBSTACLE,
            title = "What in you might get in the way?",
            body = "Be honest and realistic. A habit, a feeling, or a belief, and how you'll react when it shows up.",
        )
        Spacer(Modifier.height(16.dp))
        ContextNote(WoopStep.WISH, state.wish)
        Spacer(Modifier.height(8.dp))
        ContextNote(WoopStep.OUTCOME, state.outcome)
        Spacer(Modifier.height(20.dp))
        state.obstacles.forEachIndexed { index, obstacle ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                WoopTextField(
                    value = obstacle.text,
                    onValueChange = { vm.setObstacleText(obstacle.key, it) },
                    placeholder = if (index == 0) "e.g. I'll feel too tired after work and put it off" else "Another obstacle",
                    label = if (state.obstacles.size > 1) "Obstacle ${index + 1}" else null,
                    accent = c.accent,
                    minLines = 2,
                    // No auto-focus here: the keyboard would scroll the question away, and this one deserves a pause.
                    modifier = Modifier.weight(1f),
                )
                if (state.obstacles.size > 1) {
                    IconButton(onClick = { vm.removeObstacle(obstacle.key) }, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove obstacle ${index + 1}")
                    }
                }
            }
        }
        if (state.obstacles.size < CreateWoopViewModel.MAX_OBSTACLES) {
            TextButton(
                onClick = vm::addObstacle,
                colors = ButtonDefaults.textButtonColors(contentColor = c.accent),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add another obstacle", style = MaterialTheme.typography.labelLarge)
            }
        }
        ExampleSuggestions(
            obstacleExamples,
            c.accent,
            onPick = { picked ->
                val target = state.obstacles.lastOrNull { it.text.isBlank() } ?: state.obstacles.last()
                vm.setObstacleText(target.key, picked)
            },
        )
    }
}

@Composable
private fun PlanStep(state: CreateState, vm: CreateWoopViewModel, onClose: () -> Unit) {
    val c = WoopStep.PLAN.colors()
    val obstacleColors = WoopStep.OBSTACLE.colors()
    FlowScaffold(
        progress = 4,
        onClose = onClose,
        onBack = { vm.back() },
        primaryText = "Next",
        primaryEnabled = state.plansFilled,
        onPrimary = vm::next,
        primaryColor = c.accent,
        primaryContentColor = c.onAccent,
    ) {
        StepHeader(
            step = WoopStep.PLAN,
            title = "Make an if-then plan",
            body = "When the obstacle shows up, what will you do? Pick one small action you can do right in that moment.",
        )
        Spacer(Modifier.height(20.dp))
        state.obstacles.forEachIndexed { index, obstacle ->
            if (index > 0) Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    ContextNote(
                        WoopStep.OBSTACLE,
                        obstacle.text,
                        label = if (state.obstacles.size > 1) "Obstacle ${index + 1}" else "Obstacle",
                    )
                    Spacer(Modifier.height(14.dp))
                    WoopTextField(
                        value = obstacle.ifCue,
                        onValueChange = { vm.setIfCue(obstacle.key, it) },
                        label = "If…",
                        placeholder = "the moment it happens",
                        accent = obstacleColors.accent,
                        minLines = 1,
                    )
                    Spacer(Modifier.height(12.dp))
                    WoopTextField(
                        value = obstacle.thenAction,
                        onValueChange = { vm.setThenAction(obstacle.key, it) },
                        label = "then I will…",
                        placeholder = "one small, specific action",
                        accent = c.accent,
                        minLines = 2,
                        autoFocus = index == 0 && obstacle.thenAction.isBlank(),
                    )
                    ExampleSuggestions(planExamples, c.accent, onPick = { vm.setThenAction(obstacle.key, it) })
                }
            }
        }
    }
}

@Composable
private fun SummaryStep(state: CreateState, vm: CreateWoopViewModel, onClose: () -> Unit) {
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.finish() }
    val save = {
        if (state.checkInEveryDays > 0 && needsPermission && !Notifications.canNotify(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.finish()
        }
    }
    FlowScaffold(
        progress = 4,
        onClose = onClose,
        onBack = { vm.back() },
        primaryText = "Save my WOOP",
        onPrimary = save,
    ) {
        StepHeader(step = null, overline = "All four steps done", title = "Here's your WOOP")
        Spacer(Modifier.height(20.dp))
        WoopSummaryCards(
            wish = state.wish,
            outcome = state.outcome,
            obstacles = state.obstacles.filter { it.text.isNotBlank() }.map { Triple(it.text, it.ifCue, it.thenAction) },
            onEditWish = { vm.goTo(CreateStep.WISH) },
            onEditOutcome = { vm.goTo(CreateStep.OUTCOME) },
            onEditObstacle = { vm.goTo(CreateStep.OBSTACLE) },
            onEditPlan = { vm.goTo(CreateStep.PLAN) },
        )
        Spacer(Modifier.height(16.dp))
        TipCard(
            icon = Icons.Rounded.RecordVoiceOver,
            text = "Say your if-then out loud once. Rehearsing it links the moment to the action, so it fires more automatically later.",
        )
        Spacer(Modifier.height(28.dp))
        SectionTitle("When should we check in?")
        Text(
            "A quick re-read keeps the plan fresh. About a minute.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ChipChoices(
            options = listOf(1 to "Every day", 3 to "Every 3 days", 7 to "Every week", 0 to "No reminders"),
            selected = state.checkInEveryDays,
            onSelect = vm::setCheckIn,
        )
        if (state.checkInEveryDays > 0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("at", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(12.dp))
                TimeButton(state.checkInHour, state.checkInMinute, onPick = vm::setCheckInTime)
            }
        }
    }
}

/** The W·O·O·P recap used on the summary step and the detail screen's header. */
@Composable
fun WoopSummaryCards(
    wish: String,
    outcome: String,
    obstacles: List<Triple<String, String, String>>,
    onEditWish: (() -> Unit)? = null,
    onEditOutcome: (() -> Unit)? = null,
    onEditObstacle: (() -> Unit)? = null,
    onEditPlan: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LetterCard(WoopStep.WISH, onEdit = onEditWish) {
            Text(wish, style = MaterialTheme.typography.titleLarge)
        }
        LetterCard(WoopStep.OUTCOME, onEdit = onEditOutcome) {
            Text(outcome, style = MaterialTheme.typography.bodyLarge)
        }
        LetterCard(WoopStep.OBSTACLE, title = if (obstacles.size > 1) "Obstacles" else "Obstacle", onEdit = onEditObstacle) {
            obstacles.forEachIndexed { i, (text, _, _) ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        LetterCard(WoopStep.PLAN, title = if (obstacles.size > 1) "Plans" else "Plan", onEdit = onEditPlan) {
            obstacles.forEachIndexed { i, (_, ifCue, thenAction) ->
                if (i > 0) {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = LocalContentColor.current.copy(alpha = 0.15f))
                }
                IfThenText(ifCue, thenAction)
            }
        }
    }
}

@Composable
private fun DoneStep(state: CreateState, onClose: () -> Unit, onOpenGoal: (Long) -> Unit, onStartAnother: () -> Unit) {
    val c = WoopStep.PLAN.colors()
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
        primaryText = "Back to Today",
        onPrimary = onClose,
        secondaryText = "View this WOOP",
        onSecondary = { onOpenGoal(state.goalId) },
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
                Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(64.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "WOOP saved",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        val next = state.nextCheckIn
        val nextText = if (next != null) {
            "Next check-in ${Schedule.describeDay(next, ZonedDateTime.now())} at ${formatTime(next.hour, next.minute, is24)}."
        } else {
            "No check-in reminders. You can turn them on any time."
        }
        Text(
            "$nextText\nRe-read it whenever your motivation dips. It takes about a minute.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onStartAnother, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
            Text("Start another WOOP", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Example answers (one tap fills the field)
// ---------------------------------------------------------------------------------------------

private fun wishExamples(timeframe: Timeframe?): List<String> = when (timeframe) {
    Timeframe.TODAY -> listOf(
        "Finish the first draft of my report",
        "Go for a 20-minute walk",
        "Clear my inbox down to zero",
        "Call my mom tonight",
    )
    Timeframe.MONTH -> listOf(
        "Run 5 km without stopping",
        "Hold a 10-minute conversation in Spanish",
        "Ship the first version of my side project",
        "Cook dinner at home four nights a week",
    )
    Timeframe.LONGER -> listOf(
        "Run a half marathon",
        "Switch careers into UX design",
        "Speak conversational Japanese",
        "Write and publish a short book",
    )
    else -> listOf(
        "Run three times this week",
        "Read 50 pages of my book",
        "Practise guitar 15 minutes a day",
        "Send my job application",
    )
}

private val outcomeExamples = listOf(
    "I'll feel proud and light, knowing I kept a promise to myself.",
    "I'll have more energy in the mornings and feel strong in my body.",
    "I'll feel calm and in control instead of always behind.",
    "I'll finally have something I made that I can show people.",
)

private val obstacleExamples = listOf(
    "I'll pick up my phone and lose an hour scrolling.",
    "I'll feel too tired after work and tell myself tomorrow.",
    "I'll wait until I feel ready and keep putting it off.",
    "I'll get frustrated when progress is slow and want to quit.",
    "I'll say yes to other people's requests and run out of time.",
)

private val planExamples = listOf(
    "put my phone in another room and start a 10-minute timer",
    "do just 2 minutes, because starting is what counts",
    "take one slow breath and open the file",
    "lay out my running shoes the night before",
    "remind myself slow progress is still progress, then do one more",
)
