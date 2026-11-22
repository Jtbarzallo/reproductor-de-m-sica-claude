# Bitácora — Reproductor JAT

Aquí se anota cada cambio del reproductor (web y app Android), de lo más nuevo a lo más antiguo.
Antes de cada cambio se revisa esta bitácora y la lista [MEJORAS.md](MEJORAS.md).

---

## 21 nov 2026 (noche) — Carátulas automáticas
**Pedido por Jorge:** que el reproductor encuentre las carátulas de las canciones que salen sin portada (cuadro morado con la nota).

**Novedades**
- Las canciones que **no traen portada** buscan su carátula **solas en internet** (iTunes; si no está, Deezer; y si tampoco, MusicBrainz). Se hace en segundo plano, una por una, sin molestar.
- Solo acepta la carátula si **el título y el artista coinciden** con la canción, para no poner una portada equivocada. Si hay varias versiones, prefiere la del artista de la etiqueta (por ejemplo la versión de Tamara y no la original de Roberto Carlos).
- **⋮ de arriba → "🖼 Buscar carátulas que faltan"**: vuelve a intentar todas las que no se encontraron, mostrando el avance.
- **⋮ de cada canción → "🖼 Buscar carátula"** (o "Buscar otra vez la carátula" si salió una que no te gusta).
- La carátula se guarda dentro de la app para verla **sin internet**.
- **Android Auto:** en la pantalla del carro también sale la carátula encontrada.
- Si no hay internet, no se marca nada: se vuelve a intentar sola la próxima vez.

**Respaldo (regla fija):** la dirección de cada carátula encontrada va en el archivo de respaldo (💾) y en la copia automática de Descargas/ReproductorJAT. Al cargar el respaldo vuelven todas sin buscarlas otra vez.

---

## 21 nov 2026 — Nueva pantalla estilo "Canciones favoritas"
**Pedido por Jorge:** que el reproductor (web y app) se vea como la captura que le gustó (lista con portadas, barra oscura abajo y menú de 4 botones).

**Novedades**
- Pantalla clara nueva, con color morado de acento.
- Menú de abajo con 4 secciones: **Inicio · Explorar · Favoritos · Búsqueda**.
- **Lista de canciones con portada** a la izquierda, título en negrita (más fácil de leer), artista debajo y botón **⋮** a la derecha. La canción que suena sale en **morado**. Las que tienen letra muestran un micrófono 🎤.
- **Mini reproductor oscuro** fijo abajo: ▶/⏸, título y artista, ❤ favorita y ⏭, con una barrita morada que avanza con la canción. Al tocar el título se abre el reproductor completo.
- **Reproductor a pantalla completa** ("Reproduciendo"): portada grande, barra de tiempo, controles grandes, y debajo las tarjetas de **Sonido** (volumen, estéreo, 8D, ecualizador) y **DJ** (modo DJ y mesa DJ). Se cierra con la flecha ˅.
- **Favoritos (nuevo):** marcar canciones con ❤ (desde el mini reproductor, el reproductor completo o el ⋮ de cada canción). La sección Favoritos tiene "Agregar canciones" para marcar varias de una vez con buscador. Al tocar una favorita, siguiente/anterior/aleatorio se quedan dentro de las favoritas.
- **Búsqueda (nuevo):** busca por título, artista, álbum o género mientras escribes (no importan tildes ni mayúsculas).
- **Explorar:** mosaicos de colores para Géneros, Ritmo, Mezcla DJ, Artistas, Álbumes, Años y Carpetas, y el botón "Clasificar música".
- **Botón morado redondo** arriba: reproduce en aleatorio lo que estás viendo (toda la música, un grupo o tus favoritas).
- **⋮ de arriba:** elegir carpeta / buscar canciones nuevas, clasificar, sonido y DJ, guardar respaldo y **Ajustes** (carpeta, Android Auto, clave AcoustID y respaldo).
- **⋮ de cada canción:** reproducir, agregar/quitar de favoritas, reproducir y ver letra, subir en la lista, quitar.
- **Botón atrás:** en la app el botón "atrás" del celular primero cierra lo que esté abierto (menú, reproductor, ajustes, grupo) antes de mandar la app al fondo.
- App Android: barra de estado y pantalla de "Ajustes de Android Auto" en el mismo estilo claro y morado.

**Respaldo (regla fija):** las favoritas se guardan en el archivo de respaldo (💾) y en la copia automática de Descargas/ReproductorJAT de la app. Al cargar un respaldo vuelven a quedar marcadas.

**Se mantiene igual:** todo lo de antes (estéreo, 8D, ecualizador, modo DJ, mesa DJ, letras, reconocer, clasificar, carpeta, respaldo). La pantalla de Android Auto en el carro la dibuja el propio Android Auto, así que esa no cambia de diseño.

---

## 18 nov 2026
- Modo DJ: fundido cruzado durante toda la mezcla (4, 8 o 12 s).
- Fundido entre canciones también en el carro / Android Auto (Ajustes: Apagado, 4, 8 o 12 s).
- Audio 8D rehecho: retraso entre oídos, sombra de la cabeza, profundidad atrás; el volumen ya no hace vaivén.
- Mesa DJ a mano: plato A y B, crossfader, Sync de BPM y "Pasar al otro plato".

## 13 nov 2026
- Modo DJ "de verdad": une los golpes, iguala velocidad y volumen, y en aleatorio elige la siguiente con velocidad parecida.
- Ecualizador de 5 barras con ajustes listos.
- Clasificación "Mezcla DJ".
- Estéreo rehecho con medidor izquierda/derecha y prueba de parlantes.
- Audio 8D.
- Letras: búsqueda mejorada y verificación por título/artista/duración.
