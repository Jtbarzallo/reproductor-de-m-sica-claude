package ec.jat.reproductor

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** El "motor" que suena en el celular y que Android Auto muestra en la pantalla del carro. */
class PlaybackService : MediaLibraryService() {

    companion object {
        private var instancia: PlaybackService? = null
        /** Si el motor de Android Auto está sonando, se pausa (para que no suenen los dos). */
        fun pausarSiSuena() {
            val s = instancia ?: return
            if (s::player.isInitialized && s.player.isPlaying) s.player.pause()
        }
    }

    private var sesion: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private val main = Handler(Looper.getMainLooper())
    private val busquedas = HashMap<String, List<MediaItem>>()

    private val CMD_ALEATORIO = SessionCommand("jat.ALEATORIO", Bundle.EMPTY)
    private val CMD_REPETIR = SessionCommand("jat.REPETIR", Bundle.EMPTY)

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
        if (k == "saltarSilencios") player.skipSilenceEnabled = Respaldo.saltarSilencios(this)
    }

    // ------------------------------------------------------------------ fundido entre canciones
    // Cuando falta poco para que acabe la canción, un segundo reproductor ("ayudante") empieza la
    // siguiente en silencio. Durante el fundido la que termina BAJA poco a poco y la nueva SUBE poco a
    // poco hasta igualarse. Al final, el reproductor principal pasa a la canción nueva justo en el punto
    // donde va el ayudante, y el ayudante se apaga sin que se note.
    private lateinit var ayudante: ExoPlayer
    private enum class Fase { NADA, PREPARANDO, FUNDIENDO, TRASPASO }
    private var fase = Fase.NADA
    private var indiceNuevo = -1
    private var inicioFundido = 0L
    private var largoFundido = 0L
    private var inicioTraspaso = 0L
    private val PASO = 50L

    private val vigilante = object : Runnable {
        override fun run() {
            try { pasoFundido() } catch (e: Exception) { cancelarFundido() }
            main.postDelayed(this, if (fase == Fase.NADA) 250L else PASO)
        }
    }

    private fun pasoFundido() {
        val seg = Respaldo.fundido(this)
        when (fase) {
            Fase.NADA -> {
                if (seg <= 0 || !player.isPlaying || player.repeatMode == Player.REPEAT_MODE_ONE) return
                if (!player.hasNextMediaItem()) return
                val dur = player.duration
                if (dur == C.TIME_UNSET || dur < 20_000) return
                val falta = dur - player.currentPosition
                val quiero = seg * 1000L
                if (falta > quiero + 1500 || falta < 1500) return
                indiceNuevo = player.nextMediaItemIndex
                if (indiceNuevo == C.INDEX_UNSET) return
                ayudante.setMediaItem(player.getMediaItemAt(indiceNuevo))
                ayudante.volume = 0f
                ayudante.skipSilenceEnabled = player.skipSilenceEnabled
                ayudante.prepare()
                ayudante.playWhenReady = true
                fase = Fase.PREPARANDO
            }
            Fase.PREPARANDO -> {
                if (!player.isPlaying) { cancelarFundido(); return }
                if (ayudante.isPlaying) {
                    // el fundido dura lo elegido, pero termina antes de que se acabe la canción
                    val falta = player.duration - player.currentPosition
                    largoFundido = minOf(seg * 1000L, falta - 400).coerceAtLeast(1000L)
                    inicioFundido = System.currentTimeMillis()
                    fase = Fase.FUNDIENDO
                } else if (player.duration - player.currentPosition < 600) {
                    cancelarFundido()   // la siguiente no alcanzó a cargar: pasa normal
                }
            }
            Fase.FUNDIENDO -> {
                if (!player.isPlaying) { cancelarFundido(); return }
                val x = ((System.currentTimeMillis() - inicioFundido).toFloat() / largoFundido).coerceIn(0f, 1f)
                // curva de "potencia igual": el volumen total se mantiene parejo, sin bajón en el medio
                player.volume = Math.cos(x * Math.PI / 2).toFloat()
                ayudante.volume = Math.sin(x * Math.PI / 2).toFloat()
                if (x >= 1f) traspasar()
            }
            Fase.TRASPASO -> {
                // el principal ya está en la canción nueva (en silencio); cuando suena, se cambia en 0,4 s
                if (player.isPlaying && player.playbackState == Player.STATE_READY) {
                    val x = ((System.currentTimeMillis() - inicioTraspaso).toFloat() / 400f).coerceIn(0f, 1f)
                    player.volume = x
                    ayudante.volume = 1f - x
                    if (x >= 1f) terminarFundido()
                } else {
                    inicioTraspaso = System.currentTimeMillis()
                    if (!player.playWhenReady) cancelarFundido()
                }
            }
        }
    }

    private fun traspasar() {
        fase = Fase.TRASPASO
        player.volume = 0f
        ayudante.volume = 1f
        player.seekTo(indiceNuevo, ayudante.currentPosition + 150)
        inicioTraspaso = System.currentTimeMillis()
    }

    private fun terminarFundido() {
        fase = Fase.NADA
        player.volume = 1f
        try { ayudante.stop(); ayudante.clearMediaItems() } catch (e: Exception) {}
        ayudante.volume = 0f
    }

    private fun cancelarFundido() {
        if (fase == Fase.NADA) return
        fase = Fase.NADA
        player.volume = 1f
        try { ayudante.stop(); ayudante.clearMediaItems() } catch (e: Exception) {}
    }

    // Si aparece música nueva en el celular, el menú se actualiza solo
    private val observador = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) {
            Biblioteca.sucia = true
            main.removeCallbacks(avisarCambios)
            main.postDelayed(avisarCambios, 3000)
        }
    }
    private val avisarCambios = Runnable {
        val s = sesion ?: return@Runnable
        (listOf(Biblioteca.RAIZ) + (Biblioteca.CATEGORIAS.keys + Biblioteca.SUBCATEGORIAS.keys).map { "cat:$it" })
            .forEach { s.notifyChildrenChanged(it, Int.MAX_VALUE, null) }
    }

    override fun onCreate() {
        super.onCreate()
        instancia = this
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.skipSilenceEnabled = Respaldo.saltarSilencios(this)
        // segundo reproductor solo para el fundido (no pide el audio del carro, va mezclado con el principal)
        ayudante = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                false
            )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        ayudante.volume = 0f
        main.post(vigilante)
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let { Respaldo.guardarUltima(this@PlaybackService, it.mediaId, 0) }
                // si cambia de canción por otra razón (botón siguiente, elegir otra), se corta el fundido
                if (fase == Fase.FUNDIENDO || fase == Fase.PREPARANDO) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && player.currentMediaItemIndex == indiceNuevo) traspasar()
                    else cancelarFundido()
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) guardarPosicion()
                else {
                    MainActivity.orden("pausa")   // si suena en el carro, se calla la pantalla principal
                    WebService.detener(this@PlaybackService)   // y se quita su sesión, para que el carro use esta (con la lista de música)
                }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { actualizarBotones() }
            override fun onRepeatModeChanged(repeatMode: Int) { actualizarBotones() }
        })
        getSharedPreferences("reproductor", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefsListener)

        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        sesion = MediaLibrarySession.Builder(this, player, Callback())
            .setSessionActivity(abrir)
            .setCustomLayout(botones())
            .build()

        try {
            contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observador)
        } catch (e: Exception) {}
    }

    private fun guardarPosicion() {
        val item = player.currentMediaItem ?: return
        Respaldo.guardarUltima(this, item.mediaId, player.currentPosition)
    }

    private fun botones(): ImmutableList<CommandButton> = ImmutableList.of(
        CommandButton.Builder()
            .setDisplayName(if (::player.isInitialized && player.shuffleModeEnabled) "Aleatorio: sí" else "Aleatorio: no")
            .setIconResId(R.drawable.ic_aleatorio)
            .setSessionCommand(CMD_ALEATORIO)
            .build(),
        CommandButton.Builder()
            .setDisplayName(
                if (::player.isInitialized) when (player.repeatMode) {
                    Player.REPEAT_MODE_ALL -> "Repetir: todo"
                    Player.REPEAT_MODE_ONE -> "Repetir: una"
                    else -> "Repetir: no"
                } else "Repetir: no"
            )
            .setIconResId(R.drawable.ic_repetir)
            .setSessionCommand(CMD_REPETIR)
            .build(),
    )

    private fun actualizarBotones() { sesion?.setCustomLayout(botones()) }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = sesion

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = sesion?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        if (instancia === this) instancia = null
        guardarPosicion()
        try { contentResolver.unregisterContentObserver(observador) } catch (e: Exception) {}
        getSharedPreferences("reproductor", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefsListener)
        main.removeCallbacks(vigilante)
        try { ayudante.release() } catch (e: Exception) {}
        sesion?.run {
            player.release()
            release()
        }
        sesion = null
        super.onDestroy()
    }

    // ------------------------------------------------------------------ menú y órdenes

    private inner class Callback : MediaLibrarySession.Callback {

        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            // Al conectar el carro: si la música está sonando en el celular, sigue en Android Auto
            // con la misma canción y en el mismo punto.
            if (esCarro(controller)) {
                WebService.enCarro = true
                val sonaba = WebService.estaSonando()
                main.post {
                    if (sonaba) pasarAlCarro()
                    // la sesión de la pantalla principal no tiene lista de música: se apaga para que Android Auto
                    // muestre la de este servicio (donde sí se puede escoger música)
                    WebService.detener(this@PlaybackService)
                }
            }
            val comandos = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(CMD_ALEATORIO).add(CMD_REPETIR).build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(comandos)
                .setCustomLayout(botones())
                .build()
        }

        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
            if (esCarro(controller)) WebService.enCarro = false
        }

        override fun onCustomCommand(
            session: MediaSession, controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand, args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_ALEATORIO.customAction -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                CMD_REPETIR.customAction -> player.repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
            actualizarBotones()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            if (params?.isRecent == true)    // Android Auto pide "lo último que sonó" para su tarjeta de seguir escuchando
                Futures.immediateFuture(LibraryResult.ofItem(Biblioteca.item(this@PlaybackService, Biblioteca.RECIENTES)!!, params))
            else Futures.immediateFuture(LibraryResult.ofItem(Biblioteca.item(this@PlaybackService, Biblioteca.RAIZ)!!, estiloLista()))

        override fun onGetItem(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = Biblioteca.item(this@PlaybackService, mediaId)
            return Futures.immediateFuture(
                if (item != null) LibraryResult.ofItem(item, null) else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
            parentId: String, page: Int, pageSize: Int, params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val todos = Biblioteca.hijos(this@PlaybackService, parentId)
            return Futures.immediateFuture(LibraryResult.ofItemList(pagina(todos, page, pageSize), params))
        }

        override fun onSearch(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            val r = Biblioteca.buscar(this@PlaybackService, query).map { Biblioteca.itemCancion(it, "todas") }
            busquedas[query] = r
            session.notifySearchResultChanged(browser, query, r.size, params)
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
            query: String, page: Int, pageSize: Int, params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val r = busquedas[query] ?: Biblioteca.buscar(this@PlaybackService, query).map { Biblioteca.itemCancion(it, "todas") }
            return Futures.immediateFuture(LibraryResult.ofItemList(pagina(r, page, pageSize), params))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val salida = mutableListOf<MediaItem>()
            mediaItems.forEach { mi ->
                val r = Biblioteca.resolver(this@PlaybackService, mi.mediaId)
                if (r != null) salida.add(r.first[r.second])
                else if (mi.localConfiguration != null) salida.add(mi)
            }
            return Futures.immediateFuture(salida)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession, controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val ctx = this@PlaybackService
            // Pedido por voz: "Ok Google, pon salsa en Reproductor JAT"
            if (mediaItems.size == 1) {
                val consulta = mediaItems[0].requestMetadata.searchQuery
                if (consulta != null) {
                    var lista = Biblioteca.buscar(ctx, consulta)
                    if (lista.isEmpty()) lista = Biblioteca.canciones(ctx)
                    val items = lista.map { Biblioteca.itemCancion(it, "todas") }
                    return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(items, 0, C.TIME_UNSET))
                }
                // Tocar una canción: suena su grupo entero desde esa canción
                val r = Biblioteca.resolver(ctx, mediaItems[0].mediaId)
                if (r != null) {
                    player.shuffleModeEnabled = r.third || player.shuffleModeEnabled && mediaItems[0].mediaId.startsWith("song:")
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(r.first, r.second, if (startPositionMs > 0) startPositionMs else C.TIME_UNSET)
                    )
                }
            }
            val salida = mediaItems.mapNotNull { mi ->
                Biblioteca.resolver(ctx, mi.mediaId)?.let { it.first[it.second] } ?: mi.takeIf { it.localConfiguration != null }
            }
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(salida, startIndex.coerceIn(0, maxOf(0, salida.size - 1)), startPositionMs))
        }

        // Al subir al carro o tocar ▶ en el auricular: sigue donde te quedaste
        override fun onPlaybackResumption(
            mediaSession: MediaSession, controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val ctx = this@PlaybackService
            val id = Respaldo.ultimaId(ctx)
            val r = id?.let { Biblioteca.resolver(ctx, it) } ?: Biblioteca.resolver(ctx, Biblioteca.idTodoContexto("aleatorio"))
            return if (r == null) Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0))
            else Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(r.first, r.second, if (id != null) Respaldo.ultimaPos(ctx) else 0L)
            )
        }
    }

    /** Pide a Android Auto que muestre las carpetas y canciones como lista (con su carátula). */
    private fun estiloLista(): LibraryParams = LibraryParams.Builder().setExtras(Bundle().apply {
        putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
        putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
        putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
        putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
    }).build()

    private fun esCarro(c: MediaSession.ControllerInfo) =
        c.packageName.contains("gearhead") || c.packageName.contains("android.car") || c.packageName.contains("automotive")

    private var ultimoPase = 0L
    private fun pasarAlCarro() {
        val ahora = SystemClock.elapsedRealtime()
        if (ahora - ultimoPase < 5000 || player.isPlaying) return
        ultimoPase = ahora
        val id = Respaldo.ultimaId(this) ?: return
        val r = try { Biblioteca.resolver(this, id) } catch (e: Exception) { null } ?: return
        MainActivity.orden("pausa")
        player.setMediaItems(r.first, r.second, Respaldo.ultimaPos(this))
        player.prepare()
        player.play()
    }

    private fun pagina(todos: List<MediaItem>, page: Int, pageSize: Int): ImmutableList<MediaItem> {
        if (pageSize <= 0 || pageSize == Int.MAX_VALUE) return ImmutableList.copyOf(todos)
        val desde = page.toLong() * pageSize
        if (desde >= todos.size) return ImmutableList.of()
        val hasta = minOf(todos.size.toLong(), desde + pageSize).toInt()
        return ImmutableList.copyOf(todos.subList(desde.toInt(), hasta))
    }
}
