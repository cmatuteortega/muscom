package com.muscom.app.engine

/**
 * Carta = entero 0..39. palo = c / 10 (0 oros, 1 copas, 2 espadas, 3 bastos);
 * rango = RANKS[c % 10]. Idéntico a packages/mus-engine/src/mus/cards.ts.
 */
typealias Card = Int

enum class Suit { OROS, COPAS, ESPADAS, BASTOS }

object Cards {
    val RANKS = intArrayOf(1, 2, 3, 4, 5, 6, 7, 10, 11, 12)
    private const val SUIT_LETTERS = "OCEB"

    fun suitOf(c: Card): Suit = Suit.entries[c / 10]
    fun rankOf(c: Card): Int = RANKS[c % 10]
    fun make(rank: Int, suit: Suit): Card {
        val idx = RANKS.indexOf(rank)
        require(idx >= 0) { "Rango inválido: $rank" }
        return suit.ordinal * 10 + idx
    }

    fun fullDeck(): MutableList<Card> = MutableList(40) { it }

    fun effRank(c: Card, reyes: Int): Int {
        val r = rankOf(c)
        if (reyes == 8) {
            if (r == 3) return 12
            if (r == 2) return 1
        }
        return r
    }

    fun pointValue(c: Card, reyes: Int): Int {
        val r = effRank(c, reyes)
        return if (r >= 10) 10 else r
    }

    fun toShort(c: Card): String = "${rankOf(c)}${SUIT_LETTERS[c / 10]}"

    fun parse(s: String): Card {
        val t = s.trim().uppercase()
        val suit = SUIT_LETTERS.indexOf(t.last())
        require(suit >= 0) { "Carta inválida: $s" }
        return make(t.dropLast(1).toInt(), Suit.entries[suit])
    }

    fun parseHand(s: String): List<Card> = s.trim().split(Regex("\\s+")).map(::parse)
}
