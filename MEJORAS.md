# Lista de mejoras — Reproductor JAT

## Hechas
- [x] Modo DJ "de verdad" (13 nov): une los golpes (iguala velocidad sin cambiar el tono y entra en el golpe), termina la mezcla antes de que la canción baje sola, iguala el volumen, y en aleatorio elige la siguiente con velocidad parecida
- [x] Ecualizador de 5 barras con ajustes listos y tope para que no sature (13 nov); queda guardado y va en el respaldo
- [x] Clasificación "Mezcla DJ": mezclables / no mezclables y grupos por velocidad (13 nov)
- [x] Estéreo rehecho: se adapta a cada canción, medidor izquierda/derecha y prueba de parlantes (13 nov)
- [x] Audio 8D (13 nov): el sonido gira alrededor de la cabeza, 3 velocidades; para audífonos
- [x] Letras: búsqueda que entiende títulos tipo "Artista - Canción (Video Oficial)" y artistas mal puestos; solo acepta la letra si título/artista/duración coinciden (13 nov)

- [x] Modo DJ (18 nov): fundido cruzado — la canción que termina baja poco a poco mientras la nueva sube, durante toda la mezcla (4, 8 o 12 s)

- [x] Fundido entre canciones también en el carro / Android Auto (18 nov): la que termina baja poco a poco y la nueva sube hasta igualarse; se elige en Ajustes (Apagado, 4, 8 o 12 s; por defecto 8 s) y va en el respaldo

- [x] Audio 8D rehecho (18 nov): antes solo subía y bajaba el volumen de cada oído. Ahora el oído lejano escucha hasta 0,7 ms después (lo que más ubica el sonido), la cabeza le quita agudos al oído lejano, detrás suena más opaco y con más eco de sala (profundidad), y los graves se quedan al centro. Medido: el volumen total ya no hace vaivén (antes ~3 dB extra)
- [x] Mesa DJ a mano (18 nov), basada en el "Mini Mixer DJ": dos platos (A y B) con canción, play/pausa, volver al inicio, barra, volumen, bajos y velocidad propios; crossfader de potencia igual con Centrar; "Sync" iguala la velocidad (BPM) con el otro plato; "Pasar al otro plato" mezcla sola en 8 s; tocar una canción de la lista la carga en el plato que no suena; el plato que más suena es el principal (título, barra, letra). Al cerrar la mesa vuelve el modo DJ automático
- [x] Pantalla nueva estilo "Canciones favoritas" (21 nov): menú de abajo (Inicio, Explorar, Favoritos, Búsqueda), lista con portadas y ⋮, mini reproductor oscuro, reproductor a pantalla completa, botón morado de aleatorio. En la web y en la app
- [x] Favoritas con ❤ (21 nov): sección Favoritos, agregar varias con buscador, se reproduce dentro de las favoritas; van en el respaldo
- [x] Búsqueda por título, artista, álbum o género (21 nov)
- [x] Carátulas automáticas desde internet (21 nov): iTunes → Deezer → MusicBrainz, solo si título y artista coinciden; botón para buscar las que faltan y para una canción; también en Android Auto; van en el respaldo
- [x] Mensajes de voz de WhatsApp fuera de la lista (21 nov), en la app, la web y Android Auto; no se borran del celular
- [x] Carátula grande y fondo del color de la carátula (21 nov), también la barra de arriba del celular en la app
- [x] Carátulas: el artista tiene que coincidir con el de la canción (no con el álbum); se revisan las de antes; "Cambiar carátula" para elegir entre varias, una imagen del celular o ninguna (21 nov)
- [x] Editar datos (TAG) de cada canción, reconocer una sola por el sonido, carátula por álbum cuando el título es "Track 04", álbumes del artista en "Cambiar carátula" (21 nov); van en el respaldo
- [x] Letra a pantalla completa estilo Deezer (22 nov): fondo del color de la carátula o la carátula difuminada, líneas cantadas en blanco; va en el respaldo
- [x] Sin morado (22 nov): interfaz en azul marino, con opción de rojo en Ajustes → Color de la app; va en el respaldo
- [x] Abrir con Reproductor JAT (22 nov): al tocar una canción en otra app (por ejemplo Archivos JAT), el Reproductor JAT aparece en "Abrir con" y la pone a sonar dentro de la lista de la música del celular
- [x] Transmitir a otro aparato Bluetooth (22 nov): selector de salida de Android, ajustes de Bluetooth y "Sonando en"
- [x] Mixer estilo DJ (22 nov): ondas de colores de las dos canciones, golpes/compases, BPM, tono Camelot, loop de compases, mezclar ahora, elegir la siguiente por compatibilidad; tono en el respaldo
- [x] Mixer DJ de dos platos (22 nov): suenan a la vez, SYNC de velocidad y golpes con corrección automática, crossfader, Vol y Bajos por plato, carga sola la siguiente
- [x] Mixer: mover las pistas a mano (22 nov): arrastrar la onda, ±1 golpe, ⏪/⏩ mantenidos, barra de velocidad ±10 %
- [x] Igualar tiempos (22 nov): bpm y tono en todas las filas, medición automática en segundo plano, grupo "Por velocidad", 🎯 solo tiempo parecido con % de ajuste, "Igualar solo" al cargar en el mixer; en el respaldo

## Pendientes / ideas
- [ ] Favoritas también en Android Auto (una pestaña "Favoritas" en el carro)
- [ ] Deslizar hacia abajo para cerrar el reproductor completo
- [ ] Pasar de canción deslizando la carátula a los lados (como Deezer)
- [ ] Guardar también los datos editados dentro del archivo de música (escribir el TAG en el MP3)
- [ ] Letra: botón "Con traducción" (traducir la letra al español/inglés)
- [ ] Mixer: marcar puntos (cue) y ecualizador por canción (graves/medios/agudos) mientras se mezcla
- [ ] Cola "Reproducir a continuación" desde el ⋮ de cada canción
- [ ] Entrar en el inicio de un compás/frase (no solo en un golpe)
- [ ] Mezclar también canciones de velocidades muy distintas con un efecto de transición (eco/filtro)
- [ ] Ecualizador, estéreo y 8D también en Android Auto (en el carro hoy no pasan por el sonido de la página)
- [ ] Mesa DJ: escuchar el plato que no suena por los audífonos antes de mezclarlo (pre-escucha)
