# ASTER Browser — Android MVP 0.1.0

Primer prototipo nativo Android de ASTER Browser. Utiliza Android WebView como motor de renderizado; no es un motor web independiente.

## Funciones incluidas
- Navegación web mediante HTTPS.
- Búsqueda Google desde la barra de direcciones.
- Atrás, adelante, inicio y recargar.
- Varias pestañas WebView.
- Marcadores e historial guardados localmente.
- Interfaz oscura adaptable.

## Limitaciones de esta beta
- Es un MVP: todavía no incluye gestor de descargas, modo incógnito real, sincronización, bloqueo avanzado de rastreadores ni sincronización de pestañas.
- La lista de pestañas es básica y no persiste al cerrar la aplicación.
- Android WebView usa el componente web del sistema Android.
- Este proyecto compila un APK Android. La versión de escritorio se desarrollará después reutilizando el diseño y la lógica de interfaz con un contenedor de escritorio.

## Compilación
Abre Actions en GitHub y ejecuta el workflow "Build ASTER Android APK". Al terminar, descarga el artefacto ASTER-Browser-debug.
