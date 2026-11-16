package com.estudo.pokerassistant

import kotlin.random.Random

/** Resultado de uma sugestao: acao, equity, explicacao e (pre-flop) o range usado. */
data class Advice(val action: String, val eq: Double, val why: String, val range: String = "")

/** Caracteristicas da mao do heroi (ou de uma mao do vilao) numa mesa com board. */
class Feat(
    val level: Int,        // 0 nada .. 6 full house+
    val fd: Boolean, val nutFd: Boolean,
    val oesd: Boolean, val gut: Boolean,
    val outs: Int, val over: Int, val blocker: Boolean
)

class Texture(val wet: Int, val label: String)

/** Maos possiveis do vilao com pesos (probabilidade de ele ter essa mao dada a acao). */
class Pool(val hands: List<IntArray>, private val cum: DoubleArray) {
    val total: Double = if (cum.isEmpty()) 0.0 else cum[cum.size - 1]
    fun pick(r: Random): IntArray {
        val x = r.nextDouble() * total
        var lo = 0
        var hi = cum.size - 1
        while (lo < hi) {
            val m = (lo + hi) ushr 1
            if (cum[m] < x) lo = m + 1 else hi = m
        }
        return hands[lo]
    }
}

object Engine {
    const val RANKS = "23456789TJQKA"
    const val SUITS = "shdc"
    val PHASES = listOf("Inicio", "Meio", "Pre-bolha", "Bolha", "Premiacao", "Mesa final")
    val PROFILES = listOf("Normal", "Curto", "Grande")
    val FOLD_WORDS = listOf("fold", "desist", "correu", "correr")
    val CHECK_WORDS = listOf("check", "mesa", "passou", "passar")

    private val ICM = doubleArrayOf(0.0, 0.01, 0.035, 0.07, 0.03, 0.06)
    private val OPEN_MULT = doubleArrayOf(1.0, 1.0, 0.95, 0.90, 1.0, 0.92)
    private val LEVELS = listOf("nada", "carta alta", "par fraco", "par forte", "2 pares/trinca", "sequencia/flush", "full house+")

    /** Premio de risco (equity extra exigida) pela fase do torneio e pelo perfil de stack. */
    fun icmPrem(phase: Int, profile: Int): Double =
        ICM[phase.coerceIn(0, 5)] * doubleArrayOf(1.0, 1.35, 0.5)[profile.coerceIn(0, 2)]

    fun openMult(phase: Int, profile: Int, ante: Boolean = false): Double =
        OPEN_MULT[phase.coerceIn(0, 5)] * (if (profile == 2) 1.15 else 1.0) * (if (ante) 1.12 else 1.0)

    fun id(name: String): Int? {
        if (name.length != 2) return null
        val r = RANKS.indexOf(name[0]); val s = SUITS.indexOf(name[1])
        return if (r < 0 || s < 0) null else r * 4 + s
    }

    // ------------------------------------------------------------------ avaliador
    private fun straightHigh(mask: Int): Int {
        val m2 = (mask shl 1) or (if ((mask and (1 shl 12)) != 0) 1 else 0)
        for (h in 13 downTo 4) if (((m2 shr (h - 4)) and 31) == 31) return h - 1
        return -1
    }

    private fun pack(cat: Int, r: List<Int>): Long {
        var v = cat.toLong()
        for (i in 0 until 5) v = v * 16 + r.getOrElse(i) { 0 }
        return v
    }

    fun score(cards: IntArray, n: Int): Long {
        val rc = IntArray(13); val sc = IntArray(4); val sm = IntArray(4); var all = 0
        for (i in 0 until n) {
            val x = cards[i]; val r = x shr 2; val s = x and 3
            rc[r]++; sc[s]++; sm[s] = sm[s] or (1 shl r); all = all or (1 shl r)
        }
        var fm = 0
        for (s in 0..3) if (sc[s] >= 5) fm = sm[s]
        if (fm != 0) { val sh = straightHigh(fm); if (sh >= 0) return pack(8, listOf(sh)) }
        val groups = (12 downTo 0).filter { rc[it] > 0 }.sortedByDescending { rc[it] }
        fun kick(ex: List<Int>, k: Int) = (12 downTo 0).filter { rc[it] > 0 && it !in ex }.take(k)
        val top = groups[0]; val c0 = rc[top]
        if (c0 == 4) return pack(7, listOf(top) + kick(listOf(top), 1))
        if (c0 == 3 && groups.size > 1 && rc[groups[1]] >= 2) return pack(6, listOf(top, groups[1]))
        if (fm != 0) return pack(5, (12 downTo 0).filter { ((fm shr it) and 1) == 1 }.take(5))
        val sh = straightHigh(all)
        if (sh >= 0) return pack(4, listOf(sh))
        if (c0 == 3) return pack(3, listOf(top) + kick(listOf(top), 2))
        if (c0 == 2 && groups.size > 1 && rc[groups[1]] == 2) {
            val a = groups[0]; val b = groups[1]
            return pack(2, listOf(a, b) + kick(listOf(a, b), 1))
        }
        if (c0 == 2) return pack(1, listOf(top) + kick(listOf(top), 3))
        return pack(0, kick(emptyList(), 5))
    }

    // ------------------------------------------------------------------ equity
    /** Equity contra maos aleatorias (usada so como referencia). */
    fun equity(hero: IntArray, board: IntArray, opps: Int, sims: Int = 1500): Double {
        val used = BooleanArray(52)
        hero.forEach { used[it] = true }; board.forEach { used[it] = true }
        val rest = (0 until 52).filter { !used[it] }.toIntArray()
        val need = 2 * opps + 5 - board.size
        val rnd = Random(System.nanoTime())
        val buf = IntArray(7)
        var total = 0.0
        repeat(sims) {
            for (i in 0 until need) {
                val j = i + rnd.nextInt(rest.size - i)
                val t = rest[i]; rest[i] = rest[j]; rest[j] = t
            }
            buf[0] = hero[0]; buf[1] = hero[1]
            for (i in 0 until 5) buf[2 + i] = if (i < board.size) board[i] else rest[2 * opps + (i - board.size)]
            val h = score(buf, 7)
            var best = Long.MIN_VALUE
            for (o in 0 until opps) {
                buf[0] = rest[2 * o]; buf[1] = rest[2 * o + 1]
                val s = score(buf, 7)
                if (s > best) best = s
            }
            total += if (h > best) 1.0 else if (h == best) 0.5 else 0.0
        }
        return total / sims
    }

    private fun makePool(combos: List<IntArray>, used: BooleanArray): Pool {
        val hs = ArrayList<IntArray>(); val cum = ArrayList<Double>(); var t = 0.0
        for (c in combos) if (!used[c[0]] && !used[c[1]]) { t += 1.0; hs.add(c); cum.add(t) }
        return Pool(hs, cum.toDoubleArray())
    }

    /** Equity contra maos sorteadas de um pool ponderado (range do vilao). */
    fun equityPool(hero: IntArray, board: IntArray, opps: Int, pool: Pool, sims: Int): Double {
        if (pool.hands.isEmpty()) return equity(hero, board, opps, sims)
        val used = BooleanArray(52)
        hero.forEach { used[it] = true }; board.forEach { used[it] = true }
        val rest = (0 until 52).filter { !used[it] }.toIntArray()
        val rnd = Random(System.nanoTime())
        val buf = IntArray(7)
        val dead = BooleanArray(52)
        val oh = Array(opps) { IntArray(2) }
        val run = IntArray(5)
        var total = 0.0
        var done = 0
        var tries = 0
        while (done < sims && tries < sims * 4) {
            tries++
            java.util.Arrays.fill(dead, false)
            var ok = true
            for (o in 0 until opps) {
                var pick: IntArray? = null
                for (t in 0 until 8) {
                    val c = pool.pick(rnd)
                    if (!dead[c[0]] && !dead[c[1]]) { pick = c; break }
                }
                if (pick == null) { ok = false; break }
                dead[pick[0]] = true; dead[pick[1]] = true
                oh[o][0] = pick[0]; oh[o][1] = pick[1]
            }
            if (!ok) continue
            for (i in board.indices) run[i] = board[i]
            for (i in board.size until 5) {
                var c: Int
                do { c = rest[rnd.nextInt(rest.size)] } while (dead[c])
                dead[c] = true; run[i] = c
            }
            buf[0] = hero[0]; buf[1] = hero[1]
            for (i in 0 until 5) buf[2 + i] = run[i]
            val h = score(buf, 7)
            var best = Long.MIN_VALUE
            for (o in 0 until opps) {
                buf[0] = oh[o][0]; buf[1] = oh[o][1]
                val s = score(buf, 7)
                if (s > best) best = s
            }
            total += if (h > best) 1.0 else if (h == best) 0.5 else 0.0
            done++
        }
        return if (done == 0) equity(hero, board, opps, sims) else total / done
    }

    // ------------------------------------------------------------------ leitura da mao e do board
    fun classify(h0: Int, h1: Int, board: IntArray): Feat {
        val n = board.size
        val cards = IntArray(n + 2)
        cards[0] = h0; cards[1] = h1
        for (i in 0 until n) cards[2 + i] = board[i]
        val cat = (score(cards, n + 2) shr 20).toInt()
        val bcat = if (n >= 3) (score(board, n) shr 20).toInt() else 0
        val hr0 = h0 shr 2
        val hr1 = h1 shr 2
        val hiHole = maxOf(hr0, hr1)
        val holePair = hr0 == hr1
        val counts = IntArray(13)
        for (c in cards) counts[c shr 2]++
        val ranks = board.map { it shr 2 }.distinct().sortedDescending()
        val bmax = ranks[0]
        val second = if (ranks.size > 1) ranks[1] else -1

        fun pairLevel(pr: Int): Int = if (holePair) {
            if (pr > bmax) 3 else if (pr > second) 2 else 1
        } else {
            if (pr >= bmax) 3 else if (pr == second) 2 else 1
        }
        val aceHigh = if (hiHole == 12) 1 else 0

        val level = when {
            cat >= 6 && cat > bcat -> 6
            cat in 4..5 && cat > bcat -> 5
            cat == 3 -> {
                val tr = (12 downTo 0).first { counts[it] == 3 }
                if (tr == hr0 || tr == hr1) 4 else aceHigh
            }
            cat == 2 -> {
                val prs = (12 downTo 0).filter { counts[it] == 2 }
                val withHole = prs.filter { it == hr0 || it == hr1 }
                when {
                    withHole.size >= 2 -> 4
                    withHole.size == 1 -> pairLevel(withHole[0])
                    else -> aceHigh
                }
            }
            cat == 1 -> {
                val pr = (12 downTo 0).first { counts[it] == 2 }
                if (pr == hr0 || pr == hr1) pairLevel(pr) else aceHigh
            }
            else -> aceHigh
        }

        var fd = false; var nutFd = false; var oesd = false; var gut = false; var outs = 0
        if (n in 3..4 && cat < 5) {
            val sc = IntArray(4)
            for (c in cards) sc[c and 3]++
            for (s in 0..3) {
                if (sc[s] == 4 && ((h0 and 3) == s || (h1 and 3) == s)) {
                    fd = true
                    nutFd = ((h0 and 3) == s && hr0 == 12) || ((h1 and 3) == s && hr1 == 12)
                }
            }
            if (cat < 4) {
                var hm = 0; var bm = 0
                for (c in cards) hm = hm or (1 shl (c shr 2))
                for (c in board) bm = bm or (1 shl (c shr 2))
                var heroOuts = 0
                for (r in 0..12) {
                    if ((hm and (1 shl r)) != 0) continue
                    if (straightHigh(hm or (1 shl r)) >= 0 && straightHigh(bm or (1 shl r)) < 0) heroOuts++
                }
                if (heroOuts >= 2) oesd = true else if (heroOuts == 1) gut = true
            }
            outs = (if (fd) 9 else 0) + (if (oesd) 8 else if (gut) 4 else 0)
            if (fd && oesd) outs -= 2
        }
        val over = (if (hr0 > bmax) 1 else 0) + (if (hr1 > bmax) 1 else 0)
        return Feat(level, fd, nutFd, oesd, gut, outs, over, hiHole == 12)
    }

    fun texture(board: IntArray): Texture {
        val sc = IntArray(4)
        for (c in board) sc[c and 3]++
        val smax = sc.maxOrNull() ?: 0
        val ranks = board.map { it shr 2 }.distinct().sortedDescending()
        val paired = ranks.size < board.size
        var conn = 0
        for (i in 0 until ranks.size - 1) if (ranks[i] - ranks[i + 1] <= 2) conn++
        var wet = 0
        if (smax >= 3) wet += 2 else if (smax == 2 && board.size == 3) wet += 1
        wet += when { conn >= 2 -> 2; conn == 1 -> 1; else -> 0 }
        var label = when {
            smax >= 3 -> "monotone"
            wet >= 3 -> "molhado"
            wet == 2 -> "medio"
            else -> "seco"
        }
        if (paired) label += " pareado"
        return Texture(wet, label)
    }

    private fun describeHand(f: Feat): String {
        val d = ArrayList<String>()
        if (f.fd) d.add(if (f.nutFd) "FD nuts" else "FD")
        if (f.oesd) d.add("OESD") else if (f.gut) d.add("gutshot")
        return if (d.isEmpty()) LEVELS[f.level] else "${LEVELS[f.level]} + ${d.joinToString("/")}"
    }

    // ------------------------------------------------------------------ modelo do vilao pos-flop
    /** Range pre-flop presumido do vilao, conforme o papel do heroi na mao. */
    private fun villainPre(role: Int, opps: Int): List<IntArray> {
        val widen = 1.0 + 0.12 * (opps - 1)
        val classes = when (role) {
            1 -> Ranges.slice(2.5, 24.0 * widen)  // heroi abriu: vilao pagou (sem as maos de 3-bet)
            2 -> Ranges.top(22.0 * widen)         // heroi pagou: vilao e o agressor
            else -> Ranges.top(35.0 * widen)
        }
        return Ranges.pool(classes)
    }

    /** Peso: chance de o vilao ter a mao, dada a acao dele (0 check, 1 aposta pequena, 2 media, 3 grande). */
    private fun accept(f: Feat, act: Int, street: Int, multi: Boolean): Double {
        val strongDraw = f.fd || f.oesd
        var w = when (act) {
            0 -> when { f.level >= 5 -> 0.5; f.level == 4 -> 0.7; f.level == 3 -> 0.8; else -> 1.0 }
            1 -> when { f.level >= 3 -> 1.0; f.level == 2 -> 0.55; strongDraw -> 0.6; f.gut -> 0.3; else -> 0.2 }
            2 -> when {
                f.level >= 4 -> 1.0; f.level == 3 -> 0.85; strongDraw -> 0.7; f.level == 2 -> 0.3
                f.gut -> 0.25; f.level == 1 -> 0.07; else -> 0.12
            }
            else -> when {
                f.level >= 4 -> 1.0; f.level == 3 -> 0.6; strongDraw -> 0.5; f.level == 2 -> 0.12
                f.gut -> 0.1; f.level == 1 -> 0.03; else -> 0.08
            }
        }
        if (act > 0 && f.level <= 2) {
            if (street == 2) w *= 0.85
            if (street == 3) w *= 0.7
            if (multi) w *= 0.7
        }
        return w
    }

    /** Chance de o vilao pagar uma aposta do heroi (0 = 33% pot, 1 = 55%, 2 = 75%). */
    private fun callW(f: Feat, s: Int): Double {
        val strongDraw = f.fd || f.oesd
        return when (s) {
            0 -> when { f.level >= 3 -> 1.0; f.level == 2 -> 0.9; strongDraw -> 0.9; f.level == 1 -> 0.5; f.gut -> 0.5; else -> 0.2 }
            1 -> when { f.level >= 3 -> 1.0; f.level == 2 -> 0.7; strongDraw -> 0.85; f.level == 1 -> 0.28; f.gut -> 0.3; else -> 0.07 }
            else -> when { f.level >= 4 -> 1.0; f.level == 3 -> 0.85; strongDraw -> 0.7; f.level == 2 -> 0.45; f.level == 1 -> 0.15; f.gut -> 0.15; else -> 0.03 }
        }
    }

    // ------------------------------------------------------------------ formatacao
    private fun n(x: Double) = x.toLong().toString()
    private fun pc(x: Double) = "${(x * 100).toInt()}%"

    // ------------------------------------------------------------------ decisao
    /**
     * Pre-flop: tabelas de range por POSICAO x STACK (80-100, 50-79, 30-49, 20-29, 15, 10 BB),
     * ajustadas pela FASE do torneio e pelo PERFIL de stack.
     * Pos-flop: classifica sua mao (par, draws), le a textura do board, estima o range do
     * vilao pela acao dele e decide por EV (aposta/check) ou pot odds + implied odds (call).
     *
     * role: 0 desconhecido, 1 voce abriu (agressor), 2 voce pagou/BB (caller).
     */
    fun suggest(hero: IntArray, board: IntArray, opps: Int, pot: Double, toCall: Double, heroBet: Double,
                stack: Double, bb: Double, phase: Int, pos: Int, openerSel: Int, profile: Int, role: Int,
                ante: Boolean = false): Advice =
        if (board.isEmpty()) preflop(hero, opps, pot, toCall, heroBet, stack, bb, phase, pos, openerSel, profile, ante)
        else postflop(hero, board, opps, pot, toCall, heroBet, stack, bb, phase, pos, profile, role)

    private fun rangeEq(hero: IntArray, pct: Double, push: Boolean = false, sims: Int = 700): Double {
        val used = BooleanArray(52)
        hero.forEach { used[it] = true }
        val pool = makePool(Ranges.pool(Ranges.top(pct, push)), used)
        return equityPool(hero, IntArray(0), 1, pool, sims)
    }

    private fun preflop(hero: IntArray, opps: Int, pot: Double, toCall: Double, heroBet: Double,
                        stack: Double, bb: Double, phase: Int, pos: Int, openerSel: Int, profile: Int, ante: Boolean): Advice {
        val bbv = maxOf(bb, 1.0)
        val stackBb = stack / bbv
        val b = Ranges.bucket(stackBb)
        val cls = Ranges.className(hero[0], hero[1])
        val icm = icmPrem(phase, profile)
        val openM = openMult(phase, profile, ante)
        val tight3 = (1.0 - 1.5 * icm).coerceIn(0.6, 1.0)
        val tightCall = (1.0 - 3.0 * icm).coerceIn(0.5, 1.0)
        val totalMax = toCall + heroBet
        val ip = Pos.inPosition(pos)
        val head = "$cls | ${Pos.NAMES[pos]} | ${Ranges.BUCKETS[b]} BB | ${PHASES[phase.coerceIn(0, 5)]}"
        val heroRaised = heroBet >= bbv * 1.8
        val allIn = toCall > bbv * 1.5 && toCall >= stack * 0.85

        // 1) BB sem aposta na frente
        if (pos == Pos.BB && toCall <= 0.0 && totalMax <= bbv * 1.05) {
            return Advice("CHECK", equity(hero, IntArray(0), 1, 500), "$head | ninguem abriu: check")
        }

        // 2) Ninguem abriu: ranges de abertura (RFI) / push
        if (!allIn && totalMax <= bbv * 1.05) {
            val pct = (Ranges.rfi(if (pos == Pos.BB) Pos.SB else pos, b) * openM).coerceAtMost(100.0)
            val set = Ranges.top(pct, b >= 4)
            val inR = cls in set
            val size = bbv * (if (pos == Pos.SB) 3.0 else if (b <= 1) 2.2 else 2.0)
            val act = when {
                inR && b >= 4 -> "ALL-IN (${n(stack)})"
                inR -> "RAISE p/ ${n(size)}"
                toCall <= 0.0 -> "CHECK"
                else -> "FOLD"
            }
            val why = "$head | abrir ${pct.toInt()}% das maos | " + (if (inR) "dentro do range" else "fora do range") +
                (if (b >= 4) " (push/fold)" else "")
            return Advice(act, equity(hero, IntArray(0), 1, 500), why, Ranges.describe(set))
        }

        // 3) Enfrentando all-in (ou aposta enorme): equity contra o range de quem empurrou
        val fourBetCtx = heroRaised && heroBet > bbv * 3.6
        if (allIn || fourBetCtx || (!heroRaised && totalMax > bbv * 4.6)) {
            val pct = if (allIn) Ranges.shoveRangePct(stackBb + heroBet / bbv) else 4.5
            val eq = rangeEq(hero, pct, allIn)
            val need = toCall / (pot + toCall)
            val icmUse = icm * minOf(1.0, toCall / maxOf(stack, 1.0) * 1.5)
            val req = need + icmUse + 0.01
            val why = "$head | vilao ${pct.toInt()}% | eq ${pc(eq)} vs odds ${pc(need)} (+ICM ${pc(icmUse)})"
            val act = when {
                eq >= req && allIn -> "CALL ALL-IN (${n(toCall)})"
                eq >= 0.68 && !allIn -> {
                    val to = (toCall + heroBet) * 2.4
                    if (to >= 0.4 * (stack + heroBet)) "ALL-IN (${n(stack)})" else "RAISE p/ ${n(to)} (4-bet)"
                }
                eq >= req -> "CALL ${n(toCall)}"
                else -> "FOLD"
            }
            return Advice(act, eq, why, Ranges.describe(Ranges.top(pct, allIn)))
        }

        // 4) Heroi abriu e enfrentou 3-bet
        if (heroRaised && toCall > 0.0) {
            val (four, calls, openSet) = Ranges.vs3bet(pos, b, openM, tightCall)
            val to = (toCall + heroBet) * (if (ip) 2.2 else 2.4)
            val act = when {
                cls in four -> if (b >= 2 || to >= 0.4 * (stack + heroBet)) "ALL-IN (${n(stack)})" else "RAISE p/ ${n(to)} (4-bet)"
                cls in calls -> "CALL ${n(toCall)}"
                else -> "FOLD"
            }
            val rng = "4bet/shove: ${Ranges.describe(four)} || Call: ${Ranges.describe(calls)}"
            return Advice(act, rangeEq(hero, Ranges.rfi(pos, b) * openM * 0.45, b >= 4), "$head | enfrentando 3-bet (abertura ${openSet.size} maos)", rng)
        }

        // 5) Enfrentando um open normal: 3-bet / call / fold
        val opener = if (openerSel <= 0) Pos.autoOpener(pos) else openerSel - 1
        val (three, calls) = Ranges.vsOpen(pos, opener, b, tight3, tightCall * (if (opps > 1) 0.85 else 1.0))
        val sizeTo = totalMax * (if (ip) 3.0 else 3.8)
        val act = when {
            cls in three -> if (b >= 3 || sizeTo >= 0.4 * (stack + heroBet)) "ALL-IN (${n(stack)})" else "RAISE p/ ${n(sizeTo)} (3-bet)"
            cls in calls -> "CALL ${n(toCall)}"
            else -> "FOLD"
        }
        val tag = if (b >= 3) "reshove" else "3-bet"
        val rng = "$tag: ${Ranges.describe(three)} || Call: ${Ranges.describe(calls)}"
        val eq = rangeEq(hero, Ranges.rfi(opener, b) * openM, b >= 4)
        return Advice(act, eq, "$head | vs open de ${Pos.NAMES[opener]} | eq ${pc(eq)} vs range dele", rng)
    }

    private fun postflop(hero: IntArray, board: IntArray, opps: Int, pot: Double, toCall: Double, heroBet: Double,
                         stack: Double, bb: Double, phase: Int, pos: Int, profile: Int, role: Int): Advice {
        val bbv = maxOf(bb, 1.0)
        val street = board.size - 2           // 1 flop, 2 turn, 3 river
        val f = classify(hero[0], hero[1], board)
        val tex = texture(board)
        val ip = Pos.inPosition(pos)
        val multi = opps > 1
        val spr = stack / maxOf(pot, 1.0)
        val used = BooleanArray(52)
        hero.forEach { used[it] = true }; board.forEach { used[it] = true }
        val combos = villainPre(role, opps).filter { !used[it[0]] && !used[it[1]] }
        val feats = combos.map { classify(it[0], it[1], board) }

        fun pool(w: (Feat) -> Double): Pool {
            val hs = ArrayList<IntArray>(); val cum = ArrayList<Double>(); var t = 0.0
            for (i in combos.indices) {
                val x = w(feats[i])
                if (x > 0.0) { t += x; hs.add(combos[i]); cum.add(t) }
            }
            return Pool(hs, cum.toDoubleArray())
        }

        val handTxt = describeHand(f)
        val head = "$handTxt | board ${tex.label} | SPR ${"%.1f".format(spr)}"
        val icmBase = icmPrem(phase, profile)

        // ---------- enfrentando aposta
        if (toCall > 0.0) {
            val betFrac = toCall / maxOf(pot - toCall, bbv)   // pot ja inclui a aposta que o heroi enfrenta
            val act = if (betFrac <= 0.40) 1 else if (betFrac <= 0.80) 2 else 3
            val p = pool { accept(it, act, street, multi) }
            val eq = equityPool(hero, board, opps, p, 1300)
            val need = toCall / (pot + toCall)
            val allIn = toCall >= stack * 0.95
            val icm = icmBase * minOf(1.0, toCall / maxOf(stack, 1.0) * 1.5)
            var implied = 0.0
            if (!allIn && street <= 2 && spr > 2.5) {
                if (f.fd || f.oesd) implied = if (street == 1) 0.05 else 0.03 else if (f.gut) implied = 0.015
                if (f.nutFd) implied += 0.015
            }
            val req = if (allIn) need + icm + 0.005
            else need + icm + 0.01 + (if (!ip) 0.02 else 0.0) + 0.02 * (opps - 1) - implied
            val why = "$head | aposta ${(betFrac * 100).toInt()}% pot | eq ${pc(eq)} vs range dele | precisa ${pc(maxOf(req, 0.0))}"
            val raiseTo = toCall * 3
            val bluffRaise = !allIn && street == 1 && opps == 1 && betFrac <= 0.6 && f.outs >= 12 && eq >= 0.42
            val action = when {
                allIn -> if (eq >= req) "CALL ALL-IN (${n(toCall)})" else "FOLD"
                spr <= 2.0 && eq >= 0.70 -> "ALL-IN (${n(stack)})"
                f.level >= 4 && eq >= 0.78 -> if (raiseTo >= stack * 0.6) "ALL-IN (${n(stack)})" else "RAISE p/ ${n(raiseTo)}"
                bluffRaise -> if (raiseTo >= stack * 0.6) "ALL-IN (${n(stack)})" else "RAISE p/ ${n(raiseTo)} (semi-bluff)"
                eq >= req -> "CALL ${n(toCall)}"
                else -> "FOLD"
            }
            return Advice(action, eq, why)
        }

        // ---------- sem aposta na frente: apostar ou dar check (decide por EV)
        fun node(x: Feat) = if (ip) accept(x, 0, street, multi) else 1.0
        val rangeNode = pool { node(it) }
        val eqNow = equityPool(hero, board, opps, rangeNode, 900)
        val real = if (ip) 0.95 else 0.85
        val evCheck = eqNow * pot * real
        if (spr <= 1.5 && eqNow >= 0.5) {
            return Advice("ALL-IN (${n(stack)})", eqNow, "$head | SPR baixo: stack comprometido | eq ${pc(eqNow)}")
        }
        val sizes = doubleArrayOf(0.33, 0.55, 0.75)
        val labels = listOf("33% pot", "55% pot", "75% pot")
        var bestEv = evCheck
        var bestS = -1
        val evs = ArrayList<String>()
        for (s in 0..2) {
            val bet = minOf(pot * sizes[s], stack)
            if (bet <= 0.0) continue
            if (street == 3 && f.level <= 1 && !f.blocker) { evs.add("-"); continue }   // river: sem blefe sem bloqueador
            val pc2 = pool { node(it) * callW(it, s) }
            val fold = if (rangeNode.total > 0) 1.0 - pc2.total / rangeNode.total else 0.0
            val foldAll = Math.pow(fold.coerceIn(0.0, 1.0), opps.toDouble())
            val eqC = if (pc2.hands.isEmpty()) 1.0 else equityPool(hero, board, opps, pc2, 600)
            val ev = foldAll * pot + (1 - foldAll) * (eqC * (pot + bet) - (1 - eqC) * bet)
            evs.add("${(ev / maxOf(pot, 1.0) * 100).toInt()}")
            if (ev > bestEv + 0.02 * pot) { bestEv = ev; bestS = s }
        }
        val why = "$head | eq ${pc(eqNow)} | EV(check ${(evCheck / maxOf(pot, 1.0) * 100).toInt()} | bets ${evs.joinToString("/")} % pot)"
        if (bestS < 0) return Advice("CHECK", eqNow, why)
        val bet = minOf(pot * sizes[bestS], stack)
        val act = if (bet >= stack * 0.9) "ALL-IN (${n(stack)})" else "APOSTAR ${n(bet)} (${labels[bestS]})"
        return Advice(act, eqNow, why)
    }

    // ------------------------------------------------------------------ leitura de texto (OCR)
    fun parseNum(t: String): Double? {
        var s = t.uppercase().replace('O', '0').replace(" ", "").filter { it.isDigit() || it in ".,KM" }
        var mult = 1.0
        if (s.endsWith("K")) { mult = 1e3; s = s.dropLast(1) } else if (s.endsWith("M")) { mult = 1e6; s = s.dropLast(1) }
        s = s.filter { it.isDigit() || it in ".," }
        s = if (mult == 1.0) s.replace(".", "").replace(",", "") else s.replace(",", ".")
        return s.toDoubleOrNull()?.times(mult)
    }

    fun parseAction(t: String): String? {
        val l = t.lowercase()
        if (FOLD_WORDS.any { l.contains(it) }) return "fold"
        if (CHECK_WORDS.any { l.contains(it) }) return "check"
        return null
    }
}
