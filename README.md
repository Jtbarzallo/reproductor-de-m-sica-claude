# Reproductor de Música JAT

Reproductor de música sencillo en una sola página HTML.

- Reproducir/pausar, anterior/siguiente, barra de tiempo y volumen
- Modos aleatorio y repetir
- Mezcla DJ: clasifica qué canciones se pueden mezclar (golpe marcado) y las agrupa por velocidad parecida; también en Android Auto (Más → Mezcla DJ)
- Modo DJ: mezcla una canción con la siguiente (4, 8 o 12 s) como un DJ: une los golpes de las dos canciones (iguala la velocidad sin cambiar el tono y entra justo en el golpe), pasa los bajos de una a la otra en vez de bajar el volumen, termina la mezcla antes de que la canción se apague sola, iguala el volumen y, en aleatorio, elige una siguiente con velocidad parecida. Salta los silencios del inicio y del final
- Sonido estéreo: Normal, Ampliado, Máximo y Mono→Estéreo. Se ajusta solo a cada canción (crea estéreo si es mono, no exagera si ya es abierta), con medidor izquierda/derecha y prueba de parlantes
- Elegir la carpeta de música: carga todo y revisa sola cada minuto si hay canciones nuevas (o borradas)
- Ver por género, ritmo (lento / moderado / rápido según BPM), artista, álbum, año y carpeta
- Clasificar: género desde la etiqueta o MusicBrainz y ritmo analizado del audio
- Lista de canciones (ordenar y quitar)
- Lee etiquetas de los archivos: título, artista, álbum y portada (si no tiene, usa el nombre del archivo)
- Reconoce canciones sin etiqueta por el sonido (AcoustID + MusicBrainz, portada de Cover Art Archive). Necesita una clave gratis de acoustid.org
- Letra en ventana flotante que se puede mover, sincronizada con la canción cuando hay (lrclib.net)
- Controles desde la pantalla de bloqueo del celular
- Recuerda canciones, posición y volumen al cerrar
- **Respaldo en archivo** (💾 Guardar / 📂 Cargar): lista, orden, posición, volumen, modos, etiquetas, letras y clave

## App Android (Android Auto)
👉 [Descargar ReproductorJAT.apk](https://github.com/Jtbarzallo/reproductor-de-m-sica-claude/releases/latest/download/ReproductorJAT.apk) — detalles en [android/README.md](android/README.md)
