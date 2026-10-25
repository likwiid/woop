// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.likwiid.woop.data.GoalEntity
import com.likwiid.woop.data.GoalStatus
import com.likwiid.woop.data.ObstacleEntity
import com.likwiid.woop.data.Timeframe
import com.likwiid.woop.data.WoopRepository
import com.likwiid.woop.reminders.ReminderScheduler
import com.likwiid.woop.reminders.Schedule
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import kotlin.random.Random

/** The order matters: mental contrasting only works dream-first, obstacle-second. */
enum class CreateStep(val progress: Int) {
    TIMEFRAME(0),
    WISH(1),
    LIKELIHOOD(1),
    OUTCOME(2),
    IMAGINE_OUTCOME(2),
    OBSTACLE(3),
    IMAGINE_OBSTACLE(3),
    PLAN(4),
    SUMMARY(4),
    DONE(4),
}

data class ObstacleDraft(
    val key: Long = Random.nextLong(),
    val text: String = "",
    val ifCue: String = "",
    val thenAction: String = "",
)

data class CreateState(
    val loading: Boolean = true,
    val step: CreateStep = CreateStep.TIMEFRAME,
    val goalId: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val timeframe: Timeframe? = null,
    val wish: String = "",
    val likelihood: Int = 0,
    val outcome: String = "",
    val obstacles: List<ObstacleDraft> = listOf(ObstacleDraft()),
    val checkInEveryDays: Int = 3,
    val checkInHour: Int = 19,
    val checkInMinute: Int = 0,
    val nextCheckIn: ZonedDateTime? = null,
) {
    val lowOdds: Boolean get() = likelihood in 1..2
    val obstaclesFilled: Boolean get() = obstacles.any { it.text.isNotBlank() }
    val plansFilled: Boolean get() = obstacles.filter { it.text.isNotBlank() }.all { it.ifCue.isNotBlank() && it.thenAction.isNotBlank() }
}

class CreateWoopViewModel(
    private val repository: WoopRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateState())
    val state: StateFlow<CreateState> = _state.asStateFlow()
    private var saveJob: Job? = null
    private val saveLock = Mutex()

    init {
        val draftId = savedState.get<Long>("draftId") ?: -1L
        viewModelScope.launch {
            val draft = if (draftId > 0) repository.getGoal(draftId) else null
            _state.value = if (draft == null) {
                CreateState(loading = false)
            } else {
                val g = draft.goal
                val obstacles = draft.obstacles.map { ObstacleDraft(text = it.text, ifCue = it.ifCue, thenAction = it.thenAction) }
                    .ifEmpty { listOf(ObstacleDraft()) }
                val restored = CreateState(
                    loading = false,
                    goalId = g.id,
                    createdAt = g.createdAt,
                    timeframe = g.timeframe,
                    wish = g.wish,
                    likelihood = g.likelihood,
                    outcome = g.outcome,
                    obstacles = obstacles,
                    checkInEveryDays = defaultCadence(g.timeframe),
                )
                restored.copy(step = resumeStep(restored))
            }
        }
    }

    private fun resumeStep(s: CreateState): CreateStep = when {
        s.wish.isBlank() -> CreateStep.WISH
        s.likelihood == 0 -> CreateStep.LIKELIHOOD
        s.outcome.isBlank() -> CreateStep.OUTCOME
        !s.obstaclesFilled -> CreateStep.OBSTACLE
        !s.plansFilled -> CreateStep.PLAN
        else -> CreateStep.SUMMARY
    }

    private fun defaultCadence(timeframe: Timeframe?): Int = when (timeframe) {
        Timeframe.TODAY -> 1
        Timeframe.WEEK -> 3
        Timeframe.MONTH, Timeframe.LONGER -> 7
        null -> 3
    }

    private fun edit(block: (CreateState) -> CreateState) {
        _state.update(block)
        scheduleDraftSave()
    }

    fun pickTimeframe(tf: Timeframe) {
        edit { it.copy(timeframe = tf, checkInEveryDays = defaultCadence(tf)) }
        goTo(CreateStep.WISH)
    }

    fun setWish(text: String) = edit { it.copy(wish = text) }
    fun setLikelihood(value: Int) = edit { it.copy(likelihood = value) }
    fun setOutcome(text: String) = edit { it.copy(outcome = text) }

    fun setObstacleText(key: Long, text: String) = edit { s ->
        s.copy(obstacles = s.obstacles.map { if (it.key == key) it.copy(text = text) else it })
    }

    fun setIfCue(key: Long, text: String) = edit { s ->
        s.copy(obstacles = s.obstacles.map { if (it.key == key) it.copy(ifCue = text) else it })
    }

    fun setThenAction(key: Long, text: String) = edit { s ->
        s.copy(obstacles = s.obstacles.map { if (it.key == key) it.copy(thenAction = text) else it })
    }

    fun addObstacle() = edit { s -> if (s.obstacles.size >= MAX_OBSTACLES) s else s.copy(obstacles = s.obstacles + ObstacleDraft()) }

    fun removeObstacle(key: Long) = edit { s ->
        val rest = s.obstacles.filterNot { it.key == key }
        s.copy(obstacles = rest.ifEmpty { listOf(ObstacleDraft()) })
    }

    fun setCheckIn(everyDays: Int) = _state.update { it.copy(checkInEveryDays = everyDays) }
    fun setCheckInTime(hour: Int, minute: Int) = _state.update { it.copy(checkInHour = hour, checkInMinute = minute) }

    fun goTo(step: CreateStep) {
        _state.update { s ->
            var next = s.copy(step = step)
            if (step == CreateStep.PLAN) {
                // The "if" starts as the obstacle itself; the user only has to add the "then".
                next = next.copy(
                    obstacles = s.obstacles
                        .filter { it.text.isNotBlank() }
                        .ifEmpty { s.obstacles }
                        .map { if (it.ifCue.isBlank()) it.copy(ifCue = cueFrom(it.text)) else it },
                )
            }
            next
        }
        scheduleDraftSave()
    }

    fun next() {
        val s = _state.value
        val step = when (s.step) {
            CreateStep.TIMEFRAME -> CreateStep.WISH
            CreateStep.WISH -> CreateStep.LIKELIHOOD
            CreateStep.LIKELIHOOD -> CreateStep.OUTCOME
            CreateStep.OUTCOME -> CreateStep.IMAGINE_OUTCOME
            CreateStep.IMAGINE_OUTCOME -> CreateStep.OBSTACLE
            CreateStep.OBSTACLE -> CreateStep.IMAGINE_OBSTACLE
            CreateStep.IMAGINE_OBSTACLE -> CreateStep.PLAN
            CreateStep.PLAN -> CreateStep.SUMMARY
            CreateStep.SUMMARY, CreateStep.DONE -> return
        }
        goTo(step)
    }

    /** Returns false when there is no earlier step (the caller should close the flow). */
    fun back(): Boolean {
        val step = when (_state.value.step) {
            CreateStep.TIMEFRAME, CreateStep.DONE -> return false
            CreateStep.WISH -> CreateStep.TIMEFRAME
            CreateStep.LIKELIHOOD -> CreateStep.WISH
            CreateStep.OUTCOME -> CreateStep.LIKELIHOOD
            CreateStep.IMAGINE_OUTCOME -> CreateStep.OUTCOME
            CreateStep.OBSTACLE -> CreateStep.IMAGINE_OUTCOME
            CreateStep.IMAGINE_OBSTACLE -> CreateStep.OBSTACLE
            CreateStep.PLAN -> CreateStep.IMAGINE_OBSTACLE
            CreateStep.SUMMARY -> CreateStep.PLAN
        }
        goTo(step)
        return true
    }

    private fun scheduleDraftSave() {
        if (_state.value.step == CreateStep.DONE) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            saveDraftNow()
        }
    }

    private suspend fun saveDraftNow() = saveLock.withLock {
        val s = _state.value
        if (s.step == CreateStep.DONE || (s.timeframe == null && s.wish.isBlank())) return@withLock
        val id = repository.saveGoal(s.toGoal(GoalStatus.DRAFT), s.toObstacles())
        _state.update { it.copy(goalId = id) }
    }

    /** Called when the user closes the flow mid-way, so nothing typed is lost. */
    fun saveAndClose(onClosed: () -> Unit) {
        viewModelScope.launch {
            saveJob?.cancelAndJoin()
            withContext(NonCancellable) { saveDraftNow() }
            onClosed()
        }
    }

    fun finish() {
        viewModelScope.launch {
            saveJob?.cancelAndJoin()
            val (goal, id) = saveLock.withLock {
                val s = _state.value
                val goal = s.toGoal(GoalStatus.ACTIVE)
                goal to repository.saveGoal(goal, s.toObstacles())
            }
            val s = _state.value
            val next = if (s.checkInEveryDays > 0) {
                Schedule.nextCheckIn(
                    ZonedDateTime.now(),
                    ReminderScheduler.checkInBase(goal),
                    s.checkInEveryDays,
                    s.checkInHour,
                    s.checkInMinute,
                )
            } else {
                null
            }
            _state.update { it.copy(goalId = id, step = CreateStep.DONE, nextCheckIn = next) }
        }
    }

    private fun CreateState.toGoal(status: GoalStatus) = GoalEntity(
        id = goalId,
        wish = wish.trim(),
        outcome = outcome.trim(),
        timeframe = timeframe ?: Timeframe.WEEK,
        likelihood = likelihood,
        status = status,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        checkInEveryDays = checkInEveryDays,
        checkInHour = checkInHour,
        checkInMinute = checkInMinute,
    )

    private fun CreateState.toObstacles() = obstacles
        .filter { it.text.isNotBlank() || it.thenAction.isNotBlank() }
        .mapIndexed { i, o ->
            ObstacleEntity(goalId = goalId, position = i, text = o.text.trim(), ifCue = o.ifCue.trim(), thenAction = o.thenAction.trim())
        }

    companion object {
        const val MAX_OBSTACLES = 3

        /** "I'll feel too tired after work." → "I feel too tired after work" */
        fun cueFrom(obstacle: String): String = obstacle.trim()
            .trimEnd('.', '!')
            .replace(Regex("^I'll ", RegexOption.IGNORE_CASE), "I ")
            .replace(Regex("^I will ", RegexOption.IGNORE_CASE), "I ")
    }
}
