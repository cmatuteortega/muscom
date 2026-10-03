package com.muscom.app.engine

/** Port de packages/mus-engine/src/mus/evaluate.ts */
data class Pares(val cat: Int, val hi: Int, val lo: Int)

object Evaluate {
    fun grandeKey(hand: List<Card>, reyes: Int) = hand.map { Cards.effRank(it, reyes) }.sortedDescending()
    fun chicaKey(hand: List<Card>, reyes: Int) = hand.map { Cards.effRank(it, reyes) }.sorted()

    fun compareGrande(a: List<Card>, b: List<Card>, reyes: Int): Int {
        val ka = grandeKey(a, reyes)
        val kb = grandeKey(b, reyes)
        for (i in ka.indices) if (ka[i] != kb[i]) return ka[i] - kb[i]
        return 0
    }

    fun compareChica(a: List<Card>, b: List<Card>, reyes: Int): Int {
        val ka = chicaKey(a, reyes)
        val kb = chicaKey(b, reyes)
        for (i in ka.indices) if (ka[i] != kb[i]) return kb[i] - ka[i]
        return 0
    }

    fun paresOf(hand: List<Card>, reyes: Int): Pares {
        val counts = hand.groupingBy { Cards.effRank(it, reyes) }.eachCount()
        val groups = counts.entries.filter { it.value >= 2 }.sortedByDescending { it.key }
        if (groups.isEmpty()) return Pares(0, 0, 0)
        if (groups.size == 2) return Pares(3, groups[0].key, groups[1].key)
        val (r, n) = groups[0]
        return when (n) {
            4 -> Pares(3, r, r)
            3 -> Pares(2, r, 0)
            else -> Pares(1, r, 0)
        }
    }

    fun hasPares(hand: List<Card>, reyes: Int) = paresOf(hand, reyes).cat > 0
    fun paresPoints(p: Pares) = p.cat

    fun compareParesValue(a: Pares, b: Pares): Int = when {
        a.cat != b.cat -> a.cat - b.cat
        a.hi != b.hi -> a.hi - b.hi
        else -> a.lo - b.lo
    }

    fun comparePares(a: List<Card>, b: List<Card>, reyes: Int) =
        compareParesValue(paresOf(a, reyes), paresOf(b, reyes))

    fun handPoints(hand: List<Card>, reyes: Int) = hand.sumOf { Cards.pointValue(it, reyes) }
    fun hasJuego(hand: List<Card>, reyes: Int) = handPoints(hand, reyes) >= 31

    val JUEGO_ORDER = intArrayOf(31, 32, 40, 37, 36, 35, 34, 33)
    fun juegoRank(points: Int): Int = JUEGO_ORDER.indexOf(points).let { if (it < 0) 99 else it }
    fun juegoPoints(points: Int) = if (points == 31) 3 else 2

    fun compareJuego(a: List<Card>, b: List<Card>, reyes: Int) =
        juegoRank(handPoints(b, reyes)) - juegoRank(handPoints(a, reyes))

    fun comparePunto(a: List<Card>, b: List<Card>, reyes: Int) =
        handPoints(a, reyes) - handPoints(b, reyes)

    fun comparator(lance: Lance): (List<Card>, List<Card>, Int) -> Int = when (lance) {
        Lance.GRANDE -> ::compareGrande
        Lance.CHICA -> ::compareChica
        Lance.PARES -> ::comparePares
        Lance.JUEGO -> ::compareJuego
        Lance.PUNTO -> ::comparePunto
    }

    /** Mejor asiento; en empate gana el más cercano a la mano (primero de la lista). */
    fun bestSeat(hands: List<List<Card>>, seatsInManoOrder: List<Int>, lance: Lance, reyes: Int): Int {
        val cmp = comparator(lance)
        var best = seatsInManoOrder[0]
        for (i in 1 until seatsInManoOrder.size) {
            val s = seatsInManoOrder[i]
            if (cmp(hands[s], hands[best], reyes) > 0) best = s
        }
        return best
    }
}
