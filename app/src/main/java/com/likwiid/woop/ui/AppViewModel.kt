// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.likwiid.woop.AppContainer
import com.likwiid.woop.WoopApp

/** `viewModel()` wired to the app container; nav arguments arrive through the SavedStateHandle. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (container: AppContainer, savedState: SavedStateHandle) -> VM,
): VM = viewModel(
    factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as WoopApp
            create(app.container, createSavedStateHandle())
        }
    },
)
