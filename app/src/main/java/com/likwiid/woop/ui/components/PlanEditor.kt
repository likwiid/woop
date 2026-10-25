// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.likwiid.woop.data.ObstacleEntity
import com.likwiid.woop.ui.create.CreateWoopViewModel
import com.likwiid.woop.ui.create.ObstacleDraft
import com.likwiid.woop.ui.theme.WoopStep
import com.likwiid.woop.ui.theme.colors

fun ObstacleEntity.toDraft() = ObstacleDraft(text = text, ifCue = ifCue, thenAction = thenAction)

fun List<ObstacleDraft>.toEntities(goalId: Long) = filter { it.text.isNotBlank() || it.thenAction.isNotBlank() }
    .mapIndexed { i, o ->
        ObstacleEntity(goalId = goalId, position = i, text = o.text.trim(), ifCue = o.ifCue.trim(), thenAction = o.thenAction.trim())
    }

/** Edits obstacles and their if-then plans together; used by the detail sheet and the check-in. */
@Composable
fun PlanEditor(
    plans: List<ObstacleDraft>,
    onChange: (List<ObstacleDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val obstacle = WoopStep.OBSTACLE.colors()
    val plan = WoopStep.PLAN.colors()
    fun update(key: Long, block: (ObstacleDraft) -> ObstacleDraft) = onChange(plans.map { if (it.key == key) block(it) else it })

    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        plans.forEachIndexed { i, draft ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LetterBadge(WoopStep.OBSTACLE, 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (plans.size > 1) "Obstacle ${i + 1}" else "Obstacle",
                            style = MaterialTheme.typography.titleSmall,
                            color = obstacle.accent,
                            modifier = Modifier.weight(1f),
                        )
                        if (plans.size > 1) {
                            IconButton(onClick = { onChange(plans.filterNot { it.key == draft.key }) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove obstacle ${i + 1}")
                            }
                        }
                    }
                    WoopTextField(
                        value = draft.text,
                        onValueChange = { text -> update(draft.key) { it.copy(text = text) } },
                        placeholder = "What in you could get in the way?",
                        accent = obstacle.accent,
                        minLines = 1,
                    )
                    WoopTextField(
                        value = draft.ifCue,
                        onValueChange = { text -> update(draft.key) { it.copy(ifCue = text) } },
                        placeholder = "When and where it shows up",
                        label = "If…",
                        accent = plan.accent,
                        minLines = 1,
                    )
                    WoopTextField(
                        value = draft.thenAction,
                        onValueChange = { text -> update(draft.key) { it.copy(thenAction = text) } },
                        placeholder = "One small, specific action",
                        label = "then I will…",
                        accent = plan.accent,
                        minLines = 1,
                    )
                }
            }
        }
        if (plans.size < CreateWoopViewModel.MAX_OBSTACLES) {
            OutlinedButton(
                onClick = { onChange(plans + ObstacleDraft()) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add another obstacle", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
