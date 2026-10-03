package com.muscom.app.engine

/**
 * Port Kotlin de packages/mus-engine/src/mus/engine.ts.
 *
 * En el producto final el servidor es autoritativo y ejecuta el motor TypeScript;
 * este port sólo existe para el modo offline contra bots del prototipo. Su
 * equivalencia con el motor TS se verifica con los vectores de conformidad.
 */
class MusGame(val config: MusConfig = MusConfig(), val seed: String) {
    private val rng: Rng = Rng.fromSeed(seed)

    val scores = intArrayOf(0, 0)
    var dealer = config.firstDealer; private set
    var handNumber = 0; private set
    private var deck = mutableListOf<Card>()
    private var discardPile = mutableListOf<Card>()
    val hands: List<MutableList<Card>> = List(4) { mutableListOf() }
    var phase = Phase.MUS; private set
    var turn: Int? = null; private set
    private var musRound = 0
    private var pendingDiscard = mutableListOf<Int>()
    private val lastDiscardCounts = IntArray(4)
    var betting: Betting? = null; private set
    val lances = mutableListOf<LanceRecord>()
    private var declarations = Declarations(null, null)
    private val senas = mutableListOf<SenaRecord>()
    val awards = mutableListOf<Award>()
    var summary: HandSummary? = null; private set
    var winner: Int? = null; private set
    val history = mutableListOf<LogEntry>()

    init {
        require(config.reyes == 4 || config.reyes == 8)
        startHand(config.firstDealer)
    }

    val mano get() = manoOf(dealer)

    fun manoOrder(): List<Int> = List(4) { (mano + it) % 4 }

    // ───────── reparto ─────────

    private fun startHand(newDealer: Int) {
        dealer = newDealer
        handNumber += 1
        deck = Cards.fullDeck().shuffleWith(rng)
        discardPile = mutableListOf()
        hands.forEach { it.clear() }
        val order = manoOrder()
        repeat(4) { for (seat in order) hands[seat].add(deck.removeAt(0)) }
        phase = Phase.MUS
        turn = order[0]
        musRound = 0
        pendingDiscard = mutableListOf()
        lastDiscardCounts.fill(0)
        betting = null
        lances.clear()
        declarations = Declarations(null, null)
        senas.clear()
        awards.clear()
        summary = null
    }

    private fun drawCard(): Card {
        if (deck.isEmpty()) {
            deck = discardPile.shuffleWith(rng)
            discardPile = mutableListOf()
        }
        return deck.removeAt(0)
    }

    // ───────── lances ─────────

    private fun startBetting(lance: Lance, participants: List<Int>) {
        phase = Phase.of(lance)
        betting = Betting(lance, participants, participants, 0, 0, null, false)
        turn = participants[0]
    }

    private fun pay(lance: Lance, team: Int, points: Int, kind: AwardKind): Boolean {
        if (points <= 0) return false
        val capped = minOf(points, config.target - scores[team])
        scores[team] += capped
        awards.add(Award(lance, team, capped, kind))
        if (scores[team] >= config.target) {
            winner = team
            return true
        }
        return false
    }

    private fun advanceAfter(lance: Lance) {
        betting = null
        val order = manoOrder()
        when (lance) {
            Lance.GRANDE -> startBetting(Lance.CHICA, order)
            Lance.CHICA -> {
                val decl = hands.map { Evaluate.hasPares(it, config.reyes) }
                declarations = declarations.copy(pares = decl)
                val holders = order.filter { decl[it] }
                val teams = holders.map(::teamOf).toSet()
                if (teams.size == 2) return startBetting(Lance.PARES, holders)
                lances.add(LanceRecord(Lance.PARES, if (holders.isNotEmpty()) LanceOutcome.SIN_APUESTA else LanceOutcome.NO_HAY))
                enterJuego()
            }
            Lance.PARES -> enterJuego()
            else -> conteo()
        }
    }

    private fun enterJuego() {
        val order = manoOrder()
        val decl = hands.map { Evaluate.hasJuego(it, config.reyes) }
        declarations = declarations.copy(juego = decl)
        val holders = order.filter { decl[it] }
        if (holders.isEmpty()) return startBetting(Lance.PUNTO, order)
        if (holders.map(::teamOf).toSet().size == 2) return startBetting(Lance.JUEGO, holders)
        lances.add(LanceRecord(Lance.JUEGO, LanceOutcome.SIN_APUESTA))
        conteo()
    }

    fun lanceWinner(lance: Lance): Int {
        val order = manoOrder()
        val seats = when (lance) {
            Lance.PARES -> order.filter { Evaluate.hasPares(hands[it], config.reyes) }
            Lance.JUEGO -> order.filter { Evaluate.hasJuego(hands[it], config.reyes) }
            else -> order
        }
        return Evaluate.bestSeat(hands, seats, lance, config.reyes)
    }

    private fun finishGame(winners: Map<Lance, Int>) {
        betting = null
        turn = null
        phase = Phase.FIN_PARTIDA
        summary = HandSummary(hands.map { it.toList() }, winners, awards.toList(), true)
    }

    private fun resolveOrdago(lance: Lance) {
        val w = lanceWinner(lance)
        val team = teamOf(w)
        lances.add(LanceRecord(lance, LanceOutcome.QUERIDO, 0, null, true))
        pay(lance, team, config.target - scores[team], AwardKind.ORDAGO)
        winner = team
        finishGame(mapOf(lance to w))
    }

    private fun conteo() {
        betting = null
        turn = null
        val reyes = config.reyes
        val winners = linkedMapOf<Lance, Int>()
        var over = winner != null
        for (rec in lances) {
            if (rec.outcome == LanceOutcome.NO_HAY) continue
            val w = lanceWinner(rec.lance)
            winners[rec.lance] = w
            val team = teamOf(w)
            if (over) continue
            val payments = mutableListOf<Pair<Int, AwardKind>>()
            if (rec.outcome == LanceOutcome.QUERIDO) payments.add(rec.amount to AwardKind.APUESTA)
            when (rec.lance) {
                Lance.GRANDE, Lance.CHICA -> if (rec.outcome == LanceOutcome.PASO) payments.add(1 to AwardKind.PASO)
                Lance.PARES -> payments.add(
                    hands.withIndex().sumOf { (seat, h) ->
                        if (teamOf(seat) == team) Evaluate.paresPoints(Evaluate.paresOf(h, reyes)) else 0
                    } to AwardKind.JUGADA,
                )
                Lance.JUEGO -> payments.add(
                    hands.withIndex().sumOf { (seat, h) ->
                        val p = Evaluate.handPoints(h, reyes)
                        if (teamOf(seat) == team && p >= 31) Evaluate.juegoPoints(p) else 0
                    } to AwardKind.JUGADA,
                )
                Lance.PUNTO -> payments.add(1 to AwardKind.JUGADA)
            }
            for ((pts, kind) in payments) {
                if (pay(rec.lance, team, pts, kind)) {
                    over = true
                    break
                }
            }
        }
        summary = HandSummary(hands.map { it.toList() }, winners, awards.toList(), over)
        phase = if (over) Phase.FIN_PARTIDA else Phase.FIN_MANO
    }

    // ───────── validación ─────────

    fun actors(): List<Int> = when (phase) {
        Phase.FIN_PARTIDA -> emptyList()
        Phase.FIN_MANO -> listOf(0, 1, 2, 3)
        Phase.DESCARTE -> pendingDiscard.toList()
        else -> listOfNotNull(turn)
    }

    fun validate(seat: Int, a: MusAction): String? {
        if (seat !in 0..3) return "Asiento inválido"
        if (phase == Phase.FIN_PARTIDA) return "La partida ha terminado"
        if (a is MusAction.SenaAction) {
            if (phase == Phase.FIN_MANO) return "Ahora no se pueden hacer señas"
            if (senas.count { it.seat == seat } >= config.maxSenasPerHand) return "Demasiadas señas en esta mano"
            return null
        }
        if (a is MusAction.SiguienteMano) return if (phase == Phase.FIN_MANO) null else "La mano no ha terminado"
        if (phase == Phase.DESCARTE) {
            if (a !is MusAction.Descarte) return "Toca descartar"
            if (seat !in pendingDiscard) return "Ya has descartado"
            if (a.cards.isEmpty() || a.cards.size > 4) return "Descarta entre 1 y 4 cartas"
            if (a.cards.toSet().size != a.cards.size) return "Cartas repetidas"
            if (!a.cards.all { it in hands[seat] }) return "No tienes esa carta"
            return null
        }
        if (turn != seat) return "No es tu turno"
        if (phase == Phase.MUS) return if (a is MusAction.Mus || a is MusAction.NoMus) null else "Di mus o no hay mus"
        if (phase == Phase.FIN_MANO) return "La mano ha terminado"
        val b = betting!!
        val hasBet = b.proposed > 0 || b.ordago
        return when (a) {
            MusAction.Paso -> if (hasBet) "Hay una apuesta" else null
            MusAction.Quiero, MusAction.NoQuiero -> if (hasBet) null else "No hay apuesta"
            is MusAction.Envido -> when {
                b.ordago -> "Al órdago sólo se puede querer o no querer"
                a.amount < 2 || a.amount > 99 -> "Envite inválido"
                else -> null
            }
            MusAction.Ordago -> if (b.ordago) "Ya hay un órdago" else null
            else -> "Acción no válida en este lance"
        }
    }

    fun legalActions(seat: Int): List<MusAction> {
        if (seat !in actors()) return emptyList()
        return when (phase) {
            Phase.FIN_MANO -> listOf(MusAction.SiguienteMano)
            Phase.MUS -> listOf(MusAction.Mus, MusAction.NoMus)
            Phase.DESCARTE -> {
                val hand = hands[seat]
                (1 until (1 shl hand.size)).map { mask ->
                    MusAction.Descarte(hand.filterIndexed { i, _ -> mask and (1 shl i) != 0 })
                }
            }
            else -> {
                val b = betting!!
                when {
                    b.ordago -> listOf(MusAction.Quiero, MusAction.NoQuiero)
                    b.proposed == 0 -> listOf(MusAction.Paso, MusAction.Envido(2), MusAction.Envido(5), MusAction.Ordago)
                    else -> listOf(MusAction.Quiero, MusAction.NoQuiero, MusAction.Envido(2), MusAction.Envido(5), MusAction.Ordago)
                }
            }
        }
    }

    // ───────── aplicar ─────────

    fun apply(seat: Int, a: MusAction) {
        validate(seat, a)?.let { throw IllegalActionException(it) }
        history.add(LogEntry(handNumber, phase, seat, a))
        when (a) {
            is MusAction.SenaAction -> senas.add(SenaRecord(seat, a.sena))
            MusAction.SiguienteMano -> startHand(manoOf(dealer))
            MusAction.Mus -> {
                val order = manoOrder()
                val idx = order.indexOf(seat)
                if (idx == 3) {
                    phase = Phase.DESCARTE
                    turn = null
                    pendingDiscard = order.toMutableList()
                    lastDiscardCounts.fill(0)
                } else {
                    turn = order[idx + 1]
                }
            }
            MusAction.NoMus -> startBetting(Lance.GRANDE, manoOrder())
            is MusAction.Descarte -> {
                hands[seat].removeAll(a.cards.toSet())
                discardPile.addAll(a.cards)
                lastDiscardCounts[seat] = a.cards.size
                pendingDiscard.remove(seat)
                if (pendingDiscard.isEmpty()) {
                    for (p in manoOrder()) while (hands[p].size < 4) hands[p].add(drawCard())
                    phase = Phase.MUS
                    turn = mano
                    musRound += 1
                }
            }
            else -> applyBetting(seat, a)
        }
    }

    private fun opponentsAfter(participants: List<Int>, seat: Int): List<Int> =
        (1..4).map { (seat + it) % 4 }.filter { it in participants && teamOf(it) != teamOf(seat) }

    private fun applyBetting(seat: Int, a: MusAction) {
        var b = betting!!
        val lance = b.lance
        val team = teamOf(seat)
        when (a) {
            MusAction.Paso -> {
                b = b.copy(queue = b.queue.drop(1))
                if (b.queue.isEmpty()) {
                    lances.add(LanceRecord(lance, LanceOutcome.PASO))
                    return advanceAfter(lance)
                }
            }
            is MusAction.Envido -> {
                b = if (b.proposed == 0) b.copy(proposed = a.amount, deje = 1)
                else b.copy(deje = b.proposed, proposed = b.proposed + a.amount)
                b = b.copy(bettingTeam = team, queue = opponentsAfter(b.participants, seat))
            }
            MusAction.Ordago -> {
                b = b.copy(
                    deje = if (b.proposed == 0) 1 else b.proposed,
                    ordago = true,
                    bettingTeam = team,
                    queue = opponentsAfter(b.participants, seat),
                )
            }
            MusAction.Quiero -> {
                if (b.ordago) return resolveOrdago(lance)
                lances.add(LanceRecord(lance, LanceOutcome.QUERIDO, b.proposed))
                return advanceAfter(lance)
            }
            MusAction.NoQuiero -> {
                b = b.copy(queue = b.queue.drop(1))
                if (b.queue.isEmpty()) {
                    val t = b.bettingTeam!!
                    lances.add(LanceRecord(lance, LanceOutcome.NO_QUERIDO, b.deje, t, b.ordago))
                    if (pay(lance, t, b.deje, AwardKind.DEJE)) return finishGame(emptyMap())
                    return advanceAfter(lance)
                }
            }
            else -> error("no es una acción de envite")
        }
        betting = b
        turn = b.queue[0]
    }

    // ───────── vista ─────────

    fun view(seat: Int): MusView {
        val ended = phase == Phase.FIN_MANO || phase == Phase.FIN_PARTIDA
        val visible = { s: Int -> s == seat || s == partnerOf(seat) }
        return MusView(
            seat = seat,
            phase = phase,
            turn = turn,
            dealer = dealer,
            mano = mano,
            handNumber = handNumber,
            scores = scores.toList(),
            config = config,
            hand = hands[seat].toList(),
            musRound = musRound,
            pendingDiscard = pendingDiscard.toList(),
            lastDiscardCounts = lastDiscardCounts.toList(),
            betting = betting,
            lances = lances.toList(),
            awards = awards.toList(),
            declarations = declarations,
            senas = senas.filter { visible(it.seat) },
            log = history.filter { it.hand == handNumber && (it.action !is MusAction.SenaAction || visible(it.seat)) },
            revealed = if (ended) hands.map { it.toList() } else null,
            summary = summary,
            winner = winner,
            legal = legalActions(seat),
        )
    }

    /** Sólo para pruebas: fija manos y marcador. */
    internal fun rig(newHands: List<List<Card>>, newScores: IntArray? = null) {
        newHands.forEachIndexed { i, h -> hands[i].clear(); hands[i].addAll(h) }
        newScores?.copyInto(scores)
    }

    companion object {
        fun manoOf(dealer: Int) = (dealer + 1) % 4
    }
}
