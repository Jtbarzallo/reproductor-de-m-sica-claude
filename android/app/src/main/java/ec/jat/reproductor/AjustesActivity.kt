package ec.jat.reproductor

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AjustesActivity : Activity() {

    private val FONDO = Color.parseColor("#0F1220")
    private val TARJETA = Color.parseColor("#1A1F35")
    private val ACENTO = Color.parseColor("#4FD1C5")
    private val TEXTO = Color.parseColor("#EEF1FF")
    private val GRIS = Color.parseColor("#9AA3C7")

    private val main = Handler(Looper.getMainLooper())
    private val trabajo = Executors.newSingleThreadExecutor()
    private val enMain = Executor { main.post(it) }
    private lateinit var cont: LinearLayout
    private var futuro: ListenableFuture<MediaController>? = null
    private var control: MediaController? = null
    private var txtSonando: TextView? = null
    private var btnPlay: Button? = null

    private val REQ_PERMISO = 1
    private val REQ_IMPORTAR = 10
    private val REQ_EXPORTAR = 11

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = FONDO
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(FONDO)
        scroll.fitsSystemWindows = true
        cont = LinearLayout(this)
        cont.orientation = LinearLayout.VERTICAL
        cont.setPadding(dp(14), dp(14), dp(14), dp(40))
        scroll.addView(cont)
        setContentView(scroll)
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val f = MediaController.Builder(this, token).buildAsync()
        futuro = f
        f.addListener({
            try {
                control = f.get()
                control?.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) { pintarSonando() }
                })
                pintarSonando()
            } catch (e: Exception) {}
        }, enMain)
    }

    override fun onStop() {
        futuro?.let { MediaController.releaseFuture(it) }
        control = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        Biblioteca.sucia = true
        dibujar()
    }

    private fun pedirPermisos() {
        val faltan = mutableListOf<String>()
        val audio = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(audio) != PackageManager.PERMISSION_GRANTED) faltan.add(audio)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            faltan.add(Manifest.permission.POST_NOTIFICATIONS)
        if (faltan.isNotEmpty()) requestPermissions(faltan.toTypedArray(), REQ_PERMISO)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Biblioteca.sucia = true
        dibujar()
    }

    // ------------------------------------------------------------------ pantalla

    private fun dibujar() {
        trabajo.execute {
            val permiso = Biblioteca.tienePermiso(this)
            val canciones = if (permiso) Biblioteca.canciones(this) else emptyList()
            val conGenero = canciones.count { it.genero.isNotEmpty() }
            val conRitmo = canciones.count { it.bpm > 0 }
            main.post { if (!isFinishing) pintar(permiso, canciones.size, conGenero, conRitmo) }
        }
    }

    private fun pintar(permiso: Boolean, total: Int, conGenero: Int, conRitmo: Int) {
        cont.removeAllViews()
        cont.addView(texto("⚙ Android Auto y ajustes", 22f, ACENTO, true))
        cont.addView(texto("Lo que suena en el carro", 14f, GRIS).apply { setPadding(0, 0, 0, dp(10)) })

        if (!permiso) {
            val c = tarjeta()
            c.addView(texto("Paso 1: dar permiso", 18f, TEXTO, true))
            c.addView(texto("La app necesita permiso para leer la música del celular.", 15f, TEXTO))
            c.addView(boton("Dar permiso") { pedirPermisos() })
            c.addView(boton("Abrir ajustes de la app", secundario = true) {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:$packageName")))
            })
            return
        }

        // --- lo que suena ---
        val s = tarjeta()
        txtSonando = texto("", 17f, TEXTO, true)
        s.addView(txtSonando)
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        fila.addView(botonChico("⏮") { control?.seekToPreviousMediaItem() })
        btnPlay = botonChico("▶") {
            val c = control ?: return@botonChico
            if (c.mediaItemCount == 0) mezclarTodo() else if (c.isPlaying) c.pause() else c.play()
        }
        fila.addView(btnPlay)
        fila.addView(botonChico("⏭") { control?.seekToNextMediaItem() })
        s.addView(fila)
        s.addView(texto("(Esto es lo que suena en Android Auto)", 13f, GRIS))
        s.addView(boton("🔀 Mezclar toda mi música") { mezclarTodo() })
        pintarSonando()

        // --- biblioteca ---
        val b = tarjeta()
        b.addView(texto("📁 Tu música", 18f, TEXTO, true))
        b.addView(texto(
            "$total canciones en el celular.\n$conGenero con género · $conRitmo con ritmo.\n" +
                "Las canciones nuevas que pongas en el celular aparecen solas.", 15f, TEXTO
        ))
        b.addView(texto(
            "Los géneros, ritmos y títulos que clasifiques en el reproductor pasan solos al carro. " +
                "Si usaste el reproductor en el navegador, puedes traer su respaldo aquí.", 13f, GRIS
        ).apply { setPadding(0, dp(6), 0, 0) })
        b.addView(boton("📥 Importar respaldo del navegador", secundario = true) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            }, REQ_IMPORTAR)
        })

        // --- ajustes ---
        val a = tarjeta()
        a.addView(texto("🎚 En el carro", 18f, TEXTO, true))
        a.addView(Switch(this).apply {
            text = "Saltar silencios (inicio, final y huecos de las canciones)"
            setTextColor(TEXTO); textSize = 15f
            isChecked = Respaldo.saltarSilencios(this@AjustesActivity)
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, v -> Respaldo.setSaltarSilencios(this@AjustesActivity, v) }
        })
        a.addView(boton("💾 Guardar respaldo", secundario = true) {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "respaldo_reproductor_android.json")
            }, REQ_EXPORTAR)
        })
        a.addView(texto("Además se guarda solo una copia en Descargas/ReproductorJAT.", 13f, GRIS))

        // --- Android Auto ---
        val au = tarjeta()
        au.addView(texto("🚗 Usar en Android Auto", 18f, TEXTO, true))
        au.addView(texto(
            "Android Auto solo muestra apps de la Play Store, salvo que actives esto UNA vez:\n\n" +
                "1. En el celular abre la app Android Auto (Ajustes → Apps conectadas → Android Auto, o búscala en Ajustes).\n" +
                "2. Baja hasta \"Versión\" y tócala 10 veces seguidas. Acepta el aviso de modo desarrollador.\n" +
                "3. Toca los ⋮ de arriba → \"Configuración para desarrolladores\".\n" +
                "4. Activa \"Fuentes desconocidas\" (Unknown sources).\n" +
                "5. Desconecta y vuelve a conectar el celular al carro.\n" +
                "6. En la pantalla del carro toca el ícono de música / apps y elige \"Reproductor JAT\".\n" +
                "   Si no aparece: en el carro, en la lista de apps → Personalizar, y actívala.",
            14f, TEXTO
        ))
        au.addView(boton("Abrir ajustes de Android Auto", secundario = true) { abrirAndroidAuto() })
        au.addView(texto(
            "En el carro verás: Todas · Géneros · Ritmo · Más (carpetas, artistas, álbumes, años). " +
                "También puedes decir \"Ok Google, pon salsa en Reproductor JAT\".", 13f, GRIS
        ).apply { setPadding(0, dp(6), 0, 0) })
    }

    private fun mezclarTodo() {
        val c = control ?: return
        val item = MediaItem.Builder().setMediaId(Biblioteca.idTodoContexto("aleatorio")).build()
        c.setMediaItem(item)
        c.prepare(); c.play()
    }

    private fun pintarSonando() {
        val c = control
        val t = txtSonando ?: return
        val m = c?.mediaMetadata
        t.text = if (c == null || c.mediaItemCount == 0) "Nada sonando"
        else listOfNotNull(m?.title?.toString(), m?.artist?.toString()).joinToString(" — ").ifEmpty { "Sonando" }
        btnPlay?.text = if (c?.isPlaying == true) "⏸" else "▶"
    }

    private fun abrirAndroidAuto() {
        val intentos = listOf(
            Intent().setComponent(ComponentName("com.google.android.projection.gearhead",
                "com.google.android.apps.auto.carservice.service.impl.CarSettingsActivity")),
            packageManager.getLaunchIntentForPackage("com.google.android.projection.gearhead"),
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:com.google.android.projection.gearhead")),
        )
        for (i in intentos) {
            if (i == null) continue
            try { startActivity(i); return } catch (e: Exception) {}
        }
        Toast.makeText(this, "No encontré Android Auto en este celular", Toast.LENGTH_LONG).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK) return
        trabajo.execute {
            val msg = try {
                when (requestCode) {
                    REQ_IMPORTAR -> "✔ Respaldo importado: ${Respaldo.importar(this, uri)} canciones con datos"
                    REQ_EXPORTAR -> { Respaldo.exportar(this, uri); "✔ Respaldo guardado" }
                    else -> null
                }
            } catch (e: Exception) { "⚠ " + (e.message ?: "No se pudo") }
            main.post {
                if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                dibujar()
            }
        }
    }

    // ------------------------------------------------------------------ piezas

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun texto(t: String, tam: Float, color: Int, negrita: Boolean = false) = TextView(this).apply {
        text = t; textSize = tam; setTextColor(color)
        if (negrita) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(2), 0, dp(2))
    }

    private fun tarjeta(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(16), dp(14), dp(16), dp(14))
        c.background = GradientDrawable().apply { setColor(TARJETA); cornerRadius = dp(16).toFloat() }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, dp(6), 0, dp(6))
        cont.addView(c, lp)
        return c
    }

    private fun boton(t: String, secundario: Boolean = false, accion: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; textSize = 15f
        setTextColor(if (secundario) TEXTO else FONDO)
        background = GradientDrawable().apply {
            setColor(if (secundario) Color.parseColor("#263055") else ACENTO); cornerRadius = dp(12).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(10) }
        setOnClickListener { accion() }
    }

    private fun botonChico(t: String, accion: () -> Unit) = Button(this).apply {
        text = t; textSize = 22f; setTextColor(FONDO)
        background = GradientDrawable().apply { setColor(ACENTO); cornerRadius = dp(30).toFloat() }
        layoutParams = LinearLayout.LayoutParams(dp(64), dp(60)).apply { setMargins(dp(8), dp(10), dp(8), 0) }
        setOnClickListener { accion() }
    }
}
