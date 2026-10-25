// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.likwiid.woop.data.ThemeMode
import com.likwiid.woop.ui.nav.WoopNavHost
import com.likwiid.woop.ui.theme.WoopTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** Where a notification or the widget asked us to go ("create", "review/<id>" or "goal/<id>"). */
    private val pendingDest = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingDest.value = intent?.getStringExtra(EXTRA_DEST)
        val container = (application as WoopApp).container

        setContent {
            val themeMode by container.prefs.themeMode.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            WoopTheme(darkTheme = dark) {
                WoopNavHost(container = container, pendingDest = pendingDest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_DEST)?.let { pendingDest.value = it }
    }

    companion object {
        const val EXTRA_DEST = "dest"
    }
}
