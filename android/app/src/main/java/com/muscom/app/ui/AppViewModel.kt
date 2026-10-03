package com.muscom.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muscom.app.data.Settings
import com.muscom.app.data.SettingsRepository
import com.muscom.app.data.Stats
import com.muscom.app.data.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Ajustes y estadísticas compartidos por todas las pantallas. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val statsRepo: StatsRepository,
) : ViewModel() {
    /** null mientras se cargan (evita parpadeo del onboarding). */
    val settings: StateFlow<Settings?> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val stats: StateFlow<Stats> = statsRepo.stats.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    /** Borra todos los datos locales (equivalente offline del borrado de cuenta). */
    fun deleteLocalData() {
        viewModelScope.launch {
            statsRepo.clear()
            settingsRepo.clearAll()
        }
    }
}
