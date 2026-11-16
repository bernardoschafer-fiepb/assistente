package com.estudo.pokerassistant

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Tela inicial: pede as permissoes e liga o assistente (janela flutuante + captura de tela). */
class MainActivity : Activity() {
    private val reqCapture = 1001
    private val reqNotif = 1002
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        val title = TextView(this).apply { text = "Assistente de Poker (estudo)"; textSize = 22f }
        val help = TextView(this).apply {
            textSize = 15f; setPadding(0, pad, 0, pad)
            text = "Como usar:\n" +
                "1) Permita a janela flutuante.\n" +
                "2) Toque em Iniciar e aceite a captura de tela.\n" +
                "3) Abra o app de poker NA ORIENTACAO em que vai jogar.\n" +
                "4) No painel flutuante, toque em Calibrar e desenhe as areas pedidas " +
                "(cartas, pot, stack, apostas). Cada orientacao tem a sua calibragem.\n" +
                "5) Na primeira vez o app pergunta o valor/naipe de cada simbolo novo (17 no total).\n" +
                "6) Ajuste Pos, Fase, Stack, Ante e BB (botoes BB/2 e BBx2). " +
                "Se a sugestao parecer errada em pot odds, teste o botao Pot (c/ apostas ou s/ apostas).\n\n" +
                "Uso apenas para estudo, em mesa onde todos sabem que o assistente esta sendo usado."
        }
        status = TextView(this).apply { textSize = 15f; setPadding(0, 0, 0, pad) }
        root.addView(title); root.addView(help); root.addView(status)
        root.addView(button("1) Permitir janela flutuante") { openOverlaySettings() })
        root.addView(button("2) Iniciar assistente") { startAssistant() })
        root.addView(button("Parar assistente") { stopService(Intent(this, CaptureService::class.java)) })
        setContentView(ScrollView(this).apply { addView(root) })

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), reqNotif)
        }
    }

    override fun onResume() {
        super.onResume()
        status.text = if (Settings.canDrawOverlays(this)) "Janela flutuante: permitida" else "Janela flutuante: FALTA permitir"
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { onClick() }
    }

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    @Suppress("DEPRECATION")
    private fun startAssistant() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permita a janela flutuante primeiro", Toast.LENGTH_LONG).show()
            openOverlaySettings(); return
        }
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), reqCapture)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != reqCapture) return
        if (resultCode == RESULT_OK && data != null) {
            val i = Intent(this, CaptureService::class.java).putExtra("code", resultCode).putExtra("data", data)
            startForegroundService(i)
            moveTaskToBack(true)
        } else {
            Toast.makeText(this, "Captura de tela nao autorizada", Toast.LENGTH_LONG).show()
        }
    }
}
