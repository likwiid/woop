// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.likwiid.woop.MainActivity
import com.likwiid.woop.WoopApp
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.data.GoalWithDetails
import com.likwiid.woop.reminders.Notifications
import com.likwiid.woop.reminders.ReminderScheduler
import com.likwiid.woop.ui.theme.DarkPalette
import com.likwiid.woop.ui.theme.DarkScheme
import com.likwiid.woop.ui.theme.LightPalette
import com.likwiid.woop.ui.theme.LightScheme
import java.time.ZonedDateTime

/**
 * Home-screen card with the focus goal's if-then plan. Seeing the cue and the response many times a
 * day is the cheapest rehearsal there is, so the plan gets the space and everything else stays quiet.
 */
class FocusWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as WoopApp).container
        provideContent {
            val goals by container.repository.goals.collectAsState(initial = null)
            val focus = goals?.let { all -> all.firstOrNull { it.goal.status == GoalStatus.ACTIVE } }
            Card(onClick = open(context, focus?.let { Notifications.destGoal(it.goal.id) } ?: Notifications.DEST_CREATE)) {
                when {
                    goals == null -> Unit
                    focus == null -> Empty()
                    else -> Focus(focus, container.scheduler)
                }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = FocusWidget().updateAll(context)
    }
}

class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusWidget()
}

// Same tokens as the app, picked per system light/dark.
private val surface = ColorProvider(LightScheme.surfaceContainerLowest, DarkScheme.surfaceContainerLowest)
private val onSurface = ColorProvider(LightScheme.onSurface, DarkScheme.onSurface)
private val muted = ColorProvider(LightScheme.onSurfaceVariant, DarkScheme.onSurfaceVariant)
private val planAccent = ColorProvider(LightPalette.plan.accent, DarkPalette.plan.accent)
private val onPlanAccent = ColorProvider(LightPalette.plan.onAccent, DarkPalette.plan.onAccent)
private val planContainer = ColorProvider(LightPalette.plan.container, DarkPalette.plan.container)
private val onPlanContainer = ColorProvider(LightPalette.plan.onContainer, DarkPalette.plan.onContainer)
private val dueContainer = ColorProvider(LightPalette.outcome.container, DarkPalette.outcome.container)
private val onDueContainer = ColorProvider(LightPalette.outcome.onContainer, DarkPalette.outcome.onContainer)

/** Below this (a 4×1 widget) the text and padding get smaller. */
private val ROOMY_HEIGHT = 130.dp
private const val WISH_SIZE = 17f

/** Rough line height of the system font, as a multiple of its size. */
private const val LINE_HEIGHT = 1.25f

/**
 * Widget text follows the system font size up to this much. Past it a 4×2 card can no longer show
 * a whole plan; the app itself still honours the full setting.
 */
private const val MAX_TEXT_SCALE = 1.5f

@Composable
private fun systemTextScale() = LocalContext.current.resources.configuration.fontScale

@Composable
private fun shownTextScale() = minOf(systemTextScale(), MAX_TEXT_SCALE)

/** [size] in sp, scaled by the system font size but no further than [MAX_TEXT_SCALE]. */
@Composable
private fun widgetSp(size: Float) = (size * shownTextScale() / systemTextScale()).sp

@Composable
private fun cardPadding() = if (LocalSize.current.height >= ROOMY_HEIGHT) 16.dp else 10.dp

private fun open(context: Context, dest: String): Action = actionStartActivity(
    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_DEST, dest),
)

@Composable
private fun Card(onClick: Action, content: @Composable () -> Unit) {
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(surface)
            .clickable(onClick)
            .padding(cardPadding()),
    ) {
        content()
    }
}

@Composable
private fun Focus(details: GoalWithDetails, scheduler: ReminderScheduler) {
    val goal = details.goal
    val plan = details.firstPlan
    val due = scheduler.isCheckInDue(goal, ZonedDateTime.now())
    val roomy = LocalSize.current.height >= ROOMY_HEIGHT
    val planSize = if (roomy) 17f else 14f
    val boxPadding = if (roomy) 12.dp else 8.dp

    // Count the plan lines that fit, so the "then" is never the part cut off: the cue drops to one
    // line first, then the wish line goes.
    val scale = shownTextScale()
    val inner = LocalSize.current.height.value - 2 * cardPadding().value
    fun planLines(height: Float) = ((height - 2 * boxPadding.value) / (planSize * scale * LINE_HEIGHT)).toInt()
    val header = WISH_SIZE * scale * LINE_HEIGHT + 10f
    val showWish = plan == null || planLines(inner - header) >= 3
    val lines = planLines(if (showWish) inner - header else inner).coerceAtLeast(2)
    val cueLines = if (lines >= 4) 2 else 1

    Column(GlanceModifier.fillMaxSize()) {
        if (showWish) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    goal.wish,
                    style = TextStyle(color = onSurface, fontSize = widgetSp(WISH_SIZE), fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (due) CheckInPill(goal.id)
            }
            Spacer(GlanceModifier.height(10.dp))
        }
        if (plan != null) {
            Row(
                GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .cornerRadius(16.dp)
                    .background(planContainer)
                    .padding(boxPadding),
            ) {
                PlanBadge()
                Spacer(GlanceModifier.width(10.dp))
                // Read as one sentence, the way it is rehearsed: the cue, then the response in bold.
                Column(GlanceModifier.defaultWeight()) {
                    Text(
                        "If ${plan.ifCue.trim().trimEnd('.', ',')},",
                        style = TextStyle(color = onPlanContainer, fontSize = widgetSp(planSize)),
                        maxLines = cueLines,
                    )
                    Text(
                        "then I will ${plan.thenAction.trim().trimEnd('.')}.",
                        style = TextStyle(color = onPlanContainer, fontSize = widgetSp(planSize), fontWeight = FontWeight.Bold),
                        maxLines = lines - cueLines,
                    )
                }
            }
        } else {
            Text(
                "Add an if-then plan for your main obstacle.",
                style = TextStyle(color = muted, fontSize = widgetSp(14f)),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun CheckInPill(goalId: Long) {
    Text(
        "Check in",
        style = TextStyle(color = onDueContainer, fontSize = widgetSp(13f), fontWeight = FontWeight.Medium),
        modifier = GlanceModifier
            .cornerRadius(12.dp)
            .background(dueContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(open(LocalContext.current, Notifications.destReview(goalId)))
            .semantics { contentDescription = "Check-in due. Check in now" },
    )
}

@Composable
private fun PlanBadge() {
    Box(
        GlanceModifier.size(24.dp).cornerRadius(12.dp).background(planAccent),
        contentAlignment = Alignment.Center,
    ) {
        // Fixed size: the letter has to fit its 24dp circle at any system font size.
        Text("P", style = TextStyle(color = onPlanAccent, fontSize = (13f / systemTextScale()).sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun Empty() {
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text("No WOOP yet", style = TextStyle(color = onSurface, fontSize = widgetSp(WISH_SIZE), fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(4.dp))
        Text(
            "Tap to turn a wish into an if-then plan. About five minutes.",
            style = TextStyle(color = muted, fontSize = widgetSp(14f)),
            maxLines = 3,
        )
    }
}
