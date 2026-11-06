# ASTER Browser

Versión Android actual: **1.1.0** (la 1.0 fue la reconstrucción completa; la 1.1 añade fluidez, interfaz en español, mejoras para YouTube y Microsoft Rewards).

ASTER es un navegador para Android centrado en la privacidad y con estética gaming. La versión 1.0 reúne lo mejor de **Brave** (Shields, bloqueo integrado, estadísticas de privacidad) y de **Opera GX** (temas GX, Fast Action Button, GX Control, GX Corner) manteniendo una identidad propia.

> ASTER usa **Android System WebView** para mostrar las páginas. No es un fork de Chromium: no admite extensiones y algunas funciones de motor (uBlock Origin completo, Tor, sincronización) no están disponibles.

## Novedades principales

### ⛨ ASTER Shields (estilo Brave)
- Bloqueo de red con **EasyList, EasyPrivacy, AdGuard (Base, Tracking, Mobile) y la lista de Peter Lowe**, actualizado cada 24 h. Las reglas se guardan en archivos.
- **Filtrado cosmético**: reglas `##selector` genéricas y por sitio que ocultan huecos publicitarios y banners.
- Modos **Estándar / Agresivo**.
- **Panel por sitio**: Shields activados/desactivados, bloqueo de fingerprinting, ocultar anuncios, bloquear scripts y lista de dominios bloqueados.
- **Protección contra fingerprinting**, señales Global Privacy Control y DNT, **bloqueo de pop-ups**, solo HTTPS, cookies de terceros bloqueadas.
- YouTube: oculta la interfaz de anuncios, pulsa «Saltar» y adelanta anuncios de vídeo cuando se cuelan (sin garantía).
- Contadores en la página de inicio: **anuncios y rastreadores bloqueados, datos ahorrados y tiempo ahorrado**.

### ⚡ Funciones GX (estilo Opera GX)
- **Temas**: ASTER Night, Shields Dark, GX Classic, GX Purple Haze, GX Hackerman, GX Ultraviolet, GX Frutti di Mare, GX Ice Cold y Liquid Glass.
- **Fast Action Button**: botón flotante opcional que despliega acciones rápidas. Desliza ← / → para atrás/adelante, ↑ para pestañas y mantén pulsado para una pestaña nueva.
- **GX Control**: uso de RAM en directo, **limitador de RAM** (duerme las pestañas menos usadas), **Hot Tabs Killer**, ahorro de datos y limpieza de caché.
- **GX Corner**: juegos gratis en la página de inicio (API de GamerPower).
- **Sonidos y vibración GX** opcionales.
- **Fondos animados**: Nebula, Synthwave, Aurora, Lluvia de código y Minimal.

### 🎮 YouTube y Microsoft Rewards
- **YouTube**: quita los datos de anuncios del reproductor, bloquea sus servidores de anuncios, pantalla completa con giro automático, **imagen en imagen** y reproducción en segundo plano opcional.
- **Microsoft Rewards**: acceso directo a Rewards, cambio a Bing con un toque y la sesión de Microsoft se mantiene iniciada. Busca con Bing normalmente con tu cuenta iniciada para ganar puntos (automatizar búsquedas va contra las normas de Microsoft).

### 🧭 Navegación
- **Interfaz en español o inglés** (automática según el idioma del móvil).
- La barra **se oculta al hacer scroll**; **desliza sobre la barra de direcciones** para cambiar de pestaña; en tablets y plegables hay **barra de pestañas**.
- Hasta 100 pestañas con **pestañas dormidas**, selector visual con miniaturas y deslizar para cerrar.
- Barra de direcciones con **sugerencias**, coincidencias de historial y marcadores y «pegar e ir»; puede ir **arriba o abajo**.
- **Vídeo a pantalla completa**, **menú al mantener pulsado** enlaces e imágenes, abrir en pestaña nueva, en segundo plano o privada.
- **Deslizar para recargar**, **buscar en la página**, **modo lectura**, **modo noche para webs**, **versión de escritorio** por pestaña, **traducir**, **añadir a la pantalla de inicio**, **búsqueda por voz**.
- Gestores de marcadores, historial y descargas; **marcación rápida** editable.
- Buscadores: Brave Search (predeterminado en instalaciones nuevas), DuckDuckGo, Google, Bing, Startpage, Ecosia y Qwant.
- **Reproducción en segundo plano** opcional.

### 👁 Ventanas privadas
El navegador completo en un proceso separado con su propio perfil de WebView (Android 9+). No guarda historial, sesiones ni marcadores; los datos de los sitios se borran al cerrar la ventana.

### 💾 Copias de seguridad
Exporta marcadores, historial, marcación rápida, Shields por sitio, ajustes y pestañas a un archivo JSON. También importa copias de la versión 0.x. Nunca incluye cookies ni contraseñas.

## Compilación

Java + framework de Android, sin dependencias externas. GitHub Actions compila el APK; los commits con `[release]` publican una Release con `ASTER-Browser.apk`.
