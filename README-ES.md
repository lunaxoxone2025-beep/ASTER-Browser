# ASTER Browser

Versión Android actual: **1.6.0**.

ASTER es un navegador Android original y centrado en la privacidad, con una identidad inspirada en el cielo nocturno: bloqueo integrado de anuncios y rastreadores (**ASTER Halo**), gestión de memoria (**ASTER Core**), una página de inicio viva con juegos gratis (**Arcade**), el **botón Órbita** y temas propios, incluido un aspecto **Liquid Glass** completo.

> ASTER usa **Android System WebView**. No admite extensiones y algunas funciones de motor (uBlock Origin completo, Tor, sincronización) no están disponibles.

## Novedades principales

### ⛨ ASTER Halo
- Bloqueo con **EasyList, EasyPrivacy, AdGuard y la lista de Peter Lowe**, actualizado cada 24 h, y filtrado cosmético.
- Modos **Estándar / Agresivo** y controles por sitio: Halo activado/desactivado, antifingerprinting, ocultar anuncios, bloquear scripts, permitir cookies de terceros.
- Bloqueo de ventanas emergentes, solo HTTPS y contadores en la página de inicio.

### 🎨 Temas
- **Liquid Glass** y **Liquid Glass Light**: cápsulas flotantes translúcidas con brillo y borde luminoso sobre un fondo de color; los menús y páginas se apoyan en un **desenfoque real** de lo que hay detrás.
- **ASTER Night, Nova, Nebula, Aurora, Comet, Solar Flare, Glacier y Ember**.

### ⚡ ASTER Core
Uso de memoria en directo, **ahorro de memoria** (duerme las pestañas menos usadas), vista de pestañas abiertas, ahorro de datos y limpieza de caché.

### 🕹 Arcade
Centro gaming: juegos gratis de la semana en Epic Games Store (y los próximos), regalos para Steam, Xbox, PlayStation, GOG, móvil, Switch y DLC (GamerPower), y noticias de GameSpot, PC Gamer, Xbox Wire, PlayStation Blog y Vandal. Los **avisos de juegos gratis** opcionales te notifican cuando aparece un juego gratis nuevo en las tiendas que sigues.

### 🏝 ASTER Island
Toda la interfaz en una pequeña cápsula abajo, al alcance del pulgar: título y web, la estrella de Halo con los bloqueos, contador de pestañas y menú, con la barra de carga en su borde. Tócala para buscar, deslízala para cambiar de pestaña o hacia arriba para ver todas. Se esconde mientras lees.

### 🧭 Navegación
- **Menú integrado** con acciones rápidas, tarjeta de la página, interruptores rápidos y cuadrícula; los paneles se cierran deslizando hacia abajo.
- Barra que se oculta al hacer scroll, deslizar en la barra para cambiar de pestaña, **botón Órbita** opcional y diseño para tablets.
- Imagen en imagen, quitar anuncios de YouTube (experimental), **Microsoft Rewards** con Bing, interfaz en español, ventanas privadas y copias de seguridad.

## Compilación

Java + framework de Android, sin dependencias externas. GitHub Actions compila el APK; los commits con `[release]` publican una Release con `ASTER-Browser.apk`.
