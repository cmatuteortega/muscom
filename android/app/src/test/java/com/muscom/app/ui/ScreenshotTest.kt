package com.muscom.app.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.muscom.app.data.ThemeMode
import com.muscom.app.engine.Cards
import com.muscom.app.engine.MusAction
import com.muscom.app.engine.MusConfig
import com.muscom.app.engine.MusGame
import com.muscom.app.engine.Phase
import com.muscom.app.engine.Sena
import com.muscom.app.ui.game.Bubble
import com.muscom.app.ui.game.GameScreen
import com.muscom.app.ui.game.GameUiState
import com.muscom.app.ui.game.TurnTimer
import com.muscom.app.ui.home.HomeScreen
import com.muscom.app.engine.BotLevel
import com.muscom.app.ui.theme.MusTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import org.junit.Rule
import org.junit.Test

/**
 * Capturas de pantalla en JVM (Paparazzi). `./gradlew :app:recordPaparazziDebug`
 * las guarda en app/src/test/snapshots; CI las publica como artefacto.
 */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 1.0)

    private val names = listOf("Ane", "Koldo", "Maite", "Rosi")

    private fun gameIn(phase: Phase): GameUiState {
        val g = MusGame(MusConfig(), "screens")
        g.rig(listOf("12O 3C 7E 7B", "11O 7C 6E 5B", "1O 2C 4E 5O", "10O 10C 7O 6O").map(Cards::parseHand), intArrayOf(17, 23))
        g.apply(2, MusAction.SenaAction(Sena.DOS_ASES))
        when (phase) {
            Phase.MUS -> {}
            Phase.GRANDE -> {
                g.apply(0, MusAction.NoMus)
                g.apply(0, MusAction.Paso)
                g.apply(1, MusAction.Envido(2))
                g.apply(2, MusAction.Paso.let { MusAction.NoQuiero })
            }
            else -> {
                g.apply(0, MusAction.NoMus)
                while (g.phase != Phase.FIN_MANO && g.phase != Phase.FIN_PARTIDA) g.apply(g.turn!!, MusAction.Paso)
            }
        }
        val v = g.view(0)
        return GameUiState(
            view = v, names = names,
            bubbles = if (phase == Phase.GRANDE) mapOf(1 to Bubble(1, MusAction.Envido(2), Phase.GRANDE), 2 to Bubble(2, MusAction.NoQuiero, Phase.GRANDE)) else mapOf(2 to Bubble(3, MusAction.SenaAction(Sena.DOS_ASES), Phase.MUS)),
            timer = TurnTimer(0, 0, 30_000),
        )
    }

    private fun table(state: GameUiState, dark: Boolean = false) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MusTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    GameScreen(state, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
    }

    @Test fun mesaMus() = table(gameIn(Phase.MUS))
    @Test fun mesaGrandeEnvite() = table(gameIn(Phase.GRANDE))
    @Test fun mesaFinMano() = table(gameIn(Phase.FIN_MANO), dark = true)

    @Test
    fun inicio() {
        paparazzi.snapshot {
            MusTheme(themeMode = ThemeMode.LIGHT) {
                HomeScreen("Ane", BotLevel.NORMAL, {}, {}, {}, {}, {})
            }
        }
    }
}
