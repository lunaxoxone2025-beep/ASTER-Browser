# ASTER Browser

Versión Android actual: **0.12.0**.

ASTER es un navegador para Android en desarrollo, con pestañas, marcadores, historial, descargas y controles de privacidad. La nueva dirección visual utiliza tonos oscuros violetas, una página de inicio renovada y controles inspirados de forma general en navegadores modernos como Brave y Opera GX, manteniendo una identidad propia.

## Motor y plataforma

ASTER utiliza actualmente Android System WebView. No es un navegador Chromium independiente y, por tanto, algunas funciones avanzadas de otros navegadores no están disponibles.

## ASTER Shield — bloqueo de anuncios y rastreadores

- Importa reglas de red basadas en dominios compatibles desde EasyList y EasyPrivacy.
- Mantiene reglas locales en caché y actualiza las listas semanalmente cuando hay conexión.
- Permite actualizar los filtros manualmente desde el panel Shield.
- Incluye protección global, excepciones por sitio y contadores de solicitudes bloqueadas.

**Limitación importante:** la integración actual con WebView solo aplica reglas de red basadas en dominios. No aplica selectores cosméticos CSS, scriptlets ni todas las opciones avanzadas de ABP. Por eso puede bloquear muchas solicitudes de publicidad y rastreo, pero algunos anuncios o espacios vacíos pueden seguir apareciendo. No promete el mismo nivel de bloqueo que Brave.

## Copias de seguridad

En el menú del navegador, abre **Backup & restore** para exportar o importar marcadores, historial, ajustes y pestañas abiertas en un archivo JSON. No incluye cookies, contraseñas ni caché.

## Actualizaciones

ASTER comprueba si hay nuevas versiones en GitHub Releases y puede descargar el APK para abrir el instalador de Android. Android siempre exige confirmación. Para actualizar una instalación existente, todas las versiones deben estar firmadas con la misma clave permanente. Mientras no esté configurada, los APK de depuración son solo para pruebas y podrían no instalarse encima de una versión anterior.

## Compilación

El proyecto Android utiliza Gradle y Android Gradle Plugin. GitHub Actions se encarga de compilar el APK.
