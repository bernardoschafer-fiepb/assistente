package com.estudo.pokerassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

class CaptureService : Service() {
    private lateinit var wm: WindowManager
    private var mp: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private val ui = Handler(Looper.getMainLooper())
    private val exec = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var frame: Bitmap? = null
    @Volatile private var busy = false
    private var W = 0
    private var H = 0
    private var dpi = 0
    private lateinit var glyphs: Glyphs
    private lateinit var cardReader: CardReader
    private lateinit var phaseBtn: Button
    private lateinit var posBtn: Button
    private lateinit var openerBtn: Button
    private lateinit var profBtn: Button
    private lateinit var rangeBtn: Button
    private lateinit var anteBtn: Button
    private lateinit var potBtn: Button
    private lateinit var rangeView: TextView
    private var preMaxHeroBet = 0.0
    private var lastHeroKey: String? = null
    @Volatile private var cooldownUntil = 0L

    private lateinit var panel: LinearLayout
    private lateinit var panelLp: WindowManager.LayoutParams
    private lateinit var actionView: TextView
    private lateinit var infoView: TextView

    private val folded = HashSet<Int>()
    private var lastKey: String? = null
    private val ocrCache = HashMap<String, Pair<Int, String>>()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ch", "Assistente", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "ch").setContentTitle("Assistente ativo")
            .setSmallIcon(android.R.drawable.ic_menu_view).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(1, n)

        if (mp != null) return START_NOT_STICKY   // ja esta rodando: ignora segundo start
        val code = intent.getIntExtra("code", 0)
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra("data", Intent::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra("data")
        if (data == null) { stopSelf(); return START_NOT_STICKY }

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        glyphs = Glyphs(this); cardReader = CardReader(glyphs)
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mp = mpm.getMediaProjection(code, data)
        mp!!.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, ui)

        val sz = screenSize()
        W = sz[0]; H = sz[1]; dpi = sz[2]
        Cfg.land = W > H
        reader = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 2)
        vd = mp!!.createVirtualDisplay("cap", W, H, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, null)

        buildPanel()
        exec.scheduleWithFixedDelay({
            try { tick() } catch (t: Throwable) { show("Erro", t.message ?: t.toString()) }
        }, 1, 1, TimeUnit.SECONDS)
        return START_NOT_STICKY
    }

    // ---------- janela flutuante ----------
    private fun overlayParams(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT
    )

    private fun btn(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 11f; isAllCaps = false
        setPadding(12, 4, 12, 4); minWidth = 0; minimumWidth = 0
        setOnClickListener { onClick() }
    }

    private fun buildPanel() {
        panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xCC000000.toInt()); setPadding(16, 8, 16, 8) }
        actionView = TextView(this).apply { textSize = 18f; maxWidth = (W * 0.8).toInt(); setTextColor(Color.WHITE); text = "Calibre as areas"; gravity = Gravity.CENTER }
        infoView = TextView(this).apply { textSize = 11f; maxWidth = (W * 0.8).toInt(); setTextColor(0xFFCCCCCC.toInt()); gravity = Gravity.CENTER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rangeView = TextView(this).apply { textSize = 10f; maxWidth = (W * 0.8).toInt(); setTextColor(0xFF9FD3FF.toInt()); gravity = Gravity.CENTER; visibility = View.GONE }
        row.addView(btn("Calibrar") { startCalibration() })
        phaseBtn = btn("Fase: ${Engine.PHASES[Cfg.phase(this)]}") {
            val p = (Cfg.phase(this) + 1) % Engine.PHASES.size
            Cfg.setPhase(this, p); phaseBtn.text = "Fase: ${Engine.PHASES[p]}"; lastKey = null
        }
        row.addView(phaseBtn)
        row.addView(btn("Zerar treino") { glyphs.clear(); lastKey = null; show("Treino zerado", "") })
        posBtn = btn("Pos: ${Pos.NAMES[Cfg.pos(this)]}") {
            val p = (Cfg.pos(this) + 1) % Pos.NAMES.size
            Cfg.setPos(this, p); posBtn.text = "Pos: ${Pos.NAMES[p]}"; lastKey = null
        }
        openerBtn = btn("Abre: ${Pos.OPENER_LABELS[Cfg.opener(this)]}") {
            val p = (Cfg.opener(this) + 1) % Pos.OPENER_LABELS.size
            Cfg.setOpener(this, p); openerBtn.text = "Abre: ${Pos.OPENER_LABELS[p]}"; lastKey = null
        }
        profBtn = btn("Stack: ${Engine.PROFILES[Cfg.profile(this)]}") {
            val p = (Cfg.profile(this) + 1) % Engine.PROFILES.size
            Cfg.setProfile(this, p); profBtn.text = "Stack: ${Engine.PROFILES[p]}"; lastKey = null
        }
        rangeBtn = btn(if (Cfg.showRange(this)) "Range: sim" else "Range: nao") {
            val v = !Cfg.showRange(this)
            Cfg.setShowRange(this, v); rangeBtn.text = if (v) "Range: sim" else "Range: nao"; lastKey = null
        }
        row3.addView(posBtn); row3.addView(openerBtn); row3.addView(profBtn); row3.addView(rangeBtn)
        anteBtn = btn(if (Cfg.ante(this)) "Ante: sim" else "Ante: nao") {
            val v = !Cfg.ante(this)
            Cfg.setAnte(this, v); anteBtn.text = if (v) "Ante: sim" else "Ante: nao"; lastKey = null
        }
        potBtn = btn(if (Cfg.potIncl(this)) "Pot: c/ apostas" else "Pot: s/ apostas") {
            val v = !Cfg.potIncl(this)
            Cfg.setPotIncl(this, v); potBtn.text = if (v) "Pot: c/ apostas" else "Pot: s/ apostas"; lastKey = null
        }
        val row4 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row4.addView(anteBtn); row4.addView(potBtn)
        row2.addView(btn("BB÷2") { Cfg.setBb(this, Cfg.bb(this) / 2); lastKey = null })
        row2.addView(btn("BB×2") { Cfg.setBb(this, Cfg.bb(this) * 2); lastKey = null })
        row2.addView(btn("Sair") { stopSelf() })
        panel.addView(actionView); panel.addView(infoView); panel.addView(rangeView)
        panel.addView(row); panel.addView(row3); panel.addView(row4); panel.addView(row2)
        panelLp = overlayParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        panelLp.gravity = Gravity.TOP or Gravity.START; panelLp.x = 0; panelLp.y = 0
        var sx = 0f; var sy = 0f; var px = 0; var py = 0
        actionView.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = panelLp.x; py = panelLp.y }
                MotionEvent.ACTION_MOVE -> {
                    panelLp.x = px + (e.rawX - sx).toInt(); panelLp.y = py + (e.rawY - sy).toInt()
                    wm.updateViewLayout(panel, panelLp)
                }
            }
            true
        }
        wm.addView(panel, panelLp)
    }

    private fun show(action: String, info: String, range: String = "") {
        ui.post {
            if (::actionView.isInitialized) {
                actionView.text = action; infoView.text = info
                val showR = Cfg.showRange(this) && range.isNotEmpty()
                rangeView.text = range; rangeView.visibility = if (showR) View.VISIBLE else View.GONE
            }
        }
    }

    // ---------- calibracao ----------
    inner class CalibView(ctx: Context, private val items: List<Pair<String, String>>, private val onEnd: () -> Unit) : FrameLayout(ctx) {
        private var sx = 0f; private var sy = 0f; private var ex = 0f; private var ey = 0f; private var has = false
        private var idx = 0
        private val rectPaint = Paint().apply { color = Color.GREEN; style = Paint.Style.STROKE; strokeWidth = 4f }
        private val textPaint = Paint().apply { color = Color.WHITE; textSize = 38f }
        init {
            setWillNotDraw(false); setBackgroundColor(0x44000000)
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xCC000000.toInt()) }
            row.addView(btn("Confirmar") { confirm() })
            row.addView(btn("Pular") { idx++; reset() })
            row.addView(btn("Terminar") { onEnd() })
            addView(row, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        }
        private fun reset() { has = false; if (idx >= items.size) onEnd() else invalidate() }
        private fun confirm() {
            if (has && abs(ex - sx) > 4 && abs(ey - sy) > 4) {
                Cfg.set(this@CaptureService, items[idx].first,
                    Box(min(sx, ex).toInt(), min(sy, ey).toInt(), abs(ex - sx).toInt(), abs(ey - sy).toInt()))
            }
            idx++; reset()
        }
        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ex = sx; ey = sy; has = true }
                else -> { ex = e.rawX; ey = e.rawY }
            }
            invalidate(); return true
        }
        override fun onDraw(c: Canvas) {
            val loc = IntArray(2); getLocationOnScreen(loc)
            if (has) c.drawRect(min(sx, ex) - loc[0], min(sy, ey) - loc[1], maxOf(sx, ex) - loc[0], maxOf(sy, ey) - loc[1], rectPaint)
            if (idx < items.size) c.drawText(items[idx].second, 40f, 100f, textPaint)
        }
    }

    private fun fullscreen(v: View): WindowManager.LayoutParams {
        val lp = overlayParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= 28) lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        wm.addView(v, lp); return lp
    }

    private fun startCalibration() {
        busy = true; panel.visibility = View.GONE; Cfg.clearBoxes(this); Cfg.setBb(this, Cfg.bb(this))
        val items = ArrayList<Pair<String, String>>()
        items += "h1" to "SUA carta 1: so o CANTO (valor+naipe)"; items += "h2" to "SUA carta 2: so o CANTO (valor+naipe)"
        for (i in 1..5) items += "b$i" to "Board $i: CANTO (valor+naipe)"
        items += "pot" to "Valor do POT"; items += "stack" to "SEU stack"; items += "hb" to "SUA aposta atual"
        for (i in 1..7) {
            items += "s${i}b" to "Adversario $i: valor da APOSTA"
            items += "s${i}a" to "Adversario $i: texto de ACAO (fold/check)"
        }
        lateinit var view: CalibView
        view = CalibView(this, items) {
            wm.removeView(view); panel.visibility = View.VISIBLE
            lastKey = null; folded.clear(); busy = false
        }
        fullscreen(view)
    }

    // ---------- treino automatico ----------
    private fun wbtn(text: String, size: Float = 16f, onClick: () -> Unit) = Button(this).apply {
        this.text = text; textSize = size; isAllCaps = false; setPadding(0, 20, 0, 20)
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { onClick() }
    }

    private fun askGlyph(a: Read.Ask) {
        busy = true
        ui.post {
            val mp = LinearLayout.LayoutParams.MATCH_PARENT
            val wc = LinearLayout.LayoutParams.WRAP_CONTENT
            val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xF0202020.toInt()); setPadding(24, 24, 24, 24) }
            val img = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.WHITE); setImageBitmap(a.bmp) }
            val tv = TextView(this).apply {
                setTextColor(Color.WHITE); textSize = 17f; gravity = Gravity.CENTER; setPadding(0, 12, 0, 12)
                text = if (a.kind == 'r') "Simbolo novo! Qual e o VALOR da carta?" else "Simbolo novo! Qual e o NAIPE?"
            }
            root.addView(img, LinearLayout.LayoutParams(mp, 260)); root.addView(tv, LinearLayout.LayoutParams(mp, wc))
            fun close(ch: Char?) {
                try { wm.removeView(root) } catch (_: Exception) {}
                if (ch != null) glyphs.add(a.kind, ch, a.vec) else cooldownUntil = System.currentTimeMillis() + 30000
                busy = false; lastKey = null
            }
            val r1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val r2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val r3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            if (a.kind == 'r') {
                Engine.RANKS.forEachIndexed { i, ch -> (if (i < 7) r1 else r2).addView(wbtn(if (ch == 'T') "10" else ch.toString()) { close(ch) }) }
            } else {
                listOf("♠" to 's', "♥" to 'h', "♦" to 'd', "♣" to 'c').forEach { (sym, su) -> r1.addView(wbtn(sym, 24f) { close(su) }) }
            }
            r3.addView(wbtn("Nao e carta") { close(null) })
            root.addView(r1, LinearLayout.LayoutParams(mp, wc))
            if (a.kind == 'r') root.addView(r2, LinearLayout.LayoutParams(mp, wc))
            root.addView(r3, LinearLayout.LayoutParams(mp, wc))
            val lp = overlayParams((W * 0.94).toInt(), wc, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            lp.gravity = Gravity.CENTER
            wm.addView(root, lp)
        }
    }

    // ---------- utilitarios de tela ----------
    private fun screenSize(): IntArray {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        return intArrayOf(dm.widthPixels, dm.heightPixels, dm.densityDpi)
    }

    /** A orientacao mudou: ajusta o espelho da tela e passa a usar as areas calibradas dessa orientacao. */
    private fun rebuildDisplay(sz: IntArray) {
        W = sz[0]; H = sz[1]; dpi = sz[2]
        Cfg.land = W > H
        val old = reader
        val nr = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 2)
        reader = nr
        vd?.resize(W, H, dpi)
        vd?.surface = nr.surface
        try { old?.close() } catch (_: Exception) {}
        frame = null; lastKey = null; lastHeroKey = null; folded.clear(); ocrCache.clear()
    }

    /** Miolo da tela todo preto: o app de poker provavelmente bloqueia a captura (FLAG_SECURE). */
    private fun isBlank(b: Bitmap): Boolean {
        val x0 = b.width / 20; val x1 = b.width - x0
        val y0 = b.height / 8; val y1 = b.height - y0
        val sx = maxOf(1, (x1 - x0) / 24); val sy = maxOf(1, (y1 - y0) / 24)
        val pl = panelLp.x - 40; val pt = panelLp.y - 40
        val pr = panelLp.x + panel.width + 40; val pb = panelLp.y + panel.height + 160
        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                if (!(x in pl..pr && y in pt..pb)) {
                    val p = b.getPixel(x, y)
                    if (Color.red(p) + Color.green(p) + Color.blue(p) > 24) return false
                }
                x += sx
            }
            y += sy
        }
        return true
    }

    // ---------- loop principal ----------
    private fun grab() {
        val img = reader?.acquireLatestImage() ?: return
        try {
            val p = img.planes[0]
            val pad = p.rowStride - p.pixelStride * W
            val bm = Bitmap.createBitmap(W + pad / p.pixelStride, H, Bitmap.Config.ARGB_8888)
            bm.copyPixelsFromBuffer(p.buffer)
            frame = if (pad == 0) bm else Bitmap.createBitmap(bm, 0, 0, W, H)
        } finally { img.close() }
    }

    private fun readText(f: Bitmap, key: String): String {
        val b = Cfg.get(this, key) ?: return ""
        val c = crop(f, b)
        val px = IntArray(c.width * c.height); c.getPixels(px, 0, c.width, 0, 0, c.width, c.height)
        val h = px.contentHashCode()
        ocrCache[key]?.let { if (it.first == h) return it.second }
        val t = Ocr.text(c)
        ocrCache[key] = Pair(h, t); return t
    }

    private fun tick() {
        val sz = screenSize()
        if (sz[0] != W || sz[1] != H) rebuildDisplay(sz)
        grab()
        val f = frame ?: return
        if (busy) return
        if (isBlank(f)) { show("Captura bloqueada", "O app de poker esta bloqueando a captura de tela (tela preta)"); return }
        val now = System.currentTimeMillis()
        fun rd(key: String): Read? {
            val bx = Cfg.get(this, key) ?: return null
            val r = cardReader.read(crop(f, bx))
            return if (r is Read.Ask && now < cooldownUntil) Read.Empty else r
        }
        val heroR = listOf("h1", "h2").map { rd(it) }
        heroR.firstOrNull { it is Read.Ask }?.let { askGlyph(it as Read.Ask); return }
        if (heroR.any { it !is Read.Ok }) {
            folded.clear(); lastKey = null; lastHeroKey = null; preMaxHeroBet = 0.0
            if (Cfg.get(this, "h1") == null || Cfg.get(this, "h2") == null) {
                show("Calibre as areas", "Toque em Calibrar (tela ${if (Cfg.land) "horizontal" else "vertical"})"); return
            }
            show("Aguardando cartas", "Simbolos aprendidos: ${glyphs.count()}/17"); return
        }
        val heroNames = heroR.map { (it as Read.Ok).name }
        val heroKey = heroNames.joinToString("")
        if (heroKey != lastHeroKey) { lastHeroKey = heroKey; preMaxHeroBet = 0.0; folded.clear() }
        val boardR = listOf("b1", "b2", "b3", "b4", "b5").map { rd(it) }
        boardR.firstOrNull { it is Read.Ask }?.let { askGlyph(it as Read.Ask); return }
        val boardNames = boardR.filterIsInstance<Read.Ok>().map { it.name }
        if (boardNames.size !in listOf(0, 3, 4, 5)) return

        val potRaw = Engine.parseNum(readText(f, "pot")) ?: 0.0
        val bb = Cfg.bb(this)
        val stackRead = Engine.parseNum(readText(f, "stack"))
        val stack = stackRead ?: (bb * 30)
        val heroBet = Engine.parseNum(readText(f, "hb")) ?: 0.0
        val bets = ArrayList<Double>(); var seats = 0
        for (i in 1..7) {
            if (Cfg.get(this, "s${i}b") == null && Cfg.get(this, "s${i}a") == null) continue
            seats++
            bets += Engine.parseNum(readText(f, "s${i}b")) ?: 0.0
            if (Engine.parseAction(readText(f, "s${i}a")) == "fold") folded.add(i)
        }
        val toCall = ((bets.maxOrNull() ?: 0.0) - heroBet).coerceAtLeast(0.0)
        val pot = if (Cfg.potIncl(this)) potRaw else potRaw + bets.sum() + heroBet
        val opps = maxOf(1, seats - folded.size)
        val stackBb = stack / maxOf(bb, 1.0)
        val phase = Cfg.phase(this)
        val pos = Cfg.pos(this)
        val openerSel = Cfg.opener(this)
        val profile = Cfg.profile(this)
        if (boardNames.isEmpty()) preMaxHeroBet = maxOf(preMaxHeroBet, heroBet)
        val role = if (boardNames.isEmpty()) 0 else if (preMaxHeroBet > bb * 1.5) 1 else 2
        val key = "$heroNames$boardNames$pot$toCall$heroBet$opps${stackBb.toInt()}$bb$phase$pos$openerSel$profile$role${Cfg.ante(this)}"
        if (key == lastKey) return
        lastKey = key
        val hero = heroNames.map { Engine.id(it!!)!! }.toIntArray()
        val board = boardNames.mapNotNull { Engine.id(it) }.toIntArray()
        val adv = Engine.suggest(hero, board, opps, pot, toCall, heroBet, stack, bb, phase, pos, openerSel, profile, role, Cfg.ante(this))
        show(adv.action, "${heroNames.joinToString(" ")} | ${boardNames.joinToString(" ").ifEmpty { "-" }} | eq ${(adv.eq * 100).toInt()}% | " +
            "pot ${pot.toInt()} | pagar ${toCall.toInt()} | $opps adv | ${stackBb.toInt()} BB${if (stackRead == null) " (stack nao lido)" else ""}\n${adv.why}", adv.range)
    }

    override fun onDestroy() {
        exec.shutdownNow()
        try { wm.removeView(panel) } catch (_: Exception) {}
        vd?.release(); reader?.close(); mp?.stop()
        super.onDestroy()
    }
}
