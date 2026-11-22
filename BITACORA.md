# Bitácora — Reproductor JAT

Aquí se anota cada cambio del reproductor (web y app Android), de lo más nuevo a lo más antiguo.
Antes de cada cambio se revisa esta bitácora y la lista [MEJORAS.md](MEJORAS.md).

---

## 22 nov 2026 — Mixer (estilo DJ)
**Pedido por Jorge:** crear la función mixer, como la de la captura (ondas de las dos canciones, BPM, tono "10A", "8 bars").

**Novedades**
- **🎛 Mixer**: en ⋮ → "Mixer", o en la tarjeta DJ de "Reproduciendo". Pantalla oscura con:
  - **Arriba la canción que suena y abajo la siguiente**, cada una con carátula, título, artista, **BPM**, **tono** (rueda Camelot de los DJ, ej. "8A" = La menor) y tiempo.
  - **Ondas de colores** que avanzan: **azul = graves** (bombo, bajo), **naranja = medios** (voz, guitarra), **blanco = agudos** (platillos). Líneas de **golpes** (más fuertes cada compás) y **línea verde** = lo que suena.
  - **⟲5 / ▶ / 5⟳** y **🔁 Repetir** 1, 2, 4, 8 o 16 compases (loop unido a los golpes).
  - **🔍− / 🔍+** para ver más o menos tiempo. **Arrastrar la onda** de arriba para moverse; **tocar el resumen** de abajo para saltar.
  - Aviso de si **los tonos combinan** y si **la velocidad es parecida** (se unen los golpes).
  - **⇄ Mezclar ahora**: pasa a la siguiente con mezcla DJ (golpes unidos), con los segundos del Modo DJ (o 8 s).
  - **🎯 Elegir la siguiente**: lista ordenada por las que mejor se mezclan (velocidad y tono parecidos ✔). La elegida también la usa el modo DJ automático.
- El **tono** de cada canción se calcula solo la primera vez que se ve en el mixer.

**Respaldo (regla fija):** el tono calculado de cada canción va en el respaldo (💾). Las ondas no hace falta guardarlas: se vuelven a calcular solas.

---

## 22 nov 2026 — Transmitir a otro aparato (Bluetooth)
**Pedido por Jorge:** poder mandar la música a otro aparato Bluetooth, como el botón de transmitir de Deezer.

**Novedades**
- En "Reproduciendo", arriba, el botón **📡 transmitir** (también en ⋮ → "Transmitir a otro aparato").
  - **🔊 Elegir dónde suena:** abre la ventanita del propio Android para pasar la música al celular, a un parlante o a unos audífonos Bluetooth. Si el celular no tiene esa ventanita, abre los ajustes de Bluetooth.
  - **🔵 Conectar otro aparato Bluetooth:** abre los ajustes de Bluetooth para emparejar uno nuevo.
- Debajo de la barra de tiempo se ve **dónde está sonando** ("🔵 Sonando en: JBL Flip 5", "📱 Este celular", "🎧 Audífonos con cable"). Se actualiza solo; tocarlo abre el mismo menú.
- En la web: Chromecast si el navegador lo permite, o elegir parlante en computadoras.

**Respaldo:** no guarda datos nuevos (el aparato lo recuerda Android).

---

## 22 nov 2026 — Abrir canciones desde otras apps
**Pedido de Jorge:** al tocar una canción en Archivos JAT, en "Abrir con" no salía el Reproductor JAT.

- La app Android ahora se anuncia como reproductor de audio: aparece en "Abrir con" para cualquier canción.
- Busca esa canción en la música del celular (por nombre y tamaño) y la pone a sonar dentro de la lista normal, así siguiente/anterior, letra y carátula funcionan igual.
- Si la canción no está en la música del celular, avisa "No encontré esa canción en tu música".

**Respaldo (regla fija):** no se guarda nada nuevo.

---

## 22 nov 2026 — Adiós al morado: azul marino o rojo
**Pedido por Jorge:** cambiar todo lo morado de la interfaz por azul marino o rojo.

**Novedades**
- Todo lo que era morado ahora es **azul marino** (botón redondo de aleatorio, canción que suena, botón ▶, barras, botones activos, menú de abajo, ❤, cuadro sin carátula, fondo de la letra sin carátula, mosaico "Géneros", pantalla de Ajustes de Android Auto).
- Los grises claros que tenían un toque lila ahora son grises neutros.
- **⋮ → Ajustes → "🎨 Color de la app"**: elegir **🔵 Azul marino** o **🔴 Rojo**. Cambia al instante.
- El fondo con el color de la carátula (reproductor y letra) sigue igual.

**Respaldo (regla fija):** el color elegido va en el respaldo (💾).

---

## 22 nov 2026 — Letra a pantalla completa (estilo Deezer)
**Pedido por Jorge:** que la letra se vea como en Deezer, con el color de la carátula o la carátula de fondo.

**Novedades**
- 🎤 **Letra** se abre ahora **a pantalla completa**: letras grandes, en negrita y centradas; arriba el título y el artista.
- Las líneas **ya cantadas y la que suena quedan en blanco**; las que vienen salen más tenues. La letra sube sola y deja la línea actual cerca del centro. Tocar una línea salta a esa parte de la canción.
- **Fondo del color de la carátula** (un tono más oscuro para que el blanco se lea bien). Con el botón **🖼** cambia a **la carátula difuminada de fondo**, y otra vez para volver al color.
- **A− / A+** cambian el tamaño de la letra. El botón **⧉** cambia a la ventanita flotante de antes (y **⛶** vuelve a pantalla completa).
- En la app, la barra de arriba del celular toma el mismo color, y el botón **atrás** cierra la letra.

**Respaldo (regla fija):** el modo de la letra (pantalla completa o ventanita, fondo color o carátula, tamaño) va en el respaldo (💾).

---

## 21 nov 2026 (noche) — Editar datos (TAG) y mejores carátulas
**Pedido por Jorge:** mejorar las carátulas y poder cambiar el TAG (título, artista…) para encontrar la carátula verdadera. Ejemplo: "Track 04" de Enanitos Verdes, álbum "Enanitos Verdes (Variadas)", salía sin carátula.

**Novedades**
- **✏️ Editar datos:** ⋮ de la canción → "Editar datos", o **tocar el título** en "Reproduciendo". Se puede cambiar título, artista, álbum, año y género. Al guardar:
  - busca la carátula con los datos nuevos; si no la encuentra sola, abre "Cambiar carátula" para elegirla;
  - vuelve a buscar la letra si cambió el título o el artista;
  - los datos nuevos salen también en Android Auto.
- **🔍 Reconocer por el sonido** una sola canción (⋮ de la canción, o dentro de "Editar datos"): escucha la canción y llena título, artista y álbum solos (necesita la clave AcoustID de Ajustes). El botón "Reconocer sin etiqueta" ahora también reconoce las que se llaman "Track 04", "Pista 4", "04", "Unknown"…
- **Carátulas mejores:**
  - si no encuentra la canción (por ejemplo "Track 04"), busca la carátula del **álbum** por artista + nombre del álbum;
  - no se deja engañar por álbumes genéricos como "(Variadas)", "Éxitos" o "Vol. 2": en esos casos no pone una al azar;
  - en "Cambiar carátula" ahora salen también **los álbumes del artista** (marcados con 💿) para elegir el correcto.

**Respaldo (regla fija):** los datos editados a mano van en el respaldo (💾) marcados como "a mano": al cargar el respaldo en una instalación nueva **mandan sobre lo que dice el archivo**. Lo mismo la carátula elegida a mano. Los datos se guardan en la app y en el respaldo; **el archivo de música no se modifica**.

---

## 21 nov 2026 (noche) — Carátulas: arreglo y cambiar a mano
**Problema que vio Jorge:** canciones de Tamara ("Canta Roberto Carlos") salían con la carátula de Roberto Carlos, y no había forma de cambiarla.

**Causa:** la búsqueda aceptaba un resultado si el artista aparecía en *cualquier* dato de la canción, incluido el álbum. Como el álbum dice "Roberto Carlos", aceptaba los discos de Roberto Carlos.

**Arreglos y novedades**
- Ahora el **artista del resultado tiene que coincidir con el artista de la canción** (no con el álbum).
- Las carátulas que se pusieron solas con la búsqueda anterior **se revisan otra vez solas**: si había una mejor, se cambia; si la de antes no era, se quita.
- **⋮ de la canción → "🖼 Cambiar carátula"**: muestra varias carátulas posibles (con artista y álbum debajo, la más probable primero) para tocar la correcta. También:
  - **📷 Elegir una imagen del celular** (una foto o una imagen descargada).
  - **🚫 Dejar sin carátula.**
- Lo que eliges a mano **ya no se cambia solo**.

**Respaldo (regla fija):** la carátula elegida va en el respaldo (💾). Si eliges una imagen del celular, se guarda achicada dentro del respaldo, así que vuelve aunque se borre la app.

---

## 21 nov 2026 (noche) — Carátula grande y fondo del color de la carátula
**Pedido por Jorge:** carátula grande y que el fondo del reproductor cambie al color de la carátula (como en Deezer).

**Novedades**
- En "Reproduciendo" la **carátula es grande** (casi todo el ancho de la pantalla), con sombra.
- **El fondo toma un color de la carátula** de cada canción: el reproductor saca el color más vivo y que más se ve en la portada y pinta todo el fondo con él. Cambia suave al pasar de canción.
- Las letras, botones y barras se ponen **negras o blancas solas** según el color, para que siempre se lean bien.
- En la app, la **barra de arriba del celular** (hora y batería) también toma ese color mientras el reproductor está abierto.
- Si la canción no tiene carátula, queda el fondo claro con el cuadro morado de siempre.

---

## 21 nov 2026 (noche) — Fuera los mensajes de voz de WhatsApp
**Pedido por Jorge:** que los mensajes de voz de WhatsApp no salgan en el reproductor.

**Novedades**
- Los mensajes de voz de WhatsApp (carpeta "WhatsApp Voice Notes", archivos "PTT-…") **ya no entran a la lista** del reproductor, ni en la app, ni en la web, ni en el carro (Android Auto).
- Los que ya estaban en la lista se quitan solos al abrir la app.
- **No se borra nada del celular:** los mensajes siguen en WhatsApp; solo dejan de aparecer en el reproductor.
- Los audios que te mandan por WhatsApp que no son notas de voz (carpeta "WhatsApp Audio", archivos "AUD-…", que a veces son canciones) sí se siguen mostrando.

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
