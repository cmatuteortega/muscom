package com.muscom.app.engine

/**
 * Port exacto de packages/mus-engine/src/core/rng.ts (cyrb128 + sfc32).
 * Debe producir la misma secuencia que la versión TypeScript: lo comprueban los
 * tests de conformidad con conformance/vectors.json.
 */
object Cyrb128 {
    fun hash(str: String): IntArray {
        var h1 = 1779033703
        var h2 = 3144134277L.toInt()
        var h3 = 1013904242
        var h4 = 2773480762L.toInt()
        val m1 = 597399067
        val m2 = 2869860233L.toInt()
        val m3 = 951274213
        val m4 = 2716044179L.toInt()
        for (ch in str) {
            val k = ch.code
            h1 = h2 xor ((h1 xor k) * m1)
            h2 = h3 xor ((h2 xor k) * m2)
            h3 = h4 xor ((h3 xor k) * m3)
            h4 = h1 xor ((h4 xor k) * m4)
        }
        h1 = (h3 xor (h1 ushr 18)) * m1
        h2 = (h4 xor (h2 ushr 22)) * m2
        h3 = (h1 xor (h3 ushr 17)) * m3
        h4 = (h2 xor (h4 ushr 19)) * m4
        h1 = h1 xor (h2 xor h3 xor h4)
        h2 = h2 xor h1
        h3 = h3 xor h1
        h4 = h4 xor h1
        return intArrayOf(h1, h2, h3, h4)
    }
}

class Rng private constructor(private val s: IntArray) {

    fun nextUint32(): Long {
        var a = s[0]
        var b = s[1]
        var c = s[2]
        var d = s[3]
        var t = a + b
        a = b xor (b ushr 9)
        b = c + (c shl 3)
        c = (c shl 21) or (c ushr 11)
        d += 1
        t += d
        c += t
        s[0] = a; s[1] = b; s[2] = c; s[3] = d
        return t.toLong() and 0xffffffffL
    }

    /** Entero uniforme en [0, n) sin sesgo. */
    fun nextInt(n: Int): Int {
        require(n > 0) { "rango inválido $n" }
        val limit = TWO_32 - (TWO_32 % n)
        while (true) {
            val x = nextUint32()
            if (x < limit) return (x % n).toInt()
        }
    }

    fun nextFloat(): Double = nextUint32().toDouble() / TWO_32.toDouble()

    fun copy(): Rng = Rng(s.copyOf())

    companion object {
        private const val TWO_32 = 0x100000000L

        fun fromSeed(seed: String): Rng {
            val r = Rng(Cyrb128.hash(seed))
            repeat(15) { r.nextUint32() }
            return r
        }
    }
}

fun <T> MutableList<T>.shuffleWith(rng: Rng): MutableList<T> {
    for (i in size - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
    return this
}
