package ec.jat.reproductor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File

/**
 * Ajustes y respaldo.
 * Regla JAT: todo lo que guarda la app queda también en un archivo que sobrevive si se
 * desinstala la app: Descargas/ReproductorJAT/respaldo_reproductor_android.json
 */
object Respaldo {

    private const val PREFS = "reproductor"
    private const val ARCHIVO_WEB = "respaldo_web.json"
    const val NOMBRE_PUBLICO = "respaldo_reproductor_android.json"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- ajustes ----------
    fun saltarSilencios(ctx: Context) = prefs(ctx).getBoolean("saltarSilencios", false)
    fun setSaltarSilencios(ctx: Context, v: Boolean) {
        prefs(ctx).edit().putBoolean("saltarSilencios", v).apply()
        guardarPublico(ctx)
    }

    fun guardarUltima(ctx: Context, mediaId: String, posMs: Long) {
        prefs(ctx).edit().putString("ultimaId", mediaId).putLong("ultimaPos", posMs).apply()
    }
    fun ultimaId(ctx: Context): String? = prefs(ctx).getString("ultimaId", null)
    fun ultimaPos(ctx: Context): Long = prefs(ctx).getLong("ultimaPos", 0L)

    // ---------- etiquetas del reproductor web (títulos, géneros, ritmo) ----------
    fun etiquetasWeb(ctx: Context): JSONObject = try {
        val f = File(ctx.filesDir, ARCHIVO_WEB)
        if (f.exists()) JSONObject(f.readText()).optJSONObject("etiquetas") ?: JSONObject() else JSONObject()
    } catch (e: Exception) { JSONObject() }

    /** Importa un respaldo (del reproductor web o de esta app). Devuelve cuántas canciones trae. */
    fun importar(ctx: Context, uri: Uri): Int {
        val texto = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("No se pudo leer el archivo")
        val json = JSONObject(texto)
        val etiquetas = json.optJSONObject("etiquetas") ?: throw IllegalArgumentException("No es un respaldo del reproductor")
        val previo = etiquetasWeb(ctx)
        // se juntan: lo nuevo manda, lo anterior se conserva
        val keys = etiquetas.keys()
        while (keys.hasNext()) { val k = keys.next(); previo.put(k, etiquetas.get(k)) }
        File(ctx.filesDir, ARCHIVO_WEB).writeText(JSONObject().put("etiquetas", previo).toString())
        json.optJSONObject("ajustesAndroid")?.let {
            prefs(ctx).edit().putBoolean("saltarSilencios", it.optBoolean("saltarSilencios", saltarSilencios(ctx))).apply()
        }
        Biblioteca.sucia = true
        guardarPublico(ctx)
        return etiquetas.length()
    }

    fun contenido(ctx: Context): String = JSONObject()
        .put("app", "Reproductor JAT Android")
        .put("version", 1)
        .put("fecha", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date()))
        .put("etiquetas", etiquetasWeb(ctx))
        .put("ajustesAndroid", JSONObject().put("saltarSilencios", saltarSilencios(ctx)))
        .toString(2)

    fun exportar(ctx: Context, uri: Uri) {
        ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(contenido(ctx).toByteArray()) }
    }

    /** Copia automática en Descargas/ReproductorJAT (queda aunque se desinstale la app). */
    fun guardarPublico(ctx: Context) {
        if (Build.VERSION.SDK_INT < 29) return
        try {
            val datos = contenido(ctx).toByteArray()
            val guardada = prefs(ctx).getString("uriPublico", null)?.let { Uri.parse(it) }
            if (guardada != null) {
                try {
                    ctx.contentResolver.openOutputStream(guardada, "wt")?.use { it.write(datos) }
                    return
                } catch (e: Exception) { /* la borraron: se crea otra */ }
            }
            val v = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, NOMBRE_PUBLICO)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/ReproductorJAT")
            }
            val nueva = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return
            ctx.contentResolver.openOutputStream(nueva, "wt")?.use { it.write(datos) }
            prefs(ctx).edit().putString("uriPublico", nueva.toString()).apply()
        } catch (e: Exception) {
        }
    }
}
