package ec.jat.reproductor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
        window.statusBarColor = Color.parseColor("#0F1220")
        window.navigationBarColor = Color.parseColor("#0F1220")
        WebService.crearCanal(this)

        val cargador = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/musica/", WebViewAssetLoader.PathHandler { ruta -> abrirCancion(ruta) })
            .build()

        web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#0F1220"))
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
    }

    // No se pausa el WebView al salir de la app: la música sigue sonando.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { moveTaskToBack(true) }

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

        @JavascriptInterface
        fun abrirAjustes() { main.post { startActivity(Intent(this@MainActivity, AjustesActivity::class.java)) } }
    }
}
