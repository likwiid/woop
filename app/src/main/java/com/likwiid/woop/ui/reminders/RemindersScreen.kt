// SPDX-License-Identifier: GPL-3.0-or-later

@file:OptIn(ExperimentalMaterial3Api::class)

package com.likwiid.woop.ui.reminders

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.likwiid.woop.data.PromptEntity
import com.likwiid.woop.reminders.Notifications
import com.likwiid.woop.reminders.Schedule
import com.likwiid.woop.ui.appViewModel
import com.likwiid.woop.ui.components.*
import com.likwiid.woop.ui.today.GoalCard
import java.time.ZonedDateTime

@Composable
fun RemindersScreen(onOpenGoal: (Long) -> Unit) {
    val vm = appViewModel { c, _ -> RemindersViewModel(c.repository, c.scheduler) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(Notifications.canNotify(context)) }
    LifecycleResumeEffect(Unit) {
        canNotify = Notifications.canNotify(context)
        vm.refresh()
        onPauseOrDispose { }
    }
    val requestNotifications = rememberNotificationRequest { canNotify = it }
    var editing by remember { mutableStateOf<PromptEntity?>(null) }

    /** Ask for notifications the first time a reminder is switched on, not on launch. */
    fun withPermission(action: () -> Unit) {
        action()
        if (!canNotify) requestNotifications()
    }

    if (state.loading) return
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text("Reminders", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text(
                    "Nudges to write a WOOP, and check-ins for the ones you have.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val hasReminders = state.prompts.any { it.enabled } || state.checkIns.any { it.details.goal.checkInEveryDays > 0 }
        if (!canNotify && hasReminders) {
            item {
                TipCard(
                    icon = Icons.Rounded.NotificationsOff,
                    title = "Notifications are off",
                    text = "Your reminders can't show up until you allow notifications.",
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                    actions = {
                        FilledTonalButton(onClick = requestNotifications, modifier = Modifier.heightIn(min = 48.dp)) { Text("Allow") }
                    },
                )
            }
        }
        if (!state.preciseTiming && hasReminders) {
            item {
                TipCard(
                    icon = Icons.Rounded.AlarmOn,
                    title = "Reminders may run a little late",
                    text = "Android can delay them by several minutes to save battery. Allow precise timing to get them on the dot.",
                    actions = {
                        TextButton(onClick = { openExactAlarmSettings(context) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Allow precise timing")
                        }
                    },
                )
            }
        }

        item { SectionTitle("Write a new WOOP", Modifier.padding(top = 12.dp)) }
        if (state.prompts.isEmpty()) {
            item {
                Text(
                    "Pick a moment you already have, like your morning coffee or Sunday evening. Tap one to add it:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            items(promptPresets, key = { "preset-${it.label}" }) { preset ->
                PresetRow(preset, onClick = { withPermission { vm.addPreset(preset) } })
            }
        } else {
            items(state.prompts, key = { "prompt-${it.id}" }) { prompt ->
                PromptRow(
                    prompt = prompt,
                    onClick = { editing = prompt },
                    onToggle = { on -> if (on) withPermission { vm.setEnabled(prompt, true) } else vm.setEnabled(prompt, false) },
                )
            }
        }
        item {
            OutlinedButton(
                onClick = { editing = PromptEntity() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.prompts.isEmpty()) "Set my own time" else "Add a reminder", style = MaterialTheme.typography.labelLarge)
            }
        }

        item { SectionTitle("Check-ins", Modifier.padding(top = 20.dp)) }
        if (state.checkIns.isEmpty()) {
            item {
                Text(
                    "Each WOOP you save gets its own check-in schedule. They'll show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        } else {
            items(state.checkIns, key = { "checkin-${it.details.goal.id}" }) { card ->
                CheckInRow(card, onClick = { onOpenGoal(card.details.goal.id) })
            }
        }
    }

    editing?.let { prompt ->
        PromptEditorSheet(
            initial = prompt,
            onDismiss = { editing = null },
            onSave = { withPermission { vm.save(it) }; editing = null },
            onDelete = if (prompt.id != 0L) ({ vm.delete(prompt.id); editing = null }) else null,
        )
    }
}

/** Returns a function that asks for POST_NOTIFICATIONS, or opens settings once Android stops asking. */
@Composable
private fun rememberNotificationRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        latest(granted)
        if (!granted && context is Activity &&
            !ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            openNotificationSettings(context)
        }
    }
    return remember(launcher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canNotify(context)) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (!Notifications.canNotify(context)) {
                openNotificationSettings(context)
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(intent) }
}

@Composable
private fun PresetRow(preset: PromptPreset, onClick: () -> Unit) {
    val is24 = rememberIs24Hour()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(preset.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Schedule.describeDays(preset.daysMask)} · ${formatTime(preset.hour, preset.minute, is24)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PromptRow(prompt: PromptEntity, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val is24 = rememberIs24Hour()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatTime(prompt.hour, prompt.minute, is24),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (prompt.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val label = prompt.label.ifBlank { "Write a WOOP" }
                Text(
                    "$label · ${Schedule.describeDays(prompt.daysMask)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            // Named, so TalkBack says which reminder it turns on or off, not just "switch".
            val time = formatTime(prompt.hour, prompt.minute, is24)
            Switch(
                checked = prompt.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = "Reminder at $time" },
            )
        }
    }
}

@Composable
private fun CheckInRow(card: GoalCard, onClick: () -> Unit) {
    val goal = card.details.goal
    val is24 = rememberIs24Hour()
    val next = card.nextCheckIn
    val line = when {
        goal.checkInEveryDays == 0 -> "Off"
        card.due -> "Due now · ${Schedule.describeCadence(goal.checkInEveryDays).lowercase()}"
        next != null -> "${Schedule.describeCadence(goal.checkInEveryDays)} · next ${Schedule.describeDay(next, ZonedDateTime.now())} at ${formatTime(next.hour, next.minute, is24)}"
        else -> Schedule.describeCadence(goal.checkInEveryDays)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(goal.wish, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class DayPreset(val label: String, val mask: Int?) {
    EVERY_DAY("Every day", Schedule.ALL_DAYS),
    WEEKDAYS("Weekdays", Schedule.WEEKDAYS),
    WEEKENDS("Weekends", Schedule.WEEKENDS),
    CUSTOM("Pick days", null),
}

@Composable
private fun PromptEditorSheet(
    initial: PromptEntity,
    onDismiss: () -> Unit,
    onSave: (PromptEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var prompt by remember { mutableStateOf(initial) }
    var preset by remember {
        mutableStateOf(DayPreset.entries.firstOrNull { it.mask == initial.daysMask } ?: DayPreset.CUSTOM)
    }
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
            Text(if (initial.id == 0L) "New reminder" else "Edit reminder", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Opens straight into a new WOOP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            TimeButton(prompt.hour, prompt.minute, onPick = { h, m -> prompt = prompt.copy(hour = h, minute = m) })
            Spacer(Modifier.height(20.dp))
            ChipChoices(
                options = DayPreset.entries.map { it to it.label },
                selected = preset,
                onSelect = { p ->
                    preset = p
                    p.mask?.let { prompt = prompt.copy(daysMask = it) }
                },
            )
            if (preset == DayPreset.CUSTOM) {
                Spacer(Modifier.height(16.dp))
                DayPicker(prompt.daysMask, onChange = { prompt = prompt.copy(daysMask = it) })
            }
            Spacer(Modifier.height(20.dp))
            WoopTextField(
                value = prompt.label,
                onValueChange = { prompt = prompt.copy(label = it) },
                placeholder = "Name (optional), e.g. Plan my week",
                accent = MaterialTheme.colorScheme.primary,
                minLines = 1,
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                "Save reminder",
                onClick = { onSave(prompt.copy(label = prompt.label.trim(), enabled = true)) },
                enabled = prompt.daysMask and Schedule.ALL_DAYS != 0,
            )
            if (onDelete != null) {
                Spacer(Modifier.height(4.dp))
                SecondaryButton("Delete reminder", onClick = onDelete, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
