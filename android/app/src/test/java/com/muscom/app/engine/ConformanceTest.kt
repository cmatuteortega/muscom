package com.muscom.app.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.math.sign

/**
 * Verifica que el port Kotlin se comporta igual que el motor TypeScript usando
 * packages/mus-engine/conformance/vectors.json (generado con `npm run conformance`).
 */
class ConformanceTest {
    private val vectors: JsonObject by lazy {
        val path = System.getProperty("conformance.vectors") ?: "../../packages/mus-engine/conformance/vectors.json"
        Json.parseToJsonElement(File(path).readText()).jsonObject
    }

    @Test
    fun rngMatchesTypeScript() {
        for (v in vectors["rng"]!!.jsonArray) {
            val o = v.jsonObject
            val rng = Rng.fromSeed(o["seed"]!!.jsonPrimitive.content)
            val expected = o["ints"]!!.jsonArray.map { it.jsonPrimitive.int }
            assertEquals(expected, List(expected.size) { rng.nextInt(1000) })
        }
    }

    @Test
    fun evaluationMatchesTypeScript() {
        for (v in vectors["eval"]!!.jsonArray) {
            val o = v.jsonObject
            val a = o["a"]!!.jsonArray.map { it.jsonPrimitive.int }
            val b = o["b"]!!.jsonArray.map { it.jsonPrimitive.int }
            val reyes = o["reyes"]!!.jsonPrimitive.int
            val ctx = "a=$a b=$b reyes=$reyes"
            assertEquals(ctx, o["grande"]!!.jsonPrimitive.int, Evaluate.compareGrande(a, b, reyes).sign)
            assertEquals(ctx, o["chica"]!!.jsonPrimitive.int, Evaluate.compareChica(a, b, reyes).sign)
            assertEquals(ctx, o["pares"]!!.jsonPrimitive.int, Evaluate.comparePares(a, b, reyes).sign)
            assertEquals(ctx, o["juego"]!!.jsonPrimitive.int, Evaluate.compareJuego(a, b, reyes).sign)
            assertEquals(ctx, o["punto"]!!.jsonPrimitive.int, Evaluate.comparePunto(a, b, reyes).sign)
            assertEquals(ctx, o["puntosA"]!!.jsonPrimitive.int, Evaluate.handPoints(a, reyes))
            val p = o["paresA"]!!.jsonObject
            assertEquals(
                ctx,
                Pares(p["cat"]!!.jsonPrimitive.int, p["hi"]!!.jsonPrimitive.int, p["lo"]!!.jsonPrimitive.int),
                Evaluate.paresOf(a, reyes),
            )
        }
    }

    @Test
    fun fullGamesMatchTypeScript() {
        val games = vectors["games"]!!.jsonArray
        check(games.size > 10)
        for (g in games) {
            val o = g.jsonObject
            val seed = o["seed"]!!.jsonPrimitive.content
            val noOrdago = o["noOrdago"]!!.jsonPrimitive.boolean
            val policy = Rng.fromSeed(o["policySeed"]!!.jsonPrimitive.content)
            val game = MusGame(
                MusConfig(reyes = o["reyes"]!!.jsonPrimitive.int, firstDealer = o["firstDealer"]!!.jsonPrimitive.int),
                seed,
            )
            val expectedActions = o["actions"]!!.jsonArray.map { it.jsonPrimitive.content }
            val handScores = mutableListOf<List<Int>>()
            for ((step, expected) in expectedActions.withIndex()) {
                val seat = game.actors().first()
                var legal = game.legalActions(seat)
                if (noOrdago) legal = legal.filter { it != MusAction.Ordago }
                val a = legal[policy.nextInt(legal.size)]
                assertEquals("$seed paso $step", expected, "$seat|${a.encode()}")
                game.apply(seat, a)
                if (game.phase == Phase.FIN_MANO || game.phase == Phase.FIN_PARTIDA) handScores += game.scores.toList()
            }
            val expectedHandScores = o["handScores"]!!.jsonArray.map { r -> r.jsonArray.map { it.jsonPrimitive.int } }
            assertEquals(seed, expectedHandScores, handScores)
            assertEquals(seed, o["finalScores"]!!.jsonArray.map { it.jsonPrimitive.int }, game.scores.toList())
            assertEquals(seed, o["winner"]!!.jsonPrimitive.intOrNull, game.winner)
            assertEquals(seed, o["hands"]!!.jsonPrimitive.int, game.handNumber)
        }
    }
}
