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

## Pendientes / ideas
- [ ] Entrar en el inicio de un compás/frase (no solo en un golpe)
- [ ] Mezclar también canciones de velocidades muy distintas con un efecto de transición (eco/filtro)
- [ ] Ecualizador, estéreo y 8D también en Android Auto (en el carro hoy no pasan por el sonido de la página)
- [ ] Mesa DJ: escuchar el plato que no suena por los audífonos antes de mezclarlo (pre-escucha)
