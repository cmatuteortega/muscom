package com.muscom.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muscom.app.engine.BotLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val onboardingDone: Boolean = false,
    val playerName: String = "",
    val botLevel: BotLevel = BotLevel.NORMAL,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val highContrast: Boolean = false,
    val textScale: Float = 1f,
    /** 0 lento, 1 normal, 2 rápido. */
    val botSpeed: Int = 1,
    val reyes: Int = 8,
    val target: Int = 40,
    val turnSeconds: Int = 30,
    /** null = aún no se ha preguntado. */
    val analyticsConsent: Boolean? = null,
)

internal val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val name = stringPreferencesKey("player_name")
        val level = stringPreferencesKey("bot_level")
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val theme = stringPreferencesKey("theme")
        val contrast = booleanPreferencesKey("high_contrast")
        val textScale = floatPreferencesKey("text_scale")
        val botSpeed = intPreferencesKey("bot_speed")
        val reyes = intPreferencesKey("reyes")
        val target = intPreferencesKey("target")
        val turnSeconds = intPreferencesKey("turn_seconds")
        val consent = stringPreferencesKey("analytics_consent")
    }

    private fun fromPrefs(p: Preferences) = Settings(
        onboardingDone = p[K.onboarding] ?: false,
        playerName = p[K.name] ?: "",
        botLevel = p[K.level]?.let { runCatching { BotLevel.valueOf(it) }.getOrNull() } ?: BotLevel.NORMAL,
        sound = p[K.sound] ?: true,
        haptics = p[K.haptics] ?: true,
        themeMode = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        highContrast = p[K.contrast] ?: false,
        textScale = p[K.textScale] ?: 1f,
        botSpeed = p[K.botSpeed] ?: 1,
        reyes = p[K.reyes] ?: 8,
        target = p[K.target] ?: 40,
        turnSeconds = p[K.turnSeconds] ?: 30,
        analyticsConsent = p[K.consent]?.toBooleanStrictOrNull(),
    )

    val settings: Flow<Settings> = context.settingsStore.data.map(::fromPrefs)

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        context.settingsStore.edit { p ->
            val s = transform(fromPrefs(p))
            p[K.onboarding] = s.onboardingDone
            p[K.name] = s.playerName
            p[K.level] = s.botLevel.name
            p[K.sound] = s.sound
            p[K.haptics] = s.haptics
            p[K.theme] = s.themeMode.name
            p[K.contrast] = s.highContrast
            p[K.textScale] = s.textScale
            p[K.botSpeed] = s.botSpeed
            p[K.reyes] = s.reyes
            p[K.target] = s.target
            p[K.turnSeconds] = s.turnSeconds
            if (s.analyticsConsent == null) p.remove(K.consent) else p[K.consent] = s.analyticsConsent.toString()
        }
    }

    suspend fun clearAll() {
        context.settingsStore.edit { it.clear() }
    }
}
