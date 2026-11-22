package ec.jat.reproductor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter2
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Pantalla principal: el reproductor completo (menú, estéreo, letras, modo DJ, clasificar…)
 * leyendo la música del celular. Android Auto usa su propio motor (PlaybackService).
 */
class MainActivity : Activity() {

    companion object {
        private var actual: WeakReference<MainActivity>? = null

        /** Ordenes desde la notificación o desde Android Auto. */
        fun orden(o: String) {
            val a = actual?.get() ?: return
            a.main.post { a.web.evaluateJavascript("window.appOrden && appOrden('$o')", null) }
        }
    }

    val main = Handler(Looper.getMainLooper())
    lateinit var web: WebView
    private var elegirArchivo: ValueCallback<Array<Uri>>? = null
    private val REQ_ARCHIVO = 20
    private val REQ_PERMISO = 1

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actual = WeakReference(this)
        window.statusBarColor = Color.parseColor("#FBFBFC")
        window.navigationBarColor = Color.parseColor("#F2F3F6")
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        WebService.crearCanal(this)

        val cargador = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/musica/", WebViewAssetLoader.PathHandler { ruta -> abrirCancion(ruta) })
            .build()

        web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#FBFBFC"))
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                cargador.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // enlaces externos (por ejemplo acoustid.org) se abren en el navegador
                if (request.url.host != "appassets.androidplatform.net") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, request.url)) } catch (e: Exception) {}
                    return true
                }
                return false
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams
            ): Boolean {
                elegirArchivo?.onReceiveValue(null)
                elegirArchivo = filePathCallback
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    val tipos = fileChooserParams.acceptTypes.filter { it.isNotBlank() }
                    type = if (tipos.size == 1 && tipos[0].contains('/')) tipos[0] else "*/*"
                    if (fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                return try { startActivityForResult(i, REQ_ARCHIVO); true } catch (e: Exception) {
                    elegirArchivo = null; false
                }
            }
        }
        web.addJavascriptInterface(Puente(), "AndroidJAT")
        setContentView(web)
        web.loadUrl("https://appassets.androidplatform.net/assets/web/index.html")
        pedirPermisos()
        recibir(intent)
    }

    /** Canción abierta desde otra app ("Abrir con" → Reproductor JAT). */
    private var pendiente: String? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recibir(intent)
    }

    private fun recibir(i: Intent?) {
        if (i?.action != Intent.ACTION_VIEW) return
        val uri = i.data ?: return
        val ruta = rutaDe(uri)
        if (ruta == null) {
            Toast.makeText(this, "No encontré esa canción en la música del celular.", Toast.LENGTH_LONG).show()
            return
        }
        pendiente = ruta
        web.evaluateJavascript("window.revisarPendiente && revisarPendiente()", null)
    }

    /** Busca la canción en la música del celular y devuelve su ruta igual que en la lista ("Music/Telegram/x.mp3"). */
    private fun rutaDe(uri: Uri): String? {
        var nombre: String? = null
        var tam = -1L
        try {
            if (uri.scheme == "file") {
                val f = java.io.File(uri.path ?: return null)
                nombre = f.name; tam = f.length()
            } else {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        nombre = c.getString(0)
                        if (!c.isNull(1)) tam = c.getLong(1)
                    }
                }
            }
        } catch (e: Exception) {}
        val n = nombre ?: uri.lastPathSegment?.substringAfterLast('/') ?: return null
        val col = mutableListOf(MediaStore.Audio.Media.DISPLAY_NAME)
        if (Build.VERSION.SDK_INT >= 29) col.add(MediaStore.Audio.Media.RELATIVE_PATH) else col.add(MediaStore.Audio.Media.DATA)
        val sel = if (tam > 0) "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.SIZE} = ?" else "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val args = if (tam > 0) arrayOf(n, tam.toString()) else arrayOf(n)
        try {
            contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, col.toTypedArray(), sel, args, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nom = c.getString(0) ?: n
                    val carpeta = if (Build.VERSION.SDK_INT >= 29) (c.getString(1) ?: "").trimEnd('/')
                    else (c.getString(1) ?: "").substringBeforeLast('/').substringAfter("/0/")
                    return if (carpeta.isEmpty()) nom else "$carpeta/$nom"
                }
            }
        } catch (e: Exception) {}
        return null
    }

    // Atrás: primero cierra lo que esté abierto en la pantalla (reproductor, menú, ajustes).
    // Si no hay nada abierto, la app se va al fondo sin pausar: la música sigue sonando.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        web.evaluateJavascript("window.atras ? atras() : false") { r -> if (r != "true") moveTaskToBack(true) }
    }

    override fun onDestroy() {
        if (actual?.get() === this) actual = null
        WebService.detener(this)
        web.destroy()
        super.onDestroy()
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
        web.evaluateJavascript("window.sincronizarApp && sincronizarApp()", null)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_ARCHIVO) return
        val cb = elegirArchivo ?: return
        elegirArchivo = null
        if (resultCode != RESULT_OK || data == null) { cb.onReceiveValue(null); return }
        val uris = mutableListOf<Uri>()
        data.clipData?.let { cd -> for (k in 0 until cd.itemCount) uris.add(cd.getItemAt(k).uri) }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        cb.onReceiveValue(uris.toTypedArray())
    }

    /** Entrega el audio de una canción del celular a la página (/musica/<id>). */
    private fun abrirCancion(ruta: String): WebResourceResponse? {
        val id = ruta.trim('/').toLongOrNull() ?: return null
        return try {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            val tipo = contentResolver.getType(uri) ?: "audio/mpeg"
            WebResourceResponse(tipo, null, contentResolver.openInputStream(uri))
        } catch (e: Exception) { null }
    }

    /** Lo que la página puede pedirle a la app. */
    inner class Puente {

        @JavascriptInterface
        fun canciones(): String {
            if (!Biblioteca.tienePermiso(this@MainActivity)) return "{\"error\":\"permiso\"}"
            val arr = JSONArray()
            val col = mutableListOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= 29) col.add(MediaStore.Audio.Media.RELATIVE_PATH) else col.add(MediaStore.Audio.Media.DATA)
            try {
                contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, col.toTypedArray(),
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val nombre = c.getString(1) ?: continue
                        val carpeta = if (Build.VERSION.SDK_INT >= 29) (c.getString(2) ?: "").trimEnd('/')
                        else (c.getString(2) ?: "").substringBeforeLast('/').substringAfter("/0/")
                        arr.put(JSONObject().put("id", id).put("ruta", if (carpeta.isEmpty()) nombre else "$carpeta/$nombre"))
                    }
                }
            } catch (e: Exception) {}
            return arr.toString()
        }

        @JavascriptInterface
        fun pedirPermiso() { main.post { pedirPermisos() } }

        /** Canción que llegó desde "Abrir con" (se entrega una sola vez). */
        @JavascriptInterface
        fun pendiente(): String {
            val p = pendiente ?: ""
            pendiente = null
            return p
        }

        /** Cada cambio del reproductor: pasa géneros/ritmos a Android Auto y deja respaldo en Descargas. */
        @JavascriptInterface
        fun sincronizar(json: String) { Respaldo.sincronizarWeb(this@MainActivity, json) }

        @JavascriptInterface
        fun guardarRespaldo(nombre: String, json: String) {
            val ok = Respaldo.guardarEnDescargas(this@MainActivity, nombre, json)
            main.post {
                Toast.makeText(
                    this@MainActivity,
                    if (ok) "✔ Respaldo guardado en Descargas/ReproductorJAT" else "⚠ No se pudo guardar el respaldo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        @JavascriptInterface
        fun estado(titulo: String, artista: String, sonando: Boolean) {
            main.post {
                if (sonando) PlaybackService.pausarSiSuena()   // que no suenen los dos motores a la vez
                WebService.actualizar(this@MainActivity, titulo, artista, sonando)
            }
        }

        /** Color de la app elegido (azul marino o rojo): lo usa también la pantalla de Ajustes de Android Auto. */
        @JavascriptInterface
        fun ponerTema(color: String) {
            getSharedPreferences("reproductor", MODE_PRIVATE).edit().putString("colorTema", color).apply()
        }

        /** Color de la barra de arriba del celular (el del fondo del reproductor). */
        @JavascriptInterface
        fun colorBarra(color: String, oscuro: Boolean) {
            main.post {
                try {
                    window.statusBarColor = Color.parseColor(color)
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = if (oscuro) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } catch (e: Exception) {}
            }
        }

        @JavascriptInterface
        fun abrirAjustes() { main.post { startActivity(Intent(this@MainActivity, AjustesActivity::class.java)) } }

        /** Dónde está sonando ahora: parlante Bluetooth, audífonos o el celular. */
        @JavascriptInterface
        fun salidaAudio(): String {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            val devs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            fun nombre(d: AudioDeviceInfo) = d.productName?.toString()?.takeIf { it.isNotBlank() && it != Build.MODEL } ?: ""
            val bt = devs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            val o = JSONObject()
            when {
                bt != null -> { o.put("tipo", "bluetooth"); o.put("nombre", nombre(bt).ifEmpty { "Bluetooth" }) }
                devs.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_HEADSET } ->
                    { o.put("tipo", "audifonos"); o.put("nombre", "Audífonos con cable") }
                else -> { o.put("tipo", "celular"); o.put("nombre", "Este celular") }
            }
            return o.toString()
        }

        /** Abre la ventanita del sistema para elegir dónde suena (celular, parlante o audífonos Bluetooth…). */
        @JavascriptInterface
        fun elegirSalida() { main.post { abrirSelectorSalida() } }

        /** Abre los ajustes de Bluetooth del celular para emparejar o conectar otro aparato. */
        @JavascriptInterface
        fun ajustesBluetooth() {
            main.post {
                try { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                catch (e: Exception) { Toast.makeText(this@MainActivity, "No se pudo abrir el Bluetooth", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // Selector de salida de audio: se prueba lo que tenga cada versión de Android (y de MIUI);
    // si nada funciona, se abren los ajustes de Bluetooth.
    private fun abrirSelectorSalida() {
        if (Build.VERSION.SDK_INT >= 34) {
            try { if (MediaRouter2.getInstance(this).showSystemOutputSwitcher()) return } catch (e: Exception) {}
        }
        val panel = Intent("com.android.settings.panel.action.MEDIA_OUTPUT")
            .putExtra("com.android.settings.panel.extra.PACKAGE_NAME", packageName)
        if (Build.VERSION.SDK_INT <= 30) {
            try { if (panel.resolveActivity(packageManager) != null) { startActivity(panel); return } } catch (e: Exception) {}
        }
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                val i = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
                    .setPackage("com.android.systemui").putExtra("package_name", packageName)
                if (packageManager.queryBroadcastReceivers(i, 0).isNotEmpty()) { sendBroadcast(i); return }
            } catch (e: Exception) {}
            try { if (panel.resolveActivity(packageManager) != null) { startActivity(panel); return } } catch (e: Exception) {}
        }
        try { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        catch (e: Exception) { Toast.makeText(this, "No se pudo abrir el selector de sonido", Toast.LENGTH_SHORT).show() }
    }
}
