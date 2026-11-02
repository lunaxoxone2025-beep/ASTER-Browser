# ASTER Browser — Android MVP 0.6.1

ASTER es un navegador Android en desarrollo, con una interfaz oscura y minimalista, accesos rápidos, navegación por pestañas y controles pensados para pantallas móviles. Su lenguaje visual toma como referencia la claridad y la estructura de navegadores modernos como Brave, pero mantiene la identidad propia de ASTER.

## Novedades de 0.6.0
- Rediseño de la barra superior y la barra de navegación inferior.
- Barra de direcciones con controles separados y áreas táctiles más cómodas.
- Página de inicio renovada con buscador, accesos rápidos y estado de privacidad básico.
- Publicación automática de una GitHub Release con el APK cuando la compilación termina correctamente.
- Ajustes de tamaños y espaciados para pantallas Android con distintas densidades.

## Funciones actuales
- Página de inicio ASTER con buscador, accesos rápidos y marcadores recientes.
- Buscador configurable: Google, DuckDuckGo o Bing.
- Página de inicio web configurable.
- Atrás, adelante, inicio, recarga y selector de pestañas.
- Hasta 8 pestañas; las inactivas se pausan para reducir trabajo en segundo plano.
- Restauración local de pestañas.
- Barra de progreso de carga.
- Descargas mediante Android DownloadManager.
- Búsqueda dentro de la página y uso del menú Compartir de Android.
- Marcadores e historial locales.
- Limpieza de historial, cookies, caché y datos web.
- Preferencias guardadas localmente.

## Privacidad y seguridad
- El acceso web a contenido file:// y content:// está desactivado.
- El contenido mixto inseguro está bloqueado.
- Las cookies de terceros están desactivadas.
- Safe Browsing se habilita cuando está disponible en Android.

**Importante:** ASTER todavía no incluye un bloqueador avanzado de rastreadores ni un motor de renderizado propio. La navegación utiliza Android System WebView, que depende del componente WebView/Chrome instalado en el dispositivo.

## Descargar
Cuando una compilación termina correctamente, GitHub Actions crea o actualiza automáticamente una publicación en **Releases** y adjunta el archivo `app-debug.apk`.

- Abre [Releases](https://github.com/lunaxoxone2025-beep/ASTER-Browser/releases).
- Entra en la versión más reciente.
- Descarga `app-debug.apk` e instálalo en Android.
- Si Android lo solicita, autoriza la instalación desde esa fuente.

El APK de estas versiones es una compilación de depuración, no una versión firmada para distribución comercial.

## Estado del proyecto
ASTER sigue siendo un MVP. La interfaz y las funciones deben probarse en dispositivos reales; una compilación correcta de GitHub no sustituye las pruebas en un teléfono físico. La versión de escritorio se abordará en una fase posterior.
