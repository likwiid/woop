// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.likwiid.woop.ui.components.PrimaryButton
import com.likwiid.woop.ui.components.SecondaryButton
import com.likwiid.woop.ui.components.WoopDots
import com.likwiid.woop.ui.theme.LocalReduceMotion
import com.likwiid.woop.ui.theme.WoopStep
import com.likwiid.woop.ui.theme.colors
import kotlinx.coroutines.launch

private data class IntroPage(val title: String, val body: String, val icon: ImageVector?, val step: WoopStep)

private val pages = listOf(
    IntroPage(
        "Turn wishes into plans",
        "WOOP is a 5-minute method from motivation science: Wish, Outcome, Obstacle, Plan. One question at a time.",
        null,
        WoopStep.WISH,
    ),
    IntroPage(
        "Dreaming alone drains drive",
        "Only picturing the happy ending can make your mind relax as if you're already there. WOOP pairs the dream with what's in the way, and that contrast gives you energy to act.",
        Icons.Rounded.NightsStay,
        WoopStep.OUTCOME,
    ),
    IntroPage(
        "If-then puts you on autopilot",
        "You decide now: \"If the obstacle shows up, then I'll do this.\" When the moment comes, the plan starts with less willpower. Studies suggest it helps with ADHD-type challenges too.",
        Icons.Rounded.Bolt,
        WoopStep.PLAN,
    ),
    IntroPage(
        "Private by design",
        "Everything stays on this phone. No account, no internet, no streaks to break. Just gentle reminders, when you want them.",
        Icons.Rounded.Lock,
        WoopStep.OBSTACLE,
    ),
)

@Composable
fun OnboardingScreen(onFinish: (startWoop: Boolean) -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val last = pager.currentPage == pages.lastIndex
    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (!last) {
                TextButton(onClick = { onFinish(false) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Skip", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            Page(pages[index])
        }
        PageDots(pager.currentPage, pages.size, Modifier.align(Alignment.CenterHorizontally))
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            if (last) {
                PrimaryButton("Write my first WOOP", onClick = { onFinish(true) })
                Spacer(Modifier.height(4.dp))
                SecondaryButton("Look around first", onClick = { onFinish(false) })
            } else {
                PrimaryButton(
                    "Next",
                    onClick = {
                        scope.launch {
                            val target = pager.currentPage + 1
                            if (reduceMotion) pager.scrollToPage(target) else pager.animateScrollToPage(target)
                        }
                    },
                )
                // Keeps the button in the same spot on every page.
                Spacer(Modifier.height(56.dp))
            }
        }
    }
}

@Composable
private fun Page(page: IntroPage) {
    val c = page.step.colors()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (page.icon == null) {
            WoopDots(112.dp)
        } else {
            Box(
                Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(c.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(page.icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(56.dp))
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageDots(current: Int, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
