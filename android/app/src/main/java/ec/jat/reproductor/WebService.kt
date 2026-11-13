package ec.jat.reproductor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Mantiene viva la app mientras suena música desde la pantalla principal
 * y muestra la notificación con ⏮ ⏯ ⏭.
 */
class WebService : Service() {

    companion object {
        private const val CANAL = "reproduccion"
        private const val ID = 7
        private var vivo: WebService? = null
        private var titulo = ""
        private var artista = ""
        private var sonando = false

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
            val v = vivo
            if (v != null) { v.mostrar(); return }
            if (s) {
                try {
                    if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(Intent(ctx, WebService::class.java))
                    else ctx.startService(Intent(ctx, WebService::class.java))
                } catch (e: Exception) {}
            }
        }

        fun detener(ctx: Context) {
            try { ctx.stopService(Intent(ctx, WebService::class.java)) } catch (e: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vivo = this
        crearCanal(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ant" -> MainActivity.orden("ant")
            "sig" -> MainActivity.orden("sig")
            "play" -> MainActivity.orden(if (sonando) "pausa" else "play")
            "cerrar" -> { MainActivity.orden("pausa"); sonando = false }
        }
        mostrar()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
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

    fun mostrar() {
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = Notification.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_nota)
            .setContentTitle(titulo.ifEmpty { "Reproductor JAT" })
            .setContentText(artista.ifEmpty { if (sonando) "Sonando" else "En pausa" })
            .setContentIntent(abrir)
            .setOngoing(sonando)
            .setShowWhen(false)
            .addAction(accion("ant", android.R.drawable.ic_media_previous, "Anterior"))
            .addAction(
                if (sonando) accion("play", android.R.drawable.ic_media_pause, "Pausa")
                else accion("play", android.R.drawable.ic_media_play, "Reproducir")
            )
            .addAction(accion("sig", android.R.drawable.ic_media_next, "Siguiente"))
            .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
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
