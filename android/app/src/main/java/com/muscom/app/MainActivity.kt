package com.muscom.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muscom.app.ui.AppViewModel
import com.muscom.app.ui.nav.AppNav
import com.muscom.app.ui.theme.MusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by app.settings.collectAsStateWithLifecycle()
            val s = settings
            if (s == null) {
                Box(Modifier.fillMaxSize())
                return@setContent
            }
            MusTheme(themeMode = s.themeMode, highContrast = s.highContrast, textScale = s.textScale) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AppNav(app, s)
                }
            }
        }
    }
}
