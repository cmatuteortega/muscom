package com.muscom.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MusGameTest {
    private val base = listOf("12O 12C 1E 4B", "11O 7C 6E 5B", "1O 2C 4E 5O", "10O 10C 7O 6O")

    private fun rigged(scores: IntArray? = null): MusGame {
        val g = MusGame(MusConfig(), "rigged")
        g.rig(base.map(Cards::parseHand), scores)
        g.apply(0, MusAction.NoMus)
        return g
    }

    private fun MusGame.passLance() {
        val p = phase
        while (phase == p && betting != null) apply(turn!!, MusAction.Paso)
    }

    @Test
    fun allPassScoring() {
        val g = rigged()
        while (g.betting != null) g.passLance()
        assertEquals(Phase.FIN_MANO, g.phase)
        assertEquals(listOf(4, 2), g.scores.toList())
    }

    @Test
    fun envidoNoQueridoPaysDeje() {
        val g = rigged()
        g.apply(0, MusAction.Envido(2))
        g.apply(1, MusAction.NoQuiero)
        assertEquals(3, g.turn)
        g.apply(3, MusAction.NoQuiero)
        assertEquals(Phase.CHICA, g.phase)
        assertEquals(listOf(1, 0), g.scores.toList())
    }

    @Test
    fun ordagoQueridoEndsGame() {
        val g = rigged(intArrayOf(10, 30))
        g.apply(0, MusAction.Ordago)
        g.apply(1, MusAction.Quiero)
        assertEquals(Phase.FIN_PARTIDA, g.phase)
        assertEquals(0, g.winner)
        assertEquals(40, g.scores[0])
    }

    @Test
    fun viewHidesOtherHands() {
        val g = MusGame(MusConfig(), "v")
        val v = g.view(1)
        assertEquals(g.hands[1], v.hand)
        assertNull(v.revealed)
        g.apply(1, MusAction.SenaAction(Sena.DUPLES))
        assertTrue(g.view(0).senas.isEmpty())
        assertEquals(1, g.view(3).senas.size)
    }

    @Test
    fun illegalActionsThrow() {
        val g = rigged()
        assertThrows(IllegalActionException::class.java) { g.apply(1, MusAction.Paso) }
        assertThrows(IllegalActionException::class.java) { g.apply(0, MusAction.Quiero) }
        assertThrows(IllegalActionException::class.java) { g.apply(0, MusAction.Envido(1)) }
    }

    @Test
    fun botsPlayFullGames() {
        for (level in BotLevel.entries) {
            val rng = Rng.fromSeed("bots-$level")
            val g = MusGame(MusConfig(), "bots-$level")
            var n = 0
            while (g.phase != Phase.FIN_PARTIDA) {
                if (g.phase == Phase.GRANDE && g.view(0).senas.isEmpty() && g.betting?.queue?.size == 4) {
                    for (s in 0..3) Bots.chooseSenas(g.hands[s], 8).forEach { g.apply(s, MusAction.SenaAction(it)) }
                }
                val seat = g.actors().first()
                g.apply(seat, Bots.decide(g.view(seat), level, rng))
                check(++n < 3000)
            }
            assertNotNull(g.winner)
            assertEquals(40, g.scores.max())
        }
    }
}
