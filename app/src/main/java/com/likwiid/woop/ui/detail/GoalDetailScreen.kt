// SPDX-License-Identifier: GPL-3.0-or-later

@file:OptIn(ExperimentalMaterial3Api::class)

package com.likwiid.woop.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.data.GoalWithDetails
import com.likwiid.woop.data.Momentum
import com.likwiid.woop.data.PlanUse
import com.likwiid.woop.data.ReviewEntity
import com.likwiid.woop.reminders.Schedule
import com.likwiid.woop.ui.appViewModel
import com.likwiid.woop.ui.components.*
import com.likwiid.woop.ui.create.ObstacleDraft
import com.likwiid.woop.ui.create.WoopSummaryCards
import com.likwiid.woop.ui.theme.WoopStep
import com.likwiid.woop.ui.theme.colors
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class EditTarget { WISH, OUTCOME, PLANS, CHECK_IN }

@Composable
fun GoalDetailScreen(onBack: () -> Unit, onCheckIn: (Long) -> Unit) {
    val vm = appViewModel { c, saved -> GoalDetailViewModel(c.repository, c.scheduler, saved) }
    val state by vm.state.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLetGo by remember { mutableStateOf(false) }

    val details = state.details
    LaunchedEffect(state.loading, details == null, deleting) {
        if (!state.loading && details == null && !deleting) onBack()
    }
    if (details == null) return
    val goal = details.goal
    val active = goal.status == GoalStatus.ACTIVE

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                windowInsets = WindowInsets(0),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (active) {
                        IconButton(onClick = vm::togglePin) {
                            Icon(
                                Icons.Rounded.PushPin,
                                contentDescription = if (goal.pinned) "Remove from focus" else "Make this your focus",
                                tint = if (goal.pinned) WoopStep.WISH.colors().accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OverflowMenu(
                        status = goal.status,
                        onAchieved = { vm.setStatus(GoalStatus.ACHIEVED) },
                        onLetGo = { confirmLetGo = true },
                        onReactivate = { vm.setStatus(GoalStatus.ACTIVE) },
                        onDelete = { confirmDelete = true },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (active) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PrimaryButton(
                        text = if (state.due) "Check in now (due)" else "Check in now",
                        onClick = { onCheckIn(goal.id) },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            StatusBanner(details)
            Text(
                "${goal.timeframe.label} · started ${formatDate(goal.createdAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
            )
            WoopSummaryCards(
                wish = goal.wish,
                outcome = goal.outcome,
                obstacles = details.obstacles.map { Triple(it.text, it.ifCue, it.thenAction) },
                onEditWish = { editing = EditTarget.WISH },
                onEditOutcome = { editing = EditTarget.OUTCOME },
                onEditObstacle = { editing = EditTarget.PLANS },
                onEditPlan = { editing = EditTarget.PLANS },
            )

            if (active) {
                Spacer(Modifier.height(24.dp))
                CheckInRow(details, state.nextCheckIn, onClick = { editing = EditTarget.CHECK_IN })
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Check-ins")
            if (details.reviews.isEmpty()) {
                Text(
                    "None yet. A check-in is a 1-minute re-read that keeps the plan ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    details.reviews.forEach { ReviewRow(it) }
                }
            }
        }
    }

    when (editing) {
        EditTarget.WISH -> TextEditSheet(
            step = WoopStep.WISH,
            title = "Edit your wish",
            initial = goal.wish,
            placeholder = "Something challenging but doable",
            onDismiss = { editing = null },
            onSave = { vm.setWish(it); editing = null },
        )
        EditTarget.OUTCOME -> TextEditSheet(
            step = WoopStep.OUTCOME,
            title = "Edit the best outcome",
            initial = goal.outcome,
            placeholder = "What's the best result, and how will it feel?",
            onDismiss = { editing = null },
            onSave = { vm.setOutcome(it); editing = null },
        )
        EditTarget.PLANS -> PlansEditSheet(
            initial = details.obstacles.map { it.toDraft() },
            onDismiss = { editing = null },
            onSave = { vm.setPlans(it); editing = null },
        )
        EditTarget.CHECK_IN -> CheckInSheet(
            everyDays = goal.checkInEveryDays,
            hour = goal.checkInHour,
            minute = goal.checkInMinute,
            onDismiss = { editing = null },
            onSave = { d, h, m -> vm.setCheckIn(d, h, m); editing = null },
        )
        null -> Unit
    }

    if (confirmLetGo) {
        AlertDialog(
            onDismissRequest = { confirmLetGo = false },
            icon = { Icon(Icons.Rounded.Spa, contentDescription = null) },
            title = { Text("Let this wish go?") },
            text = {
                Text(
                    "Letting go of a wish that no longer fits is a healthy result, not a failure. It frees energy for wishes you can reach. You can reactivate it any time.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.setStatus(GoalStatus.LET_GO); confirmLetGo = false }) { Text("Let it go") }
            },
            dismissButton = { TextButton(onClick = { confirmLetGo = false }) { Text("Keep it") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("Delete this WOOP?") },
            text = { Text("Its check-ins are deleted too. This can't be undone.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        deleting = true
                        vm.delete(onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun OverflowMenu(
    status: GoalStatus,
    onAchieved: () -> Unit,
    onLetGo: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More options") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (status == GoalStatus.ACTIVE) {
                DropdownMenuItem(
                    text = { Text("Mark as achieved") },
                    leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                    onClick = { open = false; onAchieved() },
                    modifier = Modifier.heightIn(min = 52.dp),
                )
                DropdownMenuItem(
                    text = { Text("Let it go") },
                    leadingIcon = { Icon(Icons.Rounded.Spa, contentDescription = null) },
                    onClick = { open = false; onLetGo() },
                    modifier = Modifier.heightIn(min = 52.dp),
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Make active again") },
                    leadingIcon = { Icon(Icons.Rounded.Replay, contentDescription = null) },
                    onClick = { open = false; onReactivate() },
                    modifier = Modifier.heightIn(min = 52.dp),
                )
            }
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                onClick = { open = false; onDelete() },
                modifier = Modifier.heightIn(min = 52.dp),
            )
        }
    }
}

@Composable
private fun StatusBanner(details: GoalWithDetails) {
    val (text, step) = when (details.goal.status) {
        GoalStatus.ACHIEVED -> "Achieved. Nicely done." to WoopStep.PLAN
        GoalStatus.LET_GO -> "Let go. That's a valid result too." to WoopStep.OUTCOME
        else -> return
    }
    val c = step.colors()
    Surface(
        color = c.container,
        contentColor = c.onContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

@Composable
private fun CheckInRow(details: GoalWithDetails, next: ZonedDateTime?, onClick: () -> Unit) {
    val goal = details.goal
    val is24 = rememberIs24Hour()
    val summary = if (goal.checkInEveryDays == 0) {
        "Check-in reminders are off"
    } else {
        "${Schedule.describeCadence(goal.checkInEveryDays)} at ${formatTime(goal.checkInHour, goal.checkInMinute, is24)}"
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(summary, style = MaterialTheme.typography.titleMedium)
                if (next != null) {
                    Text(
                        "Next: ${Schedule.describeDay(next, ZonedDateTime.now())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ReviewRow(review: ReviewEntity) {
    val plan = when (review.planUse) {
        PlanUse.USED -> "Used the plan"
        PlanUse.MISSED -> "Plan didn't happen this time"
        PlanUse.NOT_YET -> "Obstacle didn't come up"
    }
    val momentum = when (review.momentum) {
        Momentum.MOVING -> "moving forward"
        Momentum.STUCK -> "feeling stuck"
        Momentum.DONE -> "achieved"
    }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(formatDate(review.at), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$plan · $momentum", style = MaterialTheme.typography.bodyMedium)
            if (review.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("“${review.note}”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

// ---------------------------------------------------------------------------------------------
// Edit sheets
// ---------------------------------------------------------------------------------------------

@Composable
private fun EditSheet(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            content()
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Save", onClick = onSave, enabled = saveEnabled)
        }
    }
}

@Composable
private fun TextEditSheet(
    step: WoopStep,
    title: String,
    initial: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    EditSheet(title = title, onDismiss = onDismiss, onSave = { onSave(text) }, saveEnabled = text.isNotBlank()) {
        WoopTextField(value = text, onValueChange = { text = it }, placeholder = placeholder, accent = step.colors().accent, minLines = 3)
    }
}

@Composable
private fun PlansEditSheet(
    initial: List<ObstacleDraft>,
    onDismiss: () -> Unit,
    onSave: (List<ObstacleDraft>) -> Unit,
) {
    var plans by remember { mutableStateOf(initial.ifEmpty { listOf(ObstacleDraft()) }) }
    EditSheet(
        title = "Obstacles and plans",
        onDismiss = onDismiss,
        onSave = { onSave(plans) },
        saveEnabled = plans.any { it.text.isNotBlank() },
    ) {
        PlanEditor(plans, onChange = { plans = it })
    }
}

@Composable
private fun CheckInSheet(
    everyDays: Int,
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit,
) {
    var days by remember { mutableIntStateOf(everyDays) }
    var h by remember { mutableIntStateOf(hour) }
    var m by remember { mutableIntStateOf(minute) }
    EditSheet(title = "Check-in reminders", onDismiss = onDismiss, onSave = { onSave(days, h, m) }) {
        Text(
            "A quick re-read keeps the if-then fresh, so it fires when the moment comes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        ChipChoices(
            options = listOf(1 to "Every day", 3 to "Every 3 days", 7 to "Every week", 14 to "Every 2 weeks", 0 to "Off"),
            selected = days,
            onSelect = { days = it },
        )
        if (days > 0) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("at", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(12.dp))
                TimeButton(h, m, onPick = { nh, nm -> h = nh; m = nm })
            }
        }
    }
}
