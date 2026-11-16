package com.estudo.pokerassistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.math.sqrt

data class Box(val x: Int, val y: Int, val w: Int, val h: Int)

object Cfg {
    /** true = tela na horizontal. As areas calibradas sao guardadas separadamente por orientacao. */
    @Volatile var land = false
    private fun pre() = if (land) "L_" else "P_"
    private fun sp(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    fun get(c: Context, key: String): Box? {
        val s = sp(c).getString(pre() + key, null) ?: return null
        val p = s.split(",").map { it.toInt() }
        return Box(p[0], p[1], p[2], p[3])
    }
    fun set(c: Context, key: String, b: Box) {
        sp(c).edit().putString(pre() + key, "${b.x},${b.y},${b.w},${b.h}").apply()
    }
    fun clearBoxes(c: Context) {
        val e = sp(c).edit()
        sp(c).all.keys.filter { it.startsWith(pre()) }.forEach { e.remove(it) }
        e.apply()
    }
    fun ante(c: Context) = sp(c).getBoolean("ante", false)
    fun setAnte(c: Context, v: Boolean) { sp(c).edit().putBoolean("ante", v).apply() }
    /** true = o numero lido em "pot" ja inclui as apostas da rua atual. */
    fun potIncl(c: Context) = sp(c).getBoolean("potIncl", true)
    fun setPotIncl(c: Context, v: Boolean) { sp(c).edit().putBoolean("potIncl", v).apply() }
    fun pos(c: Context) = sp(c).getInt("pos", 5)
    fun setPos(c: Context, v: Int) { sp(c).edit().putInt("pos", v).apply() }
    fun opener(c: Context) = sp(c).getInt("opener", 0)
    fun setOpener(c: Context, v: Int) { sp(c).edit().putInt("opener", v).apply() }
    fun profile(c: Context) = sp(c).getInt("profile", 0)
    fun setProfile(c: Context, v: Int) { sp(c).edit().putInt("profile", v).apply() }
    fun showRange(c: Context) = sp(c).getBoolean("showRange", false)
    fun setShowRange(c: Context, v: Boolean) { sp(c).edit().putBoolean("showRange", v).apply() }
    fun bb(c: Context) = sp(c).getFloat("bb", 100f).toDouble()
    fun setBb(c: Context, v: Double) { sp(c).edit().putFloat("bb", v.toFloat()).apply() }
    fun phase(c: Context) = sp(c).getInt("phase", 0)
    fun setPhase(c: Context, v: Int) { sp(c).edit().putInt("phase", v).apply() }
}

fun crop(b: Bitmap, r: Box): Bitmap {
    val x = r.x.coerceIn(0, b.width - 1); val y = r.y.coerceIn(0, b.height - 1)
    val w = r.w.coerceIn(1, b.width - x); val h = r.h.coerceIn(1, b.height - y)
    return Bitmap.createBitmap(b, x, y, w, h)
}

object Ocr {
    private val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    fun text(bmp: Bitmap): String {
        return try {
            var b = bmp
            if (b.height < 64) {
                val f = (64 + b.height - 1) / b.height
                b = Bitmap.createScaledBitmap(b, b.width * f, b.height * f, true)
            }
            Tasks.await(rec.process(InputImage.fromBitmap(b, 0))).text.replace("\n", " ")
        } catch (e: Exception) { "" }
    }
}

// ---------- treino automatico de simbolos (13 valores + 4 naipes) ----------
sealed class Read {
    object Empty : Read()
    class Ok(val name: String) : Read()
    class Ask(val kind: Char, val bmp: Bitmap, val vec: FloatArray) : Read()
}

class Glyphs(ctx: Context) {
    private val file = File(ctx.filesDir, "glyphs.txt")
    private val data = HashMap<String, MutableList<FloatArray>>()
    init {
        if (file.exists()) file.readLines().forEach { l ->
            val p = l.split("|")
            if (p.size == 2) data.getOrPut(p[0]) { ArrayList() }.add(p[1].split(",").map { it.toFloat() }.toFloatArray())
        }
    }
    fun count() = data.size
    fun add(kind: Char, ch: Char, v: FloatArray) {
        val l = data.getOrPut("$kind:$ch") { ArrayList() }
        if (l.size >= 8) l.removeAt(0)
        l.add(v); save()
    }
    fun clear() { data.clear(); file.delete() }
    private fun save() {
        file.writeText(data.flatMap { (k, l) -> l.map { "$k|${it.joinToString(",")}" } }.joinToString("\n"))
    }
    fun match(kind: Char, v: FloatArray, thr: Float): Char? {
        var best = -1f; var ch: Char? = null
        for ((k, l) in data) {
            if (k[0] != kind) continue
            for (t in l) {
                var d = 0f
                for (i in v.indices) d += v[i] * t[i]
                if (d > best) { best = d; ch = k[2] }
            }
        }
        return if (best >= thr) ch else null
    }
}

object GlyphParser {
    const val GW = 20
    const val GH = 28
    class Parsed(val rank: FloatArray, val suit: FloatArray, val rankBmp: Bitmap, val suitBmp: Bitmap)

    private fun segs(c: IntArray, minLen: Int = 3): List<IntArray> {
        val out = ArrayList<IntArray>(); var s = -1
        for (i in 0..c.size) {
            val on = i < c.size && c[i] > 0
            if (on && s < 0) s = i
            if (!on && s >= 0) { if (i - s >= minLen) out.add(intArrayOf(s, i - 1)); s = -1 }
        }
        return out
    }

    /** Separa o canto da carta em [valor] e [naipe]. Retorna null se nao parecer uma carta. */
    fun parse(b: Bitmap): Parsed? {
        val w = b.width; val h = b.height
        if (w < 12 || h < 12) return null
        val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h)
        val lum = IntArray(w * h) {
            val p = px[it]; (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)).toInt()
        }
        if (lum.count { it > 180 } < w * h * 0.35) return null  // fundo nao e claro: sem carta
        val bg = lum.sortedArray()[(w * h * 0.9).toInt().coerceAtMost(w * h - 1)]
        val ink = BooleanArray(w * h)
        val m = 2
        for (y in m until h - m) for (x in m until w - m) {
            val i = y * w + x; val p = px[i]
            val sat = maxOf(Color.red(p), Color.green(p), Color.blue(p)) - minOf(Color.red(p), Color.green(p), Color.blue(p))
            ink[i] = lum[i] < bg - 50 || (sat > 90 && lum[i] < bg - 20)
        }
        fun bbox(x0: Int, x1: Int, y0: Int, y1: Int): IntArray? {
            var a = w; var bx = -1; var c = h; var d = -1
            for (y in y0..y1) for (x in x0..x1) if (ink[y * w + x]) {
                if (x < a) a = x; if (x > bx) bx = x; if (y < c) c = y; if (y > d) d = y
            }
            return if (bx < 0) null else intArrayOf(a, c, bx, d)
        }
        val rowC = IntArray(h) { y -> (0 until w).count { ink[y * w + it] } }
        val rs = segs(rowC)
        val rb: IntArray?; val sb: IntArray?
        if (rs.size >= 2) {  // valor em cima, naipe embaixo
            rb = bbox(0, w - 1, rs.first()[0], rs.first()[1])
            sb = bbox(0, w - 1, rs.last()[0], rs.last()[1])
        } else {  // valor e naipe lado a lado
            val colC = IntArray(w) { x -> (0 until h).count { ink[it * w + x] } }
            val cs = segs(colC)
            if (cs.size < 2) return null
            rb = bbox(cs.first()[0], cs[cs.size - 2][1], 0, h - 1)
            sb = bbox(cs.last()[0], cs.last()[1], 0, h - 1)
        }
        if (rb == null || sb == null) return null

        fun vec(r: IntArray): FloatArray? {
            val bw = r[2] - r[0] + 1; val bh = r[3] - r[1] + 1
            val v = FloatArray(GW * GH)
            for (gy in 0 until GH) for (gx in 0 until GW) {
                val sx0 = r[0] + gx * bw / GW; val sx1 = maxOf(sx0 + 1, r[0] + (gx + 1) * bw / GW)
                val sy0 = r[1] + gy * bh / GH; val sy1 = maxOf(sy0 + 1, r[1] + (gy + 1) * bh / GH)
                var c = 0; var t = 0
                for (y in sy0 until minOf(sy1, h)) for (x in sx0 until minOf(sx1, w)) { t++; if (ink[y * w + x]) c++ }
                v[gy * GW + gx] = if (t == 0) 0f else c.toFloat() / t
            }
            val mean = v.average().toFloat(); var n = 0f
            for (i in v.indices) { v[i] -= mean; n += v[i] * v[i] }
            if (n < 1e-4f) return null
            val d = sqrt(n)
            for (i in v.indices) v[i] /= d
            return v
        }
        fun bm(r: IntArray): Bitmap {
            val x = maxOf(r[0] - 2, 0); val y = maxOf(r[1] - 2, 0)
            return Bitmap.createBitmap(b, x, y, minOf(r[2] - r[0] + 5, w - x), minOf(r[3] - r[1] + 5, h - y))
        }
        val rv = vec(rb) ?: return null
        val sv = vec(sb) ?: return null
        return Parsed(rv, sv, bm(rb), bm(sb))
    }
}

class CardReader(private val glyphs: Glyphs) {
    fun read(corner: Bitmap): Read {
        val p = GlyphParser.parse(corner) ?: return Read.Empty
        val r = glyphs.match('r', p.rank, 0.82f) ?: return Read.Ask('r', p.rankBmp, p.rank)
        val s = glyphs.match('s', p.suit, 0.82f) ?: return Read.Ask('s', p.suitBmp, p.suit)
        return Read.Ok("$r$s")
    }
}
