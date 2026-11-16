package com.estudo.pokerassistant

/** Posicoes (modelo de 8 lugares; em mesa de 9, trate UTG+2 como MP). O jogador escolhe a sua pelo botao "Pos". */
object Pos {
    val NAMES = listOf("UTG", "UTG+1", "MP", "HJ", "CO", "BTN", "SB", "BB")
    const val UTG = 0
    const val UTG1 = 1
    const val MP = 2
    const val HJ = 3
    const val CO = 4
    const val BTN = 5
    const val SB = 6
    const val BB = 7

    /** Rotulos do botao "Abridor" (0 = automatico; k = posicao k-1). */
    val OPENER_LABELS = listOf("Auto", "UTG", "UTG+1", "MP", "HJ", "CO", "BTN", "SB")

    fun inPosition(p: Int) = p == Pos.CO || p == Pos.BTN

    /** Abridor presumido quando o botao esta em "Auto": a posicao logo antes da sua. */
    fun autoOpener(hero: Int) = when (hero) {
        UTG, UTG1 -> UTG
        MP -> UTG1
        HJ -> MP
        CO -> HJ
        BTN -> CO
        SB -> BTN
        else -> CO // BB
    }
}

private fun comboCount(cls: String) = if (cls.length == 2) 6 else if (cls[2] == 's') 4 else 12

/**
 * Banco de ranges MTT.
 *
 * Ideia: as 169 maos ficam ordenadas por forca (duas ordens: DEEP para abrir/pagar e PUSH para all-ins). Cada range e um percentual
 * do topo dessa ordem, vindo de tabelas por POSICAO x FAIXA DE STACK. Isso permite
 * ajustar o range por fase do torneio/perfil apenas escalando o percentual, e
 * guardar tudo em poucos numeros em vez de centenas de strings.
 *
 * Faixas de stack (BB): 0 = 80-100+, 1 = 50-79, 2 = 30-49, 3 = 20-29, 4 = ~15, 5 = ~10.
 */
object Ranges {
    const val RANKS = "23456789TJQKA"
    val BUCKETS = listOf("80-100+", "50-79", "30-49", "20-29", "15", "10")

    /** Ordem de forca para PUSH/FOLD e all-ins (equity contra mao aleatoria / Nash). */
    private val PUSH: List<String> = (
        "AA KK QQ AKs JJ AQs KQs AJs KJs AKo TT AQo ATs QJs KTs QTs JTs 99 AJo KQo A9s 88 K9s T9s A8s Q9s J9s KJo " +
        "A5s 77 A7s A4s A3s A6s QJo ATo A2s 66 K8s 98s T8s K7s J8s KTo 87s Q8s 55 K6s K5s 97s A9o QTo JTo 76s K4s " +
        "44 K3s K2s Q7s 86s 65s J7s T7s 54s Q6s 33 A8o 75s 96s Q5s 22 Q4s J6s 64s Q3s K9o T9o 85s Q2s J5s 53s T6s " +
        "A7o 95s J9o 43s J4s Q9o 74s J3s A5o K8o J2s 84s 63s A6o T5s A4o 94s 52s T4s 93s T3s A3o Q8o K7o 98o 42s " +
        "T2s A2o 73s 92s J8o K6o 87o 83s T8o 62s 82s 32s Q7o K5o 76o 97o 72s J7o K4o 65o 86o K3o Q6o T7o 54o K2o " +
        "75o Q5o J6o 96o 64o 85o Q4o T6o Q3o J5o 53o Q2o 95o 43o J4o 74o T5o J3o 84o 63o J2o T4o 94o 52o T3o 93o " +
        "42o T2o 73o 92o 83o 62o 82o 32o 72o"
        ).split(" ")

    /** Ordem para stacks fundos (abrir/pagar): valoriza suited, conectadas e pares pequenos. */
    private val DEEP: List<String> = run {
        val prefix = (
        "AA KK QQ AKs JJ AKo AQs TT AQo KQs AJs 99 ATs KJs QJs AJo 88 KQo JTs KTs QTs A5s A4s 77 T9s 66 " +
        "A9s 98s A3s ATo KJo A8s A2s QJo K9s J9s Q9s 87s A7s KTo A6s 55 QTo JTo T8s K8s 76s A9o J8s 97s " +
        "65s 44 K7s 54s Q8s 86s A8o K6s K5s 33 75s T9o J7s K4s 64s Q7s 22 K3s 53s A7o K2s 96s Q6s 85s " +
        "T7s K9o A5o 43s 74s Q5s J6s A6o A4o Q9o J9o 63s A3o Q4s T6s A2o Q3s 52s J5s 95s K8o"
        ).split(" ")
        prefix + PUSH.filter { it !in prefix }
    }

    // ---------------------------------------------------------------- utilidades
    private class Ord(val list: List<String>) {
        /** Percentual acumulado (0..100) ao fim de cada mao. */
        val cum: DoubleArray = run {
            var c = 0.0
            DoubleArray(list.size) { c += comboCount(list[it]) * 100.0 / 1326.0; c }
        }
        fun slice(lo: Double, hi: Double): List<String> {
            val out = ArrayList<String>()
            for (i in list.indices) {
                val prev = if (i == 0) 0.0 else cum[i - 1]
                val mid = prev + (cum[i] - prev) / 2
                if (mid > lo && mid <= hi) out.add(list[i])
            }
            return out
        }
    }

    private val ORD_PUSH = Ord(PUSH)
    private val ORD_DEEP = Ord(DEEP)

    /** Maos entre lo% e hi% da ordem. push=true usa a ordem de push/fold. */
    fun slice(lo: Double, hi: Double, push: Boolean = false): List<String> =
        (if (push) ORD_PUSH else ORD_DEEP).slice(lo, hi)

    fun top(pct: Double, push: Boolean = false): List<String> = slice(-1.0, pct.coerceIn(0.0, 100.0), push)

    fun className(c1: Int, c2: Int): String {
        var r1 = c1 shr 2
        var r2 = c2 shr 2
        val suited = (c1 and 3) == (c2 and 3)
        if (r1 < r2) { val t = r1; r1 = r2; r2 = t }
        return if (r1 == r2) "${RANKS[r1]}${RANKS[r2]}" else "${RANKS[r1]}${RANKS[r2]}${if (suited) "s" else "o"}"
    }

    private val comboCache = HashMap<String, List<IntArray>>()
    fun combos(cls: String): List<IntArray> = comboCache.getOrPut(cls) {
        val r1 = RANKS.indexOf(cls[0])
        val r2 = RANKS.indexOf(cls[1])
        val out = ArrayList<IntArray>()
        if (cls.length == 2) {
            for (a in 0..3) for (b in a + 1..3) out.add(intArrayOf(r1 * 4 + a, r1 * 4 + b))
        } else if (cls[2] == 's') {
            for (s in 0..3) out.add(intArrayOf(r1 * 4 + s, r2 * 4 + s))
        } else {
            for (a in 0..3) for (b in 0..3) if (a != b) out.add(intArrayOf(r1 * 4 + a, r2 * 4 + b))
        }
        out
    }

    fun pool(classes: Collection<String>): List<IntArray> = classes.flatMap { combos(it) }

    fun bucket(stackBb: Double) = when {
        stackBb >= 80 -> 0
        stackBb >= 50 -> 1
        stackBb >= 30 -> 2
        stackBb >= 19.5 -> 3
        stackBb >= 12.5 -> 4
        else -> 5
    }

    /** Texto compacto do range, ex.: "Pares: 22+ | Suited: A2s+, KTs+ | Offsuit: ATo+". */
    fun describe(set: Collection<String>): String {
        if (set.isEmpty()) return "-"
        val parts = ArrayList<String>()
        val pairs = (0..12).filter { "${RANKS[it]}${RANKS[it]}" in set }
        if (pairs.isNotEmpty()) {
            parts.add("Pares: " + segments(pairs).joinToString(", ") { sg ->
                when {
                    sg.last == 12 -> "${RANKS[sg.first]}${RANKS[sg.first]}+"
                    sg.first == sg.last -> "${RANKS[sg.first]}${RANKS[sg.first]}"
                    else -> "${RANKS[sg.last]}${RANKS[sg.last]}-${RANKS[sg.first]}${RANKS[sg.first]}"
                }
            })
        }
        for ((suf, label) in listOf("s" to "Suited", "o" to "Offsuit")) {
            val items = ArrayList<String>()
            for (h in 12 downTo 1) {
                val lows = (0 until h).filter { "${RANKS[h]}${RANKS[it]}$suf" in set }
                if (lows.isEmpty()) continue
                for (sg in segments(lows)) {
                    val top = "${RANKS[h]}${RANKS[sg.last]}$suf"
                    val bot = "${RANKS[h]}${RANKS[sg.first]}$suf"
                    items.add(
                        when {
                            sg.first == sg.last -> bot
                            sg.last == h - 1 -> "$bot+"
                            else -> "$top-$bot"
                        }
                    )
                }
            }
            if (items.isNotEmpty()) parts.add("$label: " + items.joinToString(", "))
        }
        return parts.joinToString(" | ")
    }

    private fun segments(v: List<Int>): List<IntRange> {
        val out = ArrayList<IntRange>()
        var s = v[0]
        var p = v[0]
        for (i in 1 until v.size) {
            if (v[i] == p + 1) p = v[i] else { out.add(s..p); s = v[i]; p = v[i] }
        }
        out.add(s..p)
        return out
    }

    // ---------------------------------------------------------------- 1) ABERTURA (RFI)
    // Percentual de maos por posicao (linhas: UTG, UTG+1, MP, HJ, CO, BTN, SB)
    // x faixa de stack (colunas: 80-100, 50-79, 30-49, 20-29, 15, 10).
    // Em 15 e 10 BB a "abertura" e SHOVE (push/fold).
    private val RFI = arrayOf(
        doubleArrayOf(13.0, 13.0, 13.0, 12.0, 11.0, 20.0), // UTG
        doubleArrayOf(14.5, 14.5, 14.5, 13.5, 12.0, 22.0), // UTG+1
        doubleArrayOf(17.0, 17.0, 17.0, 16.0, 14.0, 25.0), // MP
        doubleArrayOf(21.0, 21.0, 21.0, 20.0, 18.0, 29.0), // HJ
        doubleArrayOf(28.0, 28.0, 27.0, 25.0, 23.0, 35.0), // CO
        doubleArrayOf(46.0, 45.0, 42.0, 38.0, 34.0, 48.0), // BTN
        doubleArrayOf(42.0, 42.0, 40.0, 38.0, 40.0, 58.0)  // SB (vs BB)
    )

    fun rfi(pos: Int, b: Int): Double = RFI[pos.coerceIn(0, 6)][b.coerceIn(0, 5)]

    // ---------------------------------------------------------------- 2) ENFRENTANDO UM OPEN
    // Linhas: heroi (UTG+1, MP, HJ, CO, BTN, SB, BB). Colunas: abridor (UTG..SB).
    private val THREEBET = arrayOf(
        doubleArrayOf(3.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(3.5, 3.8, 0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(3.8, 4.0, 4.3, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(4.0, 4.3, 4.6, 5.2, 0.0, 0.0, 0.0),
        doubleArrayOf(4.5, 4.8, 5.2, 6.0, 7.5, 0.0, 0.0),
        doubleArrayOf(5.0, 5.5, 6.0, 7.0, 9.0, 11.0, 0.0),
        doubleArrayOf(5.5, 6.0, 6.5, 7.5, 10.0, 12.5, 12.0)
    )
    private val FLAT = arrayOf(
        doubleArrayOf(3.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(4.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(4.5, 4.5, 5.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(5.5, 5.5, 6.0, 6.5, 0.0, 0.0, 0.0),
        doubleArrayOf(8.0, 8.5, 9.0, 10.0, 12.0, 0.0, 0.0),
        doubleArrayOf(3.0, 3.5, 4.0, 5.0, 6.0, 8.0, 0.0),
        doubleArrayOf(20.0, 21.0, 23.0, 27.0, 32.0, 42.0, 38.0)
    )
    private val MULT_3BET = doubleArrayOf(1.0, 1.0, 0.85, 0.0, 0.0, 0.0)
    private val MULT_FLAT = doubleArrayOf(1.0, 0.9, 0.7, 0.45, 0.0, 0.0)

    // Reshove (all-in sobre um open) para 20-29, 15 e 10 BB. Colunas: abridor UTG..SB.
    private val RESHOVE = arrayOf(
        doubleArrayOf(5.0, 5.5, 6.0, 7.0, 8.0, 10.0, 11.0),    // 20-29
        doubleArrayOf(7.0, 8.0, 9.0, 11.0, 14.0, 18.0, 20.0),  // ~15
        doubleArrayOf(11.0, 12.0, 14.0, 17.0, 22.0, 30.0, 32.0) // ~10
    )

    private val BLUFFS = listOf("A5s", "A4s", "A3s", "A2s", "K9s", "T9s", "98s", "87s", "76s", "65s")

    /** Devolve (3-bet/shove, call) do heroi contra um open. */
    fun vsOpen(hero: Int, opener: Int, b: Int, tight3: Double, tightCall: Double): Pair<Set<String>, Set<String>> {
        val h = hero.coerceIn(1, 7)
        val o = opener.coerceIn(0, h - 1)
        if (b >= 3) {
            var p = RESHOVE[b - 3][o]
            if (h == Pos.BB) p *= 1.3
            val jam = top(p * tight3, true).toSet()
            var callPct = FLAT[h - 1][o] * MULT_FLAT[b] * tightCall
            if (h == Pos.BB && b == 4) callPct = FLAT[h - 1][o] * 0.25 * tightCall
            val call = if (callPct <= 0.0) emptySet<String>()
            else top(p * tight3 + callPct, true).filter { it !in jam }.toSet()
            return Pair(jam, call)
        }
        val three = THREEBET[h - 1][o] * MULT_3BET[b] * tight3
        val valuePct = three * 0.62
        val threeSet = LinkedHashSet(top(valuePct))
        if (three >= 3.4) threeSet.add("AKo")
        val bluffPct = three * 0.38
        var acc = 0.0
        for (c in BLUFFS) {
            if (acc >= bluffPct) break
            if (c !in threeSet) { threeSet.add(c); acc += comboCount(c) * 100.0 / 1326.0 }
        }
        val callPct = FLAT[h - 1][o] * MULT_FLAT[b] * tightCall
        val calls = LinkedHashSet<String>()
        calls.addAll(top(valuePct + callPct).filter { it !in threeSet })
        if (h != Pos.SB) {
            if (b <= 1) {
                for (p in listOf("22", "33", "44", "55", "66", "77", "88", "99")) if (p !in threeSet) calls.add(p)
                if (Pos.inPosition(h) && o <= Pos.MP) {
                    for (p in listOf("JTs", "T9s", "98s", "87s", "76s")) if (p !in threeSet) calls.add(p)
                }
            } else if (b == 2) {
                for (p in listOf("44", "55", "66", "77", "88", "99")) if (p !in threeSet) calls.add(p)
            }
        }
        return Pair(threeSet, calls)
    }

    // ---------------------------------------------------------------- 3) ENFRENTANDO 3-BET (heroi abriu)
    /** Devolve (4-bet/shove, call) e o range de abertura do heroi. */
    fun vs3bet(pos: Int, b: Int, openM: Double, tight: Double): Triple<Set<String>, Set<String>, Set<String>> {
        val open = top(rfi(pos, b) * openM, b >= 4).toSet()
        if (b >= 3) {
            val p = doubleArrayOf(11.0, 15.0, 22.0)[b - 3] * tight
            return Triple(top(p, true).toSet(), emptySet(), open)
        }
        val value = LinkedHashSet(top(if (b == 2) 5.0 * tight else 3.2 * tight))
        value.add("AKo")
        if (b <= 1) for (c in listOf("A5s", "A4s")) if (c in open) value.add(c)
        val frac = if (Pos.inPosition(pos)) 0.42 else 0.28
        val calls = top(rfi(pos, b) * openM * frac * tight).filter { it !in value && it in open }.toSet()
        return Triple(value, calls, open)
    }

    // ---------------------------------------------------------------- 4) ALL-IN do vilao
    /** Percentual do range de quem da all-in, pelo stack efetivo em BB. */
    fun shoveRangePct(stackBb: Double) = when {
        stackBb <= 8 -> 38.0
        stackBb <= 12 -> 28.0
        stackBb <= 16 -> 21.0
        stackBb <= 25 -> 14.0
        stackBb <= 40 -> 9.0
        else -> 6.0
    }
}
