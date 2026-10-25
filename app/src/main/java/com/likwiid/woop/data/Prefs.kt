// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("woop_prefs", Context.MODE_PRIVATE)

    private val _onboardingDone = MutableStateFlow(sp.getBoolean(KEY_ONBOARDING, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == sp.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setOnboardingDone(done: Boolean) {
        sp.edit { putBoolean(KEY_ONBOARDING, done) }
        _onboardingDone.value = done
    }

    fun setThemeMode(mode: ThemeMode) {
        sp.edit { putString(KEY_THEME, mode.name) }
        _themeMode.value = mode
    }

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_THEME = "theme_mode"
    }
}
