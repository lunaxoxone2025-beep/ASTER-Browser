# ASTER Browser

Versión Android actual: **0.15.0**.

ASTER es un navegador para Android en desarrollo, con pestañas, marcadores, historial, descargas y controles de privacidad. La nueva dirección visual utiliza tonos oscuros violetas, una página de inicio renovada y controles inspirados de forma general en navegadores modernos como Brave y Opera GX, manteniendo una identidad propia.

## Motor y plataforma

ASTER utiliza actualmente Android System WebView. No es un navegador Chromium independiente y, por tanto, algunas funciones avanzadas de otros navegadores no están disponibles.

## ASTER Shield — bloqueo de anuncios y rastreadores

- Importa reglas de red basadas en dominios desde EasyList, EasyPrivacy, AdGuard Base, AdGuard Tracking Protection, AdGuard Mobile Ads y la lista de servidores publicitarios de Peter Lowe.
- Mantiene las reglas en caché y actualiza las listas cada 24 horas cuando hay conexión.
- Permite actualizar los filtros manualmente desde el panel Shield.
- Oculta algunos espacios publicitarios y superposiciones de anuncios de YouTube, e intenta pulsar automáticamente el botón visible «Saltar anuncio».
- Incluye protección global, excepciones por sitio y contadores de solicitudes bloqueadas.

**Limitación importante:** la integración actual con WebView solo aplica reglas de red basadas en dominios. No aplica selectores cosméticos CSS, scriptlets ni todas las opciones avanzadas de ABP. Por eso puede bloquear muchas solicitudes de publicidad y rastreo, pero algunos anuncios o espacios vacíos pueden seguir apareciendo. No promete el mismo nivel de bloqueo que Brave.

## Página de inicio y pestañas

- Personaliza la página de inicio mostrando u ocultando la marca ASTER, los accesos rápidos y los marcadores.
- Elige entre cuatro fondos: Violet, Midnight, Ocean y Crimson.
- El selector de pestañas ahora utiliza tarjetas en dos columnas, con título, dominio, indicador de pestaña activa, botón para cerrar y acceso rápido a una pestaña nueva.
- ASTER Shield sigue disponible directamente desde la barra superior.

## Compatibilidad y funciones de navegación

ASTER adapta las barras y la navegación a móviles Android y a ventanas amplias o redimensionables de tablets, plegables y otros dispositivos Android compatibles. También incorpora:
- Modo escritorio para solicitar las versiones de escritorio de las páginas.
- Modo lectura para simplificar artículos.
- Traducción de páginas mediante Google Translate, con idioma de destino configurable.
- Tamaño de texto ajustable.
- Subida de archivos desde el selector de archivos de Android.
- Apertura de enlaces HTTP/HTTPS de otras aplicaciones cuando ASTER está seleccionado como navegador.
- Atajos de teclado: Ctrl+L (barra de direcciones), Ctrl+T (nueva pestaña), Ctrl+W (cerrar pestaña), Ctrl+R (recargar), Ctrl+Tab / Ctrl+Shift+Tab (cambiar pestaña) y Esc (salir del modo lectura).
- Copias de seguridad que ahora incluyen estas nuevas preferencias.

ASTER sigue siendo una aplicación Android; esto no la convierte en un navegador nativo para iOS, Windows o macOS. El modo escritorio continúa utilizando Android System WebView. La traducción necesita conexión a internet y el modo lectura puede no interpretar perfectamente algunas páginas complejas.

## Copias de seguridad

En el menú del navegador, abre **Backup & restore** para exportar o importar marcadores, historial, ajustes y pestañas abiertas en un archivo JSON. No incluye cookies, contraseñas ni caché.

## Actualizaciones

ASTER comprueba si hay nuevas versiones en GitHub Releases y puede descargar el APK para abrir el instalador de Android. Android siempre exige confirmación. Para actualizar una instalación existente, todas las versiones deben estar firmadas con la misma clave permanente. Mientras no esté configurada, los APK de depuración son solo para pruebas y podrían no instalarse encima de una versión anterior.

## Compilación

El proyecto Android utiliza Gradle y Android Gradle Plugin. GitHub Actions se encarga de compilar el APK.
