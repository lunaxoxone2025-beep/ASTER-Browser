# ASTER Browser — Android MVP 0.2.0

Prototipo nativo Android de ASTER Browser. Utiliza Android WebView como motor de renderizado; todavía no es un motor web independiente.

## Funciones incluidas
- Navegación web y búsqueda Google desde la barra de direcciones.
- Atrás, adelante, inicio y recargar.
- Varias pestañas WebView con selector y cierre.
- Barra de progreso de carga.
- Descargas con confirmación, notificación y acceso al gestor de descargas de Android.
- Buscar texto dentro de la página y compartir enlaces.
- Marcadores e historial locales.
- Interfaz oscura.

## Limitaciones de esta beta
- Las pestañas no se restauran al cerrar la aplicación.
- Los marcadores e historial se guardan localmente, sin sincronización.
- El modo incógnito, la sincronización y el bloqueo avanzado de rastreadores todavía no están implementados.
- Las descargas usan el gestor nativo de Android y se guardan en la carpeta Descargas.
- Android WebView utiliza el componente web del sistema Android; no es un motor propio.
- Esta versión compila un APK Android. La versión de escritorio se desarrollará después.

## Compilación
Abre Actions en GitHub y ejecuta el workflow "Build ASTER Android APK". Al terminar, descarga el artefacto ASTER-Browser-debug.
