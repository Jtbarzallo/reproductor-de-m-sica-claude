package ec.jat.reproductor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mantiene viva la app mientras suena música desde la pantalla principal, muestra la notificación
 * con ⏮ ⏯ ⏭ y publica una "sesión multimedia" del sistema: así la canción aparece en el centro de
 * control, en la pantalla bloqueada, en el radio del carro por Bluetooth y responde a los botones
 * de los audífonos.
 */
class WebService : Service() {

    companion object {
        private const val CANAL = "reproduccion"
        private const val ID = 7
        private var vivo: WebService? = null
        private var titulo = ""
        private var artista = ""
        private var album = ""
        private var sonando = false
        private var posMs = 0L
        private var durMs = 0L
        private var vel = 1f
        private var arte: Bitmap? = null
        private var claveArte = ""
        private val main = Handler(Looper.getMainLooper())

        fun estaSonando() = sonando

        fun crearCanal(ctx: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = ctx.getSystemService(NotificationManager::class.java)
                if (nm.getNotificationChannel(CANAL) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(CANAL, "Música sonando", NotificationManager.IMPORTANCE_LOW).apply {
                            setShowBadge(false)
                        }
                    )
                }
            }
        }

        fun actualizar(ctx: Context, t: String, a: String, s: Boolean) {
            titulo = t; artista = a; sonando = s
            refrescar(ctx)
        }

        /** Datos completos que manda la pantalla principal (título, artista, álbum, posición, duración…). */
        fun actualizarDatos(ctx: Context, o: JSONObject) {
            titulo = o.optString("t"); artista = o.optString("a"); album = o.optString("al")
            sonando = o.optBoolean("s")
            posMs = (o.optDouble("pos", 0.0) * 1000).toLong()
            durMs = (o.optDouble("dur", 0.0).takeIf { !it.isNaN() && !it.isInfinite() } ?: 0.0).times(1000).toLong()
            vel = o.optDouble("rate", 1.0).toFloat()
            val mid = o.optLong("mid", 0L)
            // para que Android Auto siga con la misma canción en el mismo punto
            if (mid > 0) Respaldo.guardarUltima(ctx, Biblioteca.idCancion("todas", mid), posMs)
            refrescar(ctx)
        }

        private fun refrescar(ctx: Context) {
            val v = vivo
            if (v != null) { v.mostrar(); return }
            if (sonando) {
                try {
                    if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(Intent(ctx, WebService::class.java))
                    else ctx.startService(Intent(ctx, WebService::class.java))
                } catch (e: Exception) {}
            }
        }

        /** Carátula: "data:image/…;base64,…", una dirección https:// o vacío. */
        fun ponerPortada(ctx: Context, s: String) {
            if (s == claveArte) return
            claveArte = s
            if (s.isEmpty()) { arte = null; vivo?.mostrar(); return }
            Thread {
                val bm = try {
                    if (s.startsWith("data:")) {
                        val b = Base64.decode(s.substringAfter(","), Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(b, 0, b.size)
                    } else if (s.startsWith("https://")) {
                        val c = URL(s).openConnection() as HttpURLConnection
                        c.connectTimeout = 8000; c.readTimeout = 8000
                        c.inputStream.use { BitmapFactory.decodeStream(it) }
                    } else null
                } catch (e: Exception) { null }
                val chico = bm?.let {
                    val m = maxOf(it.width, it.height)
                    if (m > 512) Bitmap.createScaledBitmap(it, it.width * 512 / m, it.height * 512 / m, true) else it
                }
                main.post { if (claveArte == s) { arte = chico; vivo?.mostrar() } }
            }.start()
        }

        fun detener(ctx: Context) {
            try { ctx.stopService(Intent(ctx, WebService::class.java)) } catch (e: Exception) {}
        }
    }

    private var sesion: MediaSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vivo = this
        crearCanal(this)
        sesion = MediaSession(this, "ReproductorJAT").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = MainActivity.orden("reanudar")
                override fun onPause() = MainActivity.orden("pausa")
                override fun onStop() = MainActivity.orden("pausa")
                override fun onSkipToNext() = MainActivity.orden("sig")
                override fun onSkipToPrevious() = MainActivity.orden("ant")
                override fun onSeekTo(pos: Long) = MainActivity.orden("seek:" + (pos / 1000.0))
            })
            setSessionActivity(
                PendingIntent.getActivity(
                    this@WebService, 1, Intent(this@WebService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ant" -> MainActivity.orden("ant")
            "sig" -> MainActivity.orden("sig")
            "play" -> MainActivity.orden(if (sonando) "pausa" else "reanudar")
            "cerrar" -> { MainActivity.orden("pausa"); sonando = false }
        }
        mostrar()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try { sesion?.isActive = false; sesion?.release() } catch (e: Exception) {}
        sesion = null
        if (vivo === this) vivo = null
        super.onDestroy()
    }

    private fun accion(nombre: String, icono: Int, texto: String): Notification.Action {
        val pi = PendingIntent.getService(
            this, nombre.hashCode(), Intent(this, WebService::class.java).setAction(nombre),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        @Suppress("DEPRECATION")
        return Notification.Action.Builder(icono, texto, pi).build()
    }

    private fun publicarSesion() {
        val s = sesion ?: return
        val md = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, titulo.ifEmpty { "Reproductor JAT" })
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artista)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, titulo.ifEmpty { "Reproductor JAT" })
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, artista)
        if (durMs > 0) md.putLong(MediaMetadata.METADATA_KEY_DURATION, durMs)
        arte?.let { md.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it); md.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, it) }
        s.setMetadata(md.build())
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_STOP
                )
                .setState(if (sonando) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, posMs, if (sonando) vel else 0f, SystemClock.elapsedRealtime())
                .build()
        )
        if (!s.isActive) s.isActive = true
    }

    fun mostrar() {
        publicarSesion()
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val estilo = Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2)
        sesion?.let { estilo.setMediaSession(it.sessionToken) }
        val b = Notification.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_nota)
            .setContentTitle(titulo.ifEmpty { "Reproductor JAT" })
            .setContentText(artista.ifEmpty { if (sonando) "Sonando" else "En pausa" })
            .setContentIntent(abrir)
            .setOngoing(sonando)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(accion("ant", android.R.drawable.ic_media_previous, "Anterior"))
            .addAction(
                if (sonando) accion("play", android.R.drawable.ic_media_pause, "Pausa")
                else accion("play", android.R.drawable.ic_media_play, "Reproducir")
            )
            .addAction(accion("sig", android.R.drawable.ic_media_next, "Siguiente"))
            .setStyle(estilo)
        arte?.let { b.setLargeIcon(it) }
        val n = b.build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(ID, n)
        } catch (e: Exception) {
            getSystemService(NotificationManager::class.java).notify(ID, n)
        }
        if (!sonando) {
            // en pausa: se puede quitar deslizando, y Android puede cerrar el servicio
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }
}
