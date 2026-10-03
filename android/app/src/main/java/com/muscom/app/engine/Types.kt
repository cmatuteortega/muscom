package com.muscom.app.engine

/** Port de packages/mus-engine/src/mus/types.ts */
fun teamOf(seat: Int) = seat % 2
fun partnerOf(seat: Int) = (seat + 2) % 4

enum class Lance { GRANDE, CHICA, PARES, JUEGO, PUNTO }

enum class Phase {
    MUS, DESCARTE, GRANDE, CHICA, PARES, JUEGO, PUNTO, FIN_MANO, FIN_PARTIDA;

    val lance: Lance?
        get() = when (this) {
            GRANDE -> Lance.GRANDE
            CHICA -> Lance.CHICA
            PARES -> Lance.PARES
            JUEGO -> Lance.JUEGO
            PUNTO -> Lance.PUNTO
            else -> null
        }

    companion object {
        fun of(lance: Lance) = when (lance) {
            Lance.GRANDE -> GRANDE
            Lance.CHICA -> CHICA
            Lance.PARES -> PARES
            Lance.JUEGO -> JUEGO
            Lance.PUNTO -> PUNTO
        }
    }
}

enum class Sena(val code: String) {
    DOS_REYES("dosReyes"), TRES_REYES("tresReyes"), DOS_ASES("dosAses"), TRES_ASES("tresAses"),
    DUPLES("duples"), MEDIAS("medias"), TREINTA_Y_UNA("treintaYUna"), CIEGO("ciego");

    companion object {
        fun fromCode(c: String) = entries.first { it.code == c }
    }
}

sealed interface MusAction {
    data object Mus : MusAction
    data object NoMus : MusAction
    data class Descarte(val cards: List<Card>) : MusAction
    data object Paso : MusAction
    /** Sin apuesta previa: tantos envidados. Respondiendo: tantos que se suben. */
    data class Envido(val amount: Int) : MusAction
    data object Quiero : MusAction
    data object NoQuiero : MusAction
    data object Ordago : MusAction
    data class SenaAction(val sena: Sena) : MusAction
    data object SiguienteMano : MusAction

    fun encode(): String = when (this) {
        Mus -> "mus"
        NoMus -> "noMus"
        is Descarte -> "descarte:" + cards.joinToString(",")
        Paso -> "paso"
        is Envido -> "envido:$amount"
        Quiero -> "quiero"
        NoQuiero -> "noQuiero"
        Ordago -> "ordago"
        is SenaAction -> "sena:" + sena.code
        SiguienteMano -> "siguienteMano"
    }

    companion object {
        fun decode(s: String): MusAction {
            val parts = s.split(":", limit = 2)
            return when (parts[0]) {
                "mus" -> Mus
                "noMus" -> NoMus
                "descarte" -> Descarte(parts[1].split(",").map { it.toInt() })
                "paso" -> Paso
                "envido" -> Envido(parts[1].toInt())
                "quiero" -> Quiero
                "noQuiero" -> NoQuiero
                "ordago" -> Ordago
                "sena" -> SenaAction(Sena.fromCode(parts[1]))
                "siguienteMano" -> SiguienteMano
                else -> error("acción desconocida $s")
            }
        }
    }
}

data class MusConfig(
    val reyes: Int = 8,
    val target: Int = 40,
    val maxSenasPerHand: Int = 3,
    val firstDealer: Int = 3,
)

data class Betting(
    val lance: Lance,
    val participants: List<Int>,
    val queue: List<Int>,
    val proposed: Int,
    val deje: Int,
    val bettingTeam: Int?,
    val ordago: Boolean,
)

enum class LanceOutcome { PASO, QUERIDO, NO_QUERIDO, SIN_APUESTA, NO_HAY }

data class LanceRecord(
    val lance: Lance,
    val outcome: LanceOutcome,
    val amount: Int = 0,
    val team: Int? = null,
    val ordago: Boolean = false,
)

enum class AwardKind { DEJE, APUESTA, PASO, JUGADA, ORDAGO }

data class Award(val lance: Lance, val team: Int, val points: Int, val kind: AwardKind)

data class HandSummary(
    val hands: List<List<Card>>,
    val winners: Map<Lance, Int>,
    val awards: List<Award>,
    val gameOver: Boolean,
)

data class LogEntry(val hand: Int, val phase: Phase, val seat: Int, val action: MusAction)
data class SenaRecord(val seat: Int, val sena: Sena)

data class Declarations(val pares: List<Boolean>?, val juego: List<Boolean>?)

/** Vista redactada para un asiento: nunca contiene cartas ajenas antes del final de la mano. */
data class MusView(
    val seat: Int,
    val phase: Phase,
    val turn: Int?,
    val dealer: Int,
    val mano: Int,
    val handNumber: Int,
    val scores: List<Int>,
    val config: MusConfig,
    val hand: List<Card>,
    val musRound: Int,
    val pendingDiscard: List<Int>,
    val lastDiscardCounts: List<Int>,
    val betting: Betting?,
    val lances: List<LanceRecord>,
    val awards: List<Award>,
    val declarations: Declarations,
    val senas: List<SenaRecord>,
    val log: List<LogEntry>,
    val revealed: List<List<Card>>?,
    val summary: HandSummary?,
    val winner: Int?,
    val legal: List<MusAction>,
)

class IllegalActionException(msg: String) : IllegalStateException(msg)
