package com.muscom.app.engine

/**
 * Port de packages/mus-engine/src/bots/index.ts. Los bots sólo ven su MusView.
 */
enum class BotLevel { FACIL, NORMAL, DIFICIL }

private data class LevelParams(
    val samples: Int,
    val noise: Double,
    val bluff: Double,
    val betThreshold: Double,
    val acceptThreshold: Double,
    val raiseThreshold: Double,
    val useSenas: Boolean,
    val scoreAware: Boolean,
)

private val PARAMS = mapOf(
    BotLevel.FACIL to LevelParams(40, 0.25, 0.12, 0.6, 0.4, 0.92, false, false),
    BotLevel.NORMAL to LevelParams(160, 0.08, 0.05, 0.66, 0.5, 0.84, true, false),
    BotLevel.DIFICIL to LevelParams(400, 0.03, 0.07, 0.62, 0.47, 0.8, true, true),
)

data class Estimate(val grande: Double, val chica: Double, val pares: Double, val juego: Double, val punto: Double) {
    operator fun get(l: Lance) = when (l) {
        Lance.GRANDE -> grande
        Lance.CHICA -> chica
        Lance.PARES -> pares
        Lance.JUEGO -> juego
        Lance.PUNTO -> punto
    }
}

object Bots {

    fun chooseSenas(hand: List<Card>, reyes: Int): List<Sena> {
        val out = mutableListOf<Sena>()
        val p = Evaluate.paresOf(hand, reyes)
        val pts = Evaluate.handPoints(hand, reyes)
        val reyesN = hand.count { Cards.effRank(it, reyes) == 12 }
        val asesN = hand.count { Cards.effRank(it, reyes) == 1 }
        if (pts == 31) out += Sena.TREINTA_Y_UNA
        if (p.cat == 3) out += Sena.DUPLES else if (p.cat == 2) out += Sena.MEDIAS
        if (reyesN >= 3) out += Sena.TRES_REYES else if (reyesN == 2) out += Sena.DOS_REYES
        if (asesN >= 3) out += Sena.TRES_ASES else if (asesN == 2) out += Sena.DOS_ASES
        if (out.isEmpty()) out += Sena.CIEGO
        return out.take(2)
    }

    private fun senaConstraint(sena: Sena, reyes: Int): (List<Card>) -> Boolean {
        fun count(h: List<Card>, r: Int) = h.count { Cards.effRank(it, reyes) == r }
        return when (sena) {
            Sena.DOS_REYES -> { h -> count(h, 12) >= 2 }
            Sena.TRES_REYES -> { h -> count(h, 12) >= 3 }
            Sena.DOS_ASES -> { h -> count(h, 1) >= 2 }
            Sena.TRES_ASES -> { h -> count(h, 1) >= 3 }
            Sena.DUPLES -> { h -> Evaluate.paresOf(h, reyes).cat == 3 }
            Sena.MEDIAS -> { h -> Evaluate.paresOf(h, reyes).cat >= 2 }
            Sena.TREINTA_Y_UNA -> { h -> Evaluate.handPoints(h, reyes) == 31 }
            Sena.CIEGO -> { h -> !Evaluate.hasPares(h, reyes) && !Evaluate.hasJuego(h, reyes) }
        }
    }

    fun estimate(view: MusView, rng: Rng, samples: Int, useSenas: Boolean): Estimate {
        val reyes = view.config.reyes
        val me = view.seat
        val unknown = (0 until 40).filter { it !in view.hand }
        val decl = view.declarations
        val constraints = if (useSenas) {
            view.senas.filter { it.seat == partnerOf(me) }.map { senaConstraint(it.sena, reyes) }
        } else emptyList()
        val order = List(4) { (view.mano + it) % 4 }
        val wins = DoubleArray(5)
        val counted = DoubleArray(5)
        val hands = MutableList(4) { emptyList<Card>() }
        val pool = unknown.toIntArray()
        var accepted = 0
        var it = 0
        while (it < samples * 6 && accepted < samples) {
            for (i in 0 until 12) {
                val j = i + rng.nextInt(pool.size - i)
                val t = pool[i]; pool[i] = pool[j]; pool[j] = t
            }
            var k = 0
            for (s in order) {
                hands[s] = if (s == me) view.hand else listOf(pool[k], pool[k + 1], pool[k + 2], pool[k + 3]).also { k += 4 }
            }
            val relax = it > samples * 4
            it++
            if (!relax) {
                if (decl.pares != null && (0..3).any { s -> Evaluate.hasPares(hands[s], reyes) != decl.pares[s] }) continue
                if (decl.juego != null && (0..3).any { s -> Evaluate.hasJuego(hands[s], reyes) != decl.juego[s] }) continue
                if (constraints.any { f -> !f(hands[partnerOf(me)]) }) continue
            }
            accepted++
            val anyJuego = order.any { s -> Evaluate.hasJuego(hands[s], reyes) }
            for (lance in Lance.entries) {
                val seats = when (lance) {
                    Lance.PARES -> order.filter { s -> Evaluate.hasPares(hands[s], reyes) }
                    Lance.JUEGO -> order.filter { s -> Evaluate.hasJuego(hands[s], reyes) }
                    Lance.PUNTO -> if (anyJuego) emptyList() else order
                    else -> order
                }
                if (seats.isEmpty()) continue
                val best = Evaluate.bestSeat(hands, seats, lance, reyes)
                counted[lance.ordinal]++
                if (teamOf(best) == teamOf(me)) wins[lance.ordinal]++
            }
        }
        fun r(l: Lance) = if (counted[l.ordinal] > 0) wins[l.ordinal] / counted[l.ordinal] else 0.5
        return Estimate(r(Lance.GRANDE), r(Lance.CHICA), r(Lance.PARES), r(Lance.JUEGO), r(Lance.PUNTO))
    }

    private fun decideMus(view: MusView, est: Estimate, p: LevelParams, rng: Rng): MusAction {
        val reyes = view.config.reyes
        val pr = Evaluate.paresOf(view.hand, reyes)
        val pts = Evaluate.handPoints(view.hand, reyes)
        var strong = 0.0
        if (pr.cat >= 2) strong += 2 else if (pr.cat == 1 && pr.hi >= 10) strong += 1
        if (pts == 31) strong += 2 else if (pts >= 31) strong += 1
        if (est.grande > 0.75) strong += 1
        if (est.chica > 0.8) strong += 1
        if (pts == 30 || pts == 29) strong += 0.5
        var cut = strong >= 2
        if (p.scoreAware) {
            val diff = view.scores[teamOf(view.seat)] - view.scores[1 - teamOf(view.seat)]
            if (diff > 10 && strong >= 1.5) cut = true
        }
        if (rng.nextFloat() < p.noise) cut = !cut
        return if (cut) MusAction.NoMus else MusAction.Mus
    }

    fun chooseDiscard(hand: List<Card>, reyes: Int, rng: Rng, level: BotLevel): List<Card> {
        if (level == BotLevel.FACIL && rng.nextFloat() < 0.3) {
            val n = 1 + rng.nextInt(4)
            return hand.toMutableList().shuffleWith(rng).take(n)
        }
        val counts = hand.groupingBy { Cards.effRank(it, reyes) }.eachCount()
        val ases = hand.count { Cards.effRank(it, reyes) == 1 }
        val reyesN = hand.count { Cards.effRank(it, reyes) == 12 }
        val goChica = ases >= 2 && reyesN < 2
        fun keep(c: Card): Boolean {
            val r = Cards.effRank(c, reyes)
            if ((counts[r] ?: 0) >= 2) return true
            return if (goChica) r == 1 else r == 12
        }
        val discard = hand.filterNot(::keep)
        if (discard.isNotEmpty()) return discard
        val worst = hand.sortedWith(compareBy({ counts[Cards.effRank(it, reyes)] ?: 0 }, { Cards.effRank(it, reyes) })).first()
        return listOf(worst)
    }

    private fun decideBet(view: MusView, est: Estimate, p: LevelParams, rng: Rng): MusAction {
        val b = view.betting!!
        val myTeam = teamOf(view.seat)
        val myScore = view.scores[myTeam]
        val theirScore = view.scores[1 - myTeam]
        val target = view.config.target
        val pw = (est[b.lance] + (rng.nextFloat() * 2 - 1) * p.noise).coerceIn(0.0, 1.0)
        val desperate = p.scoreAware && theirScore >= target - 5 && myScore < target - 10

        if (b.ordago) {
            var accept = pw > 0.72
            if (p.scoreAware) {
                if (desperate && pw > 0.4) accept = true
                if (myScore >= target - 3 && pw < 0.8) accept = false
            }
            return if (accept) MusAction.Quiero else MusAction.NoQuiero
        }

        if (b.proposed == 0) {
            val late = maxOf(myScore, theirScore) >= target - 12
            if (p.scoreAware && ((late && pw > 0.95) || (desperate && pw > 0.62))) return MusAction.Ordago
            if (!p.scoreAware && pw > 0.98 && rng.nextFloat() < 0.15) return MusAction.Ordago
            if (pw > p.betThreshold) {
                val amount = if (pw > p.raiseThreshold && rng.nextFloat() < 0.4) 5 else 2
                return MusAction.Envido(amount)
            }
            if (rng.nextFloat() < p.bluff) return MusAction.Envido(2)
            return MusAction.Paso
        }

        val stake = b.proposed
        val need = p.acceptThreshold + minOf(0.25, (stake - 2) * 0.02)
        if (pw > p.raiseThreshold && stake < 15) {
            if (p.scoreAware && pw > 0.95 && stake >= 8) return MusAction.Ordago
            return MusAction.Envido(2)
        }
        if (pw > need) return MusAction.Quiero
        if (rng.nextFloat() < p.bluff / 2 && stake <= 4) return MusAction.Envido(2)
        return MusAction.NoQuiero
    }

    fun decide(view: MusView, level: BotLevel, rng: Rng): MusAction {
        val p = PARAMS.getValue(level)
        check(view.legal.isNotEmpty()) { "El bot no tiene acciones legales" }
        return when (view.phase) {
            Phase.FIN_MANO -> MusAction.SiguienteMano
            Phase.DESCARTE -> MusAction.Descarte(chooseDiscard(view.hand, view.config.reyes, rng, level))
            Phase.MUS -> decideMus(view, estimate(view, rng, minOf(p.samples, 120), false), p, rng)
            else -> decideBet(view, estimate(view, rng, p.samples, p.useSenas), p, rng)
        }
    }
}
