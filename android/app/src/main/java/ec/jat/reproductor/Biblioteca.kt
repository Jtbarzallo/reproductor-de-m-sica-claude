package ec.jat.reproductor

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.Normalizer

/** Una canción del celular, con lo que se sabe de ella. */
data class Cancion(
    val id: Long,
    val uri: Uri,
    val nombreArchivo: String,
    val titulo: String,
    val artista: String,
    val album: String,
    val albumId: Long,
    val anio: String,
    val genero: String,
    val bpm: Int,
    val pulso: Int,
    val carpeta: String,
    val duracionMs: Long,
)

/**
 * Toda la música del celular (la que Android reconoce como música) + los datos
 * del respaldo del reproductor web (títulos, géneros, ritmo) cuando existen.
 */
object Biblioteca {

    @Volatile private var cache: List<Cancion>? = null
    @Volatile var sucia = true

    // ----- categorías del menú (en Android Auto se ven como pestañas) -----
    const val RAIZ = "raiz"
    val CATEGORIAS = linkedMapOf(
        "todas" to "Todas",
        "genero" to "Géneros",
        "ritmo" to "Ritmo",
        "mas" to "Más",
    )
    val SUBCATEGORIAS = linkedMapOf(
        "dj" to "🎚 Mezcla DJ",
        "carpeta" to "Carpetas",
        "artista" to "Artistas",
        "album" to "Álbumes",
        "anio" to "Años",
    )

    fun canciones(ctx: Context): List<Cancion> {
        val c = cache
        if (c != null && !sucia) return c
        val nueva = cargar(ctx)
        cache = nueva
        sucia = false
        return nueva
    }

    fun tienePermiso(ctx: Context): Boolean {
        val p = if (Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_AUDIO
        else android.Manifest.permission.READ_EXTERNAL_STORAGE
        return ctx.checkSelfPermission(p) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun cargar(ctx: Context): List<Cancion> {
        if (!tienePermiso(ctx)) return emptyList()
        val extra = Respaldo.etiquetasWeb(ctx)
        val lista = ArrayList<Cancion>()
        val col = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
        )
        if (Build.VERSION.SDK_INT >= 29) col.add(MediaStore.Audio.Media.RELATIVE_PATH)
        else col.add(MediaStore.Audio.Media.DATA)
        if (Build.VERSION.SDK_INT >= 30) col.add(MediaStore.Audio.Media.GENRE)

        val base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        try {
            ctx.contentResolver.query(
                base, col.toTypedArray(),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cur ->
                fun s(nombre: String): String {
                    val i = cur.getColumnIndex(nombre)
                    return if (i >= 0) (cur.getString(i) ?: "") else ""
                }
                fun l(nombre: String): Long {
                    val i = cur.getColumnIndex(nombre)
                    return if (i >= 0) cur.getLong(i) else 0L
                }
                while (cur.moveToNext()) {
                    val id = l(MediaStore.Audio.Media._ID)
                    val archivo = s(MediaStore.Audio.Media.DISPLAY_NAME)
                    val web = extra.optJSONObject(archivo)
                    fun w(k: String) = web?.optString(k, "")?.trim().orEmpty()

                    var artista = s(MediaStore.Audio.Media.ARTIST)
                    if (artista == "<unknown>") artista = ""
                    var album = s(MediaStore.Audio.Media.ALBUM)
                    val carpeta = if (Build.VERSION.SDK_INT >= 29) {
                        s(MediaStore.Audio.Media.RELATIVE_PATH).trimEnd('/')
                    } else {
                        s(MediaStore.Audio.Media.DATA).substringBeforeLast('/').substringAfter("/0/")
                    }
                    if (album.isBlank() || album == carpeta.substringAfterLast('/')) album = ""
                    val tituloMs = s(MediaStore.Audio.Media.TITLE)
                    val anioMs = l(MediaStore.Audio.Media.YEAR).let { if (it in 1000..2999) it.toString() else "" }
                    val generoMs = if (Build.VERSION.SDK_INT >= 30) s(MediaStore.Audio.Media.GENRE) else ""

                    lista.add(
                        Cancion(
                            id = id,
                            uri = ContentUris.withAppendedId(base, id),
                            nombreArchivo = archivo,
                            titulo = w("titulo").ifEmpty { tituloMs.ifEmpty { archivo.substringBeforeLast('.') } },
                            artista = w("artista").ifEmpty { artista },
                            album = w("album").ifEmpty { album },
                            albumId = l(MediaStore.Audio.Media.ALBUM_ID),
                            anio = w("anio").ifEmpty { anioMs },
                            genero = limpiarGenero(w("genero").ifEmpty { generoMs }),
                            bpm = web?.optInt("bpm", 0) ?: 0,
                            pulso = web?.optInt("pulso", 0) ?: 0,
                            carpeta = carpeta.ifEmpty { "Carpeta principal" },
                            duracionMs = l(MediaStore.Audio.Media.DURATION),
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
        return lista
    }

    private fun limpiarGenero(g: String): String {
        var t = g.trim()
        if (t.isEmpty()) return ""
        t = t.replace(Regex("^\\(\\d+\\)\\s*"), "").split(';', '/').first().trim()
        return t.replaceFirstChar { it.uppercase() }
    }

    fun ritmo(bpm: Int): String = when {
        bpm <= 0 -> "Sin analizar"
        bpm < 95 -> "Lento"
        bpm <= 125 -> "Moderado"
        else -> "Rápido"
    }

    // Mezcla DJ: canciones con golpe marcado y velocidad parecida (igual que en el reproductor del celular)
    private val TRAMOS_DJ = intArrayOf(85, 95, 105, 115, 125, 135, 150, 170)
    private fun tempoDJ(bpm: Int): Double { var b = bpm.toDouble(); while (b < 85) b *= 2; while (b >= 170) b /= 2; return b }
    private fun mezclable(c: Cancion) = c.bpm > 0 && c.pulso >= 35
    private fun grupoDJ(c: Cancion): String {
        if (c.bpm <= 0 || c.pulso <= 0) return "Sin analizar"
        if (!mezclable(c)) return "No mezclables"
        val b = tempoDJ(c.bpm)
        for (k in 0 until TRAMOS_DJ.size - 1) if (b < TRAMOS_DJ[k + 1]) return "${TRAMOS_DJ[k]}–${TRAMOS_DJ[k + 1]} BPM"
        return "No mezclables"
    }
    private fun numDJ(g: String): Int = g.takeWhile { it.isDigit() }.toIntOrNull() ?: if (g == "No mezclables") 1000 else 2000

    /** A qué grupo pertenece una canción dentro de una categoría. */
    fun grupoDe(c: Cancion, cat: String): String = when (cat) {
        "genero" -> c.genero.ifEmpty { "Sin género" }
        "ritmo" -> ritmo(c.bpm)
        "dj" -> grupoDJ(c)
        "artista" -> c.artista.ifEmpty { "Artista desconocido" }
        "album" -> c.album.ifEmpty { "Álbum desconocido" }
        "anio" -> c.anio.ifEmpty { "Sin año" }
        "carpeta" -> c.carpeta
        else -> ""
    }

    fun grupos(ctx: Context, cat: String): List<Pair<String, Int>> {
        val cuenta = LinkedHashMap<String, Int>()
        canciones(ctx).forEach { val g = grupoDe(it, cat); cuenta[g] = (cuenta[g] ?: 0) + 1 }
        val ordenRitmo = listOf("Lento", "Moderado", "Rápido", "Sin analizar")
        return cuenta.entries.map { it.key to it.value }.sortedWith(
            if (cat == "dj") compareBy<Pair<String, Int>> { numDJ(it.first) }
            else if (cat == "ritmo") compareBy<Pair<String, Int>> { ordenRitmo.indexOf(it.first) }
            else compareBy<Pair<String, Int>>({ it.first.startsWith("Sin ") || it.first.endsWith("desconocido") }, { quitarTildes(it.first) })
        )
    }

    /** Canciones de un contexto: "todas" o "cat|grupo". */
    fun cancionesDe(ctx: Context, contexto: String): List<Cancion> {
        val todas = canciones(ctx)
        if (contexto == "todas" || contexto.isEmpty()) return todas
        val cat = contexto.substringBefore('|')
        val grupo = contexto.substringAfter('|')
        val filtradas = todas.filter { grupoDe(it, cat) == grupo }
        return if (cat == "dj") filtradas.sortedBy { if (mezclable(it)) tempoDJ(it.bpm) else 1000.0 } else filtradas
    }

    fun buscar(ctx: Context, texto: String): List<Cancion> {
        val q = quitarTildes(texto).trim()
        if (q.isEmpty()) return canciones(ctx)
        val palabras = q.split(Regex("\\s+"))
        return canciones(ctx).filter { c ->
            val todo = quitarTildes("${c.titulo} ${c.artista} ${c.album} ${c.genero} ${ritmo(c.bpm)} ${c.nombreArchivo}")
            palabras.all { todo.contains(it) }
        }
    }

    private fun quitarTildes(s: String) =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    // ----- identificadores que usa Android Auto -----
    fun cod(s: String): String = URLEncoder.encode(s, "UTF-8")
    fun dec(s: String): String = URLDecoder.decode(s, "UTF-8")

    fun idGrupo(cat: String, grupo: String) = "grp:$cat:${cod(grupo)}"
    fun idCancion(contexto: String, id: Long) = "song:${cod(contexto)}:$id"
    fun idTodoContexto(contexto: String) = "all:${cod(contexto)}"

    fun artUri(c: Cancion): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), c.albumId)

    fun itemCarpeta(id: String, titulo: String, sub: String?, reproducible: Boolean): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(titulo)
                    .setSubtitle(sub)
                    .setIsBrowsable(true)
                    .setIsPlayable(reproducible)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            ).build()

    fun itemAccion(id: String, titulo: String, sub: String?): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(titulo)
                    .setSubtitle(sub)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .build()
            ).build()

    fun itemCancion(c: Cancion, contexto: String): MediaItem {
        val sub = listOf(c.artista, c.album).filter { it.isNotEmpty() }.joinToString(" · ")
        return MediaItem.Builder()
            .setMediaId(idCancion(contexto, c.id))
            .setUri(c.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(c.titulo)
                    .setArtist(c.artista.ifEmpty { null })
                    .setAlbumTitle(c.album.ifEmpty { null })
                    .setSubtitle(sub.ifEmpty { null })
                    .setGenre(c.genero.ifEmpty { null })
                    .setArtworkUri(if (c.albumId > 0) artUri(c) else null)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            ).build()
    }

    /** Hijos de un nodo del menú. */
    fun hijos(ctx: Context, padre: String): List<MediaItem> {
        val total = canciones(ctx).size
        return when {
            padre == RAIZ -> CATEGORIAS.map { (k, v) -> itemCarpeta("cat:$k", v, null, false) }
            padre == "cat:todas" -> {
                if (total == 0) listOf(itemAccion("vacio", "No hay música", "Abre Reproductor JAT en el celular y da permiso"))
                else listOf(
                    itemAccion(idTodoContexto("aleatorio"), "🔀 Mezclar todo", "$total canciones"),
                ) + canciones(ctx).map { itemCancion(it, "todas") }
            }
            padre == "cat:mas" -> SUBCATEGORIAS.map { (k, v) -> itemCarpeta("cat:$k", v, null, false) }
            padre.startsWith("cat:") -> {
                val cat = padre.removePrefix("cat:")
                grupos(ctx, cat).map { (g, n) ->
                    val nombre = if (cat == "ritmo") (mapOf("Lento" to "🐢 ", "Moderado" to "🚶 ", "Rápido" to "🔥 ")[g] ?: "") + g else g
                    itemCarpeta(idGrupo(cat, g), nombre, "$n canciones", true)
                }
            }
            padre.startsWith("grp:") -> {
                val cat = padre.split(':')[1]
                val grupo = dec(padre.split(':')[2])
                val contexto = "$cat|$grupo"
                val lista = cancionesDe(ctx, contexto)
                listOf(itemAccion(idTodoContexto("$contexto|aleatorio"), "🔀 Mezclar este grupo", "${lista.size} canciones")) +
                    lista.map { itemCancion(it, contexto) }
            }
            else -> emptyList()
        }
    }

    /** Lista para reproducir y en qué posición empezar, a partir de lo que tocó el usuario. */
    fun resolver(ctx: Context, mediaId: String): Triple<List<MediaItem>, Int, Boolean>? {
        // devuelve (cola, inicio, aleatorio)
        return when {
            mediaId.startsWith("song:") -> {
                val partes = mediaId.split(':')
                val contexto = dec(partes[1])
                val id = partes[2].toLongOrNull() ?: return null
                val lista = cancionesDe(ctx, contexto)
                val i = lista.indexOfFirst { it.id == id }
                if (i < 0) {
                    val c = canciones(ctx).firstOrNull { it.id == id } ?: return null
                    Triple(listOf(itemCancion(c, "todas")), 0, false)
                } else Triple(lista.map { itemCancion(it, contexto) }, i, false)
            }
            mediaId.startsWith("all:") -> {
                var contexto = dec(mediaId.removePrefix("all:"))
                var mezclar = false
                if (contexto == "aleatorio") { contexto = "todas"; mezclar = true }
                if (contexto.endsWith("|aleatorio")) { contexto = contexto.removeSuffix("|aleatorio"); mezclar = true }
                val lista = cancionesDe(ctx, contexto)
                if (lista.isEmpty()) null
                else Triple(lista.map { itemCancion(it, contexto) }, if (mezclar) lista.indices.random() else 0, mezclar)
            }
            mediaId.startsWith("grp:") -> {
                val cat = mediaId.split(':')[1]
                val grupo = dec(mediaId.split(':')[2])
                val contexto = "$cat|$grupo"
                val lista = cancionesDe(ctx, contexto)
                if (lista.isEmpty()) null else Triple(lista.map { itemCancion(it, contexto) }, 0, false)
            }
            else -> null
        }
    }

    fun item(ctx: Context, mediaId: String): MediaItem? {
        if (mediaId == RAIZ) return itemCarpeta(RAIZ, "Reproductor JAT", null, false)
        if (mediaId.startsWith("song:")) {
            val r = resolver(ctx, mediaId) ?: return null
            return r.first[r.second]
        }
        if (mediaId.startsWith("cat:")) {
            val k = mediaId.removePrefix("cat:")
            return itemCarpeta(mediaId, CATEGORIAS[k] ?: SUBCATEGORIAS[k] ?: k, null, false)
        }
        if (mediaId.startsWith("grp:")) return itemCarpeta(mediaId, dec(mediaId.split(':')[2]), null, true)
        return null
    }

    fun etiquetasActuales(ctx: Context): JSONObject = Respaldo.etiquetasWeb(ctx)
}
