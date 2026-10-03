package com.muscom.app.ui.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muscom.app.data.Settings
import com.muscom.app.data.SettingsRepository
import com.muscom.app.data.StatsRepository
import com.muscom.app.engine.BotLevel
import com.muscom.app.engine.Bots
import com.muscom.app.engine.Card
import com.muscom.app.engine.HandSummary
import com.muscom.app.engine.LanceRecord
import com.muscom.app.engine.MusAction
import com.muscom.app.engine.MusConfig
import com.muscom.app.engine.MusGame
import com.muscom.app.engine.MusView
import com.muscom.app.engine.Phase
import com.muscom.app.engine.Rng
import com.muscom.app.engine.Sena
import com.muscom.app.engine.partnerOf
import com.muscom.app.feedback.Cue
import com.muscom.app.feedback.Feedback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject

const val HUMAN = 0

data class Bubble(val id: Long, val action: MusAction, val phase: Phase)
data class TurnTimer(val seat: Int, val startedAt: Long, val durationMs: Long)

data class GameUiState(
    val view: MusView? = null,
    val names: List<String> = listOf("", "", "", ""),
    val level: BotLevel = BotLevel.NORMAL,
    val bubbles: Map<Int, Bubble> = emptyMap(),
    val selected: Set<Card> = emptySet(),
    val envidoAmount: Int = 2,
    val timer: TurnTimer? = null,
    val banner: LanceRecord? = null,
    val pastHands: List<HandSummary> = emptyList(),
    val seed: String = "",
    val dealKey: Int = 0,
) {
    val myTurn: Boolean
        get() = view != null && HUMAN in actorsOf(view)

    companion object {
        fun actorsOf(v: MusView): List<Int> = when (v.phase) {
            Phase.FIN_PARTIDA -> emptyList()
            Phase.FIN_MANO -> listOf(0, 1, 2, 3)
            Phase.DESCARTE -> v.pendingDiscard
            else -> listOfNotNull(v.turn)
        }
    }
}

/**
 * Partida offline contra bots. Toda la lógica de reglas vive en el motor; el
 * ViewModel sólo orquesta turnos, tiempos y presentación.
 * En fase 3 este ViewModel consume el WebSocket del servidor en lugar de MusGame.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val statsRepo: StatsRepository,
    private val feedback: Feedback,
) : ViewModel() {

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var game: MusGame? = null
    private var botRng = Rng.fromSeed("bots")
    private var settings = Settings()
    private var loopJob: Job? = null
    private var timerJob: Job? = null
    private var seenHistory = 0
    private var seenLances = 0
    private var bubbleSeq = 0L
    private var statsRecorded = false
    private var senasSentForHand = 0
    private var lastTurnSignal: Pair<Int, Int>? = null

    fun start(names: List<String>, level: BotLevel? = null) {
        viewModelScope.launch {
            settings = settingsRepo.current()
            feedback.soundEnabled = settings.sound
            feedback.hapticsEnabled = settings.haptics
            val lvl = level ?: settings.botLevel
            val seed = newSeed()
            val g = MusGame(MusConfig(reyes = settings.reyes, target = settings.target), seed)
            game = g
            botRng = Rng.fromSeed("$seed-bots")
            seenHistory = 0
            seenLances = 0
            statsRecorded = false
            senasSentForHand = 0
            lastTurnSignal = null
            _ui.value = GameUiState(names = names, level = lvl, seed = seed, dealKey = 1)
            feedback.play(Cue.DEAL)
            pump()
        }
    }

    private fun newSeed(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ───────── acciones del humano ─────────

    fun act(action: MusAction) {
        val g = game ?: return
        if (g.validate(HUMAN, action) != null) return
        apply(HUMAN, action)
        pump()
    }

    fun toggleCard(card: Card) {
        val v = _ui.value.view ?: return
        if (v.phase != Phase.DESCARTE || HUMAN !in v.pendingDiscard) return
        feedback.play(Cue.TAP)
        _ui.update { s -> s.copy(selected = if (card in s.selected) s.selected - card else s.selected + card) }
    }

    fun discardSelected() {
        val sel = _ui.value.selected
        if (sel.isEmpty()) return
        // Respetar el orden de la mano para que el log sea estable.
        val hand = _ui.value.view?.hand ?: return
        act(MusAction.Descarte(hand.filter { it in sel }))
    }

    fun setEnvido(amount: Int) = _ui.update { it.copy(envidoAmount = amount.coerceIn(2, 30)) }

    fun sena(sena: Sena) {
        val g = game ?: return
        if (g.validate(HUMAN, MusAction.SenaAction(sena)) != null) return
        apply(HUMAN, MusAction.SenaAction(sena))
        publish()
    }

    fun nextHand() {
        val g = game ?: return
        if (g.phase != Phase.FIN_MANO) return
        apply(HUMAN, MusAction.SiguienteMano)
        _ui.update { it.copy(dealKey = it.dealKey + 1, bubbles = emptyMap(), banner = null) }
        feedback.play(Cue.DEAL)
        pump()
    }

    fun rematch() = start(_ui.value.names, _ui.value.level)

    // ───────── bucle ─────────

    private fun apply(seat: Int, action: MusAction) {
        val g = game ?: return
        val musRoundBefore = g.view(HUMAN).musRound
        g.apply(seat, action)
        if (action is MusAction.Ordago) feedback.play(Cue.ORDAGO)
        else if (action is MusAction.Envido) feedback.play(Cue.BET)
        if (g.view(HUMAN).musRound != musRoundBefore) {
            _ui.update { it.copy(dealKey = it.dealKey + 1, selected = emptySet()) }
        }
    }

    /** Publica la vista y decide quién actúa después. */
    private fun pump() {
        loopJob?.cancel()
        timerJob?.cancel()
        val g = game ?: return
        maybeBotSenas(g)
        publish()
        when (g.phase) {
            Phase.FIN_PARTIDA -> onGameOver(g)
            Phase.FIN_MANO -> {
                _ui.update { it.copy(timer = null) }
            }
            Phase.DESCARTE -> {
                val bots = g.actors().filter { it != HUMAN }
                if (HUMAN in g.actors()) startHumanTimer()
                if (bots.isNotEmpty()) loopJob = viewModelScope.launch {
                    for (seat in bots) {
                        delay(thinkDelay() / 2)
                        if (game !== g || g.phase != Phase.DESCARTE || seat !in g.actors()) return@launch
                        val view = g.view(seat)
                        val a = withContext(Dispatchers.Default) { Bots.decide(view, _ui.value.level, botRng) }
                        apply(seat, a)
                        publish()
                    }
                    if (g.phase != Phase.DESCARTE) pump()
                }
            }
            else -> {
                val seat = g.actors().firstOrNull() ?: return
                if (seat == HUMAN) {
                    startHumanTimer()
                } else {
                    val think = thinkDelay()
                    val started = SystemClock.elapsedRealtime()
                    _ui.update { it.copy(timer = TurnTimer(seat, started, think)) }
                    loopJob = viewModelScope.launch {
                        val view = g.view(seat)
                        val a = withContext(Dispatchers.Default) { Bots.decide(view, _ui.value.level, botRng) }
                        val remaining = think - (SystemClock.elapsedRealtime() - started)
                        if (remaining > 0) delay(remaining)
                        if (game !== g) return@launch
                        apply(seat, a)
                        pump()
                    }
                }
            }
        }
    }

    private fun startHumanTimer() {
        val g = game ?: return
        val ms = settings.turnSeconds * 1000L
        _ui.update { it.copy(timer = TurnTimer(HUMAN, SystemClock.elapsedRealtime(), ms)) }
        val signal = g.handNumber to g.history.size
        if (lastTurnSignal != signal) {
            lastTurnSignal = signal
            feedback.play(Cue.MY_TURN)
        }
        timerJob = viewModelScope.launch {
            delay(ms)
            if (game !== g || HUMAN !in g.actors()) return@launch
            // Tiempo agotado: decide un bot prudente por el jugador.
            val a = withContext(Dispatchers.Default) { Bots.decide(g.view(HUMAN), BotLevel.NORMAL, botRng) }
            apply(HUMAN, a)
            pump()
        }
    }

    /** Al empezar los lances, los bots hacen sus señas (el humano sólo ve las del compañero). */
    private fun maybeBotSenas(g: MusGame) {
        if (g.phase == Phase.GRANDE && senasSentForHand != g.handNumber) {
            senasSentForHand = g.handNumber
            for (seat in 1..3) {
                for (s in Bots.chooseSenas(g.hands[seat], g.config.reyes)) {
                    if (g.validate(seat, MusAction.SenaAction(s)) == null) g.apply(seat, MusAction.SenaAction(s))
                }
            }
        }
    }

    private fun thinkDelay(): Long = when (settings.botSpeed) {
        0 -> 1600L
        2 -> 450L
        else -> 950L
    } + kotlin.random.Random.nextLong(350)

    private fun publish() {
        val g = game ?: return
        val view = g.view(HUMAN)
        val newBubbles = _ui.value.bubbles.toMutableMap()
        // Burbujas: últimas acciones visibles para el humano.
        val visibleLog = g.history.drop(seenHistory)
        seenHistory = g.history.size
        for (e in visibleLog) {
            val isSena = e.action is MusAction.SenaAction
            if (isSena && e.seat != partnerOf(HUMAN) && e.seat != HUMAN) continue
            if (e.action is MusAction.SiguienteMano) continue
            val b = Bubble(++bubbleSeq, e.action, e.phase)
            newBubbles[e.seat] = b
            scheduleBubbleExpiry(e.seat, b.id, if (isSena) 4000 else 2600)
        }
        // Banner con el resultado del lance recién cerrado.
        var banner = _ui.value.banner
        if (view.lances.size > seenLances) {
            banner = view.lances.last()
            seenLances = view.lances.size
            scheduleBannerExpiry(banner)
        }
        if (view.phase == Phase.MUS && view.lances.isEmpty()) seenLances = 0
        val past = if ((view.phase == Phase.FIN_MANO || view.phase == Phase.FIN_PARTIDA) && view.summary != null &&
            _ui.value.pastHands.size < view.handNumber
        ) _ui.value.pastHands + view.summary else _ui.value.pastHands
        _ui.update {
            it.copy(
                view = view,
                bubbles = newBubbles,
                banner = banner,
                pastHands = past,
                selected = if (view.phase == Phase.DESCARTE) it.selected.filter { c -> c in view.hand }.toSet() else emptySet(),
                envidoAmount = if (view.betting?.proposed == 0) it.envidoAmount else 2,
                timer = if (HUMAN in GameUiState.actorsOf(view) || it.timer?.seat != HUMAN) it.timer else null,
            )
        }
    }

    private fun scheduleBubbleExpiry(seat: Int, id: Long, ms: Long) {
        viewModelScope.launch {
            delay(ms)
            _ui.update { s -> if (s.bubbles[seat]?.id == id) s.copy(bubbles = s.bubbles - seat) else s }
        }
    }

    private fun scheduleBannerExpiry(rec: LanceRecord) {
        viewModelScope.launch {
            delay(2200)
            _ui.update { s -> if (s.banner === rec) s.copy(banner = null) else s }
        }
    }

    private fun onGameOver(g: MusGame) {
        _ui.update { it.copy(timer = null) }
        if (statsRecorded) return
        statsRecorded = true
        val won = g.winner == 0
        feedback.play(if (won) Cue.WIN else Cue.LOSE)
        val ordago = g.lances.any { it.ordago && it.outcome == com.muscom.app.engine.LanceOutcome.QUERIDO }
        viewModelScope.launch {
            statsRepo.recordGame(won, g.handNumber, ordago, g.scores[0], g.scores[1])
        }
    }

    override fun onCleared() {
        loopJob?.cancel()
        timerJob?.cancel()
    }
}
