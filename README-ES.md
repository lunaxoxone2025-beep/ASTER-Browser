# ASTER Browser — Android MVP 0.4.1

ASTER Browser es un prototipo de navegador Android con identidad visual propia. Esta versión incorpora una página de inicio interna, un icono de aplicación y ajustes para Android 15.

## Funciones
- Página de inicio ASTER con buscador, accesos rápidos y marcadores recientes.
- Buscador configurable: Google, DuckDuckGo o Bing.
- Página de inicio web configurable mediante el botón de inicio.
- Atrás, adelante, inicio y recarga.
- Hasta 12 pestañas, con selector y cierre.
- Restauración de pestañas y página activa al volver a abrir la aplicación.
- Barra de progreso de carga.
- Descargas con confirmación, notificación y acceso al gestor de descargas de Android.
- Búsqueda de texto dentro de la página y uso del menú Compartir de Android.
- Marcadores locales con opción para abrirlos o eliminarlos.
- Historial local.
- Limpieza de historial, cookies, caché y datos web.
- Enlaces telefónicos, de correo y otros esquemas compatibles delegados a las aplicaciones Android correspondientes.
- Preferencias guardadas localmente.
- Icono de aplicación ASTER para dispositivos Android modernos y antiguos.
- Ajuste de áreas seguras del sistema para Android 15 y etiquetas de accesibilidad para los controles principales.

## Privacidad y seguridad
- El acceso a contenido file:// y content:// desde páginas web está desactivado.
- El contenido mixto inseguro está bloqueado.
- Las cookies de terceros están desactivadas.
- Safe Browsing se habilita cuando está disponible en Android.
- La copia de seguridad automática de los datos de la aplicación está desactivada.
- La navegación web utiliza HTTPS cuando se introduce un dominio sin esquema.

## Limitaciones conocidas
- **No incluye un motor de renderizado propio.** Utiliza Android System WebView, que depende del componente WebView/Chrome instalado en el dispositivo.
- No hay todavía modo incógnito aislado, sincronización entre dispositivos ni bloqueo avanzado de rastreadores.
- Las pestañas se restauran localmente, pero los estados internos de formularios o páginas no se conservan.
- La versión actual es Android. La versión de escritorio se desarrollará en una fase posterior.
- Es un MVP en desarrollo; hace falta probarlo en dispositivos reales antes de considerarlo estable.

## Compilar el APK
1. Abre la pestaña **Actions** del repositorio.
2. Selecciona el workflow **Build ASTER Android APK**.
3. Descarga el artefacto **ASTER-Browser-debug** cuando el trabajo termine correctamente.
