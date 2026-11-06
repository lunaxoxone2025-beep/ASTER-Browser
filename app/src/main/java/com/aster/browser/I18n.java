package com.aster.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight interface translation. English strings are the keys; Spanish is provided for the
 * whole interface. Unknown strings (page titles, URLs, user data) pass through unchanged.
 */
final class I18n {
    static boolean es;
    private static final Map<String, String> ES = new HashMap<>();

    private I18n() { }

    static void init(String pref) {
        if ("es".equals(pref)) es = true;
        else if ("en".equals(pref)) es = false;
        else es = "es".equals(Locale.getDefault().getLanguage());
    }

    static String languageName(String code) {
        if ("es".equals(code)) return "Español";
        if ("en".equals(code)) return "English";
        return t("Automatic (device language)");
    }

    static String t(String s) {
        if (!es || s == null) return s;
        String v = ES.get(s);
        return v == null ? s : v;
    }

    static CharSequence t(CharSequence s) {
        if (!es || s == null) return s;
        String v = ES.get(s.toString());
        return v == null ? s : v;
    }

    static CharSequence[] t(CharSequence[] items) {
        if (!es || items == null) return items;
        CharSequence[] out = new CharSequence[items.length];
        for (int i = 0; i < items.length; i++) out[i] = t(items[i]);
        return out;
    }

    static String f(String template, Object... args) {
        return String.format(Locale.getDefault(), t(template), args);
    }

    /** AlertDialog builder that translates every title, message, button and list item. */
    static final class Builder extends AlertDialog.Builder {
        Builder(Context c) { super(c); }

        @Override public AlertDialog.Builder setTitle(CharSequence title) { return super.setTitle(t(title)); }
        @Override public AlertDialog.Builder setMessage(CharSequence message) { return super.setMessage(t(message)); }
        @Override public AlertDialog.Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener l) { return super.setPositiveButton(t(text), l); }
        @Override public AlertDialog.Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener l) { return super.setNegativeButton(t(text), l); }
        @Override public AlertDialog.Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener l) { return super.setNeutralButton(t(text), l); }
        @Override public AlertDialog.Builder setItems(CharSequence[] items, DialogInterface.OnClickListener l) { return super.setItems(t(items), l); }
        @Override public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items, int checked, DialogInterface.OnClickListener l) { return super.setSingleChoiceItems(t(items), checked, l); }
        @Override public AlertDialog.Builder setMultiChoiceItems(CharSequence[] items, boolean[] checked, DialogInterface.OnMultiChoiceClickListener l) { return super.setMultiChoiceItems(t(items), checked, l); }
    }

    private static void es(String en, String translated) { ES.put(en, translated); }

    static {
        // ---- general
        es("Automatic (device language)", "Automático (idioma del dispositivo)");
        es("Language", "Idioma");
        es("Back", "Atrás");
        es("Forward", "Adelante");
        es("Home", "Inicio");
        es("Reload", "Recargar");
        es("Menu", "Menú");
        es("Tabs", "Pestañas");
        es("Private tabs", "Pestañas privadas");
        es("New tab", "Nueva pestaña");
        es("New private tab", "Nueva pestaña privada");
        es("Private tab", "Pestaña privada");
        es("Private window", "Ventana privada");
        es("Private", "Privada");
        es("Private ✓", "Privada ✓");
        es("👁  Private", "👁  Privada");
        es("Close tab", "Cerrar pestaña");
        es("Close all", "Cerrar todas");
        es("Close all tabs?", "¿Cerrar todas las pestañas?");
        es("Close", "Cerrar");
        es("Cancel", "Cancelar");
        es("Save", "Guardar");
        es("Edit", "Editar");
        es("Remove", "Quitar");
        es("Reset", "Restablecer");
        es("Clear", "Borrar");
        es("Add", "Añadir");
        es("Allow", "Permitir");
        es("Block", "Bloquear");
        es("Later", "Más tarde");
        es("Download", "Descargar");
        es("Restore", "Restaurar");
        es("Import", "Importar");
        es("Export", "Exportar");
        es("Folder", "Carpeta");
        es("Search", "Buscar");
        es("Search %s", "Buscar con %s");
        es("Search or type URL", "Busca o escribe una URL");
        es("search or type URL", "busca o escribe una URL");
        es("Search privately", "Buscar en privado");
        es("Search with your voice", "Busca con tu voz");
        es("Go to site", "Ir al sitio");
        es("Paste and go", "Pegar e ir");
        es("Copied", "Copiado");
        es("Share", "Compartir");
        es("Exit", "Salir");
        es("Close private", "Cerrar privada");
        es("Settings", "Ajustes");
        es("Themes", "Temas");
        es("Theme", "Tema");
        es("Halo", "Halo");
        es("ASTER Halo", "ASTER Halo");
        es("Rewards", "Rewards");
        es("translucent", "translúcido");
        es("%s applied", "%s aplicado");

        // ---- menu
        es("Bookmarks", "Marcadores");
        es("Stop", "Detener");
        es("Background play", "Segundo plano");
        es("History", "Historial");
        es("Downloads", "Descargas");
        es("Bookmark", "Guardar");
        es("Bookmarked", "Guardado");
        es("Find in page", "Buscar en página");
        es("Desktop site", "Versión escritorio");
        es("Reader", "Lectura");
        es("Translate", "Traducir");
        es("Night mode", "Modo noche");
        es("Add to Home", "A pantalla inicio");
        es("ASTER Core", "ASTER Core");
        es("Previous match", "Coincidencia anterior");
        es("Next match", "Siguiente coincidencia");
        es("Close find", "Cerrar búsqueda");

        // ---- toasts
        es("Opened in background", "Abierta en segundo plano");
        es("Tab limit reached — close some tabs first", "Límite de pestañas alcanzado: cierra alguna primero");
        es("Pop-up blocked", "Ventana emergente bloqueada");
        es("ASTER blocked a page with an invalid security certificate", "ASTER bloqueó una página con un certificado de seguridad no válido");
        es("Page reloaded after the renderer stopped", "Página recargada porque el motor se detuvo");
        es("No app can open this link", "Ninguna app puede abrir este enlace");
        es("Private windows need Android 9 or newer", "Las ventanas privadas requieren Android 9 o superior");
        es("Open a web page first", "Abre primero una página web");
        es("Bookmarks can't be changed from a private window", "Los marcadores no se pueden cambiar desde una ventana privada");
        es("Bookmark removed", "Marcador eliminado");
        es("Desktop site on", "Versión de escritorio activada");
        es("Desktop site off", "Versión de escritorio desactivada");
        es("Night mode on for websites", "Modo noche activado para las webs");
        es("Night mode off", "Modo noche desactivado");
        es("Open an article first", "Abre primero un artículo");
        es("No readable article found on this page", "No se encontró un artículo legible en esta página");
        es("Reader mode could not parse this page", "El modo lectura no pudo procesar esta página");
        es("Requires Android 8 or newer", "Requiere Android 8 o superior");
        es("Your launcher doesn't support shortcuts", "Tu launcher no admite accesos directos");
        es("Update filters from a normal window", "Actualiza los filtros desde una ventana normal");
        es("Updating EasyList, EasyPrivacy & AdGuard filters…", "Actualizando filtros de EasyList, EasyPrivacy y AdGuard…");
        es("Filters updated: %s rules", "Filtros actualizados: %s reglas");
        es("Couldn't update filters — keeping existing rules", "No se pudieron actualizar los filtros; se mantienen las reglas actuales");
        es("Background tabs are sleeping", "Las pestañas en segundo plano están dormidas");
        es("Cache cleared", "Caché borrada");
        es("Statistics reset", "Estadísticas restablecidas");
        es("Browsing data cleared", "Datos de navegación borrados");
        es("History cleared", "Historial borrado");
        es("Added to speed dial", "Añadido a la marcación rápida");
        es("Can't change saved data from a private window", "No se pueden cambiar datos guardados desde una ventana privada");
        es("Enter a valid web address", "Escribe una dirección web válida");
        es("Edit speed dial from a normal window", "Edita la marcación rápida desde una ventana normal");
        es("Refreshing Arcade…", "Actualizando Arcade…");
        es("Voice search isn't available on this device", "La búsqueda por voz no está disponible en este dispositivo");
        es("Use a normal window for backups", "Usa una ventana normal para las copias de seguridad");
        es("No file picker available", "No hay selector de archivos disponible");
        es("Backup exported", "Copia de seguridad exportada");
        es("Backup export failed", "Error al exportar la copia de seguridad");
        es("This file isn't a valid ASTER backup", "Este archivo no es una copia de seguridad válida de ASTER");
        es("Backup restored", "Copia de seguridad restaurada");
        es("This page generates its file in the browser; ASTER can't save it yet", "Esta página genera el archivo en el navegador; ASTER aún no puede guardarlo");
        es("Downloading %s", "Descargando %s");
        es("Could not start download", "No se pudo iniciar la descarga");
        es("Android permission was not granted", "Android no concedió el permiso");
        es("Permission removed", "Permiso eliminado");
        es("Open ASTER in Android Settings", "Abre ASTER en los Ajustes de Android");
        es("Storage permission is required on this Android version", "Esta versión de Android requiere permiso de almacenamiento");
        es("No downloads app found", "No se encontró la app de descargas");
        es("No app can open this file", "Ninguna app puede abrir este archivo");
        es("Open Android Settings → Apps → Default apps", "Abre Ajustes de Android → Aplicaciones → Apps predeterminadas");
        es("Tap the button for actions · swipe ← → for back/forward · swipe ↑ for tabs",
            "Toca el botón para ver acciones · desliza ← → para atrás/adelante · desliza ↑ para pestañas");
        es("Bing is now your search engine", "Bing es ahora tu buscador");
        es("Reset", "Restablecer");

        // ---- dialogs
        es("Private browsing unavailable", "Navegación privada no disponible");
        es("An isolated private profile requires Android 9 or newer.", "Un perfil privado aislado requiere Android 9 o superior.");
        es("Earn Microsoft Rewards points?", "¿Ganar puntos de Microsoft Rewards?");
        es("Searches made with Bing while you're signed in to your Microsoft account earn Rewards points you can redeem for Xbox gift cards and Game Pass. Use Bing as ASTER's search engine?\n\nYou can change this any time in Settings → Search engine.",
            "Las búsquedas con Bing mientras tienes iniciada tu cuenta de Microsoft dan puntos de Rewards que puedes canjear por tarjetas regalo de Xbox y Game Pass. ¿Usar Bing como buscador de ASTER?\n\nPuedes cambiarlo cuando quieras en Ajustes → Buscador.");
        es("No thanks", "No, gracias");
        es("Use Bing", "Usar Bing");
        es("Open in new tab", "Abrir en pestaña nueva");
        es("Open in background tab", "Abrir en segundo plano");
        es("Open in private tab", "Abrir en pestaña privada");
        es("Copy link", "Copiar enlace");
        es("Share link", "Compartir enlace");
        es("Open image in new tab", "Abrir imagen en pestaña nueva");
        es("Download image", "Descargar imagen");
        es("Copy image link", "Copiar enlace de la imagen");
        es("Blocking mode", "Modo de bloqueo");
        es("Standard", "Estándar");
        es("Aggressive", "Agresivo");
        es("Blocked on this page", "Bloqueado en esta página");
        es("Nothing blocked on this page yet.", "Aún no se ha bloqueado nada en esta página.");
        es("Start page wallpaper", "Fondo de la página de inicio");
        es("Navigation style", "Estilo de navegación");
        es("Bottom toolbar", "Barra inferior");
        es("Orbit button", "Botón Órbita");
        es("Address bar position", "Posición de la barra de direcciones");
        es("Top", "Arriba");
        es("Bottom", "Abajo");
        es("Text size", "Tamaño del texto");
        es("Search engine", "Buscador");
        es("Translate pages to", "Traducir páginas a");
        es("Reader style", "Estilo de lectura");
        es("Dark", "Oscuro");
        es("Light (sepia)", "Claro (sepia)");
        es("Customize start page", "Personalizar página de inicio");
        es("Halo statistics", "Estadísticas de Halo");
        es("Clock & date", "Reloj y fecha");
        es("Speed dial", "Marcación rápida");
        es("Arcade (free games)", "Arcade (juegos gratis)");
        es("Wallpaper…", "Fondo…");
        es("Per-site Halo", "Halo por sitio");
        es("No sites have custom Halo settings yet.", "Ningún sitio tiene ajustes de Halo personalizados todavía.");
        es("Reset this site to the global Halo settings?", "¿Restablecer este sitio a los ajustes globales de Halo?");
        es("Halo off", "Halo desactivado");
        es("scripts blocked", "scripts bloqueados");
        es("Clear browsing data", "Borrar datos de navegación");
        es("Browsing history", "Historial de navegación");
        es("Cookies and site data", "Cookies y datos de sitios");
        es("Cached images and files", "Imágenes y archivos en caché");
        es("Clear all history?", "¿Borrar todo el historial?");
        es("Remove from history", "Quitar del historial");
        es("Delete bookmark", "Eliminar marcador");
        es("Add to speed dial", "Añadir a marcación rápida");
        es("Edit bookmark", "Editar marcador");
        es("Name", "Nombre");
        es("URL", "URL");
        es("Web address", "Dirección web");
        es("Add to speed dial", "Añadir a marcación rápida");
        es("Edit shortcut", "Editar acceso");
        es("Backup & restore", "Copia de seguridad");
        es("Export bookmarks, history, speed dial, Halo settings, preferences and open tabs to a JSON file you control. Cookies and passwords are never included.",
            "Exporta marcadores, historial, marcación rápida, ajustes de Halo, preferencias y pestañas abiertas a un archivo JSON que tú controlas. Nunca incluye cookies ni contraseñas.");
        es("Restore backup?", "¿Restaurar la copia de seguridad?");
        es("This replaces your bookmarks, history, speed dial and settings with the backup's contents.",
            "Esto reemplaza tus marcadores, historial, marcación rápida y ajustes por el contenido de la copia.");
        es("Download file?", "¿Descargar archivo?");
        es("Save to your Downloads folder?", "¿Guardar en tu carpeta de Descargas?");
        es("Website permission", "Permiso del sitio web");
        es("Allow %s to use your %s?", "¿Permitir que %s use tu %s?");
        es("camera and microphone", "cámara y micrófono");
        es("camera", "cámara");
        es("microphone", "micrófono");
        es("Location permission", "Permiso de ubicación");
        es("Allow %s to access your location?", "¿Permitir que %s acceda a tu ubicación?");
        es("Site permissions", "Permisos de sitios");
        es("No website has been given camera, microphone or location access.", "Ningún sitio web tiene acceso a la cámara, el micrófono o la ubicación.");
        es("Android app permissions", "Permisos de la app en Android");
        es("Android settings", "Ajustes de Android");
        es("Camera", "Cámara");
        es("Microphone", "Micrófono");
        es("Location", "Ubicación");
        es("Language", "Idioma");

        // ---- tab switcher / pages
        es("Search bookmarks", "Buscar marcadores");
        es("Search history", "Buscar en el historial");
        es("No bookmarks yet. Tap ☆ in the menu to save a page.", "Aún no hay marcadores. Toca ☆ en el menú para guardar una página.");
        es("No history yet.", "Aún no hay historial.");
        es("Files you download with ASTER will appear here.", "Los archivos que descargues con ASTER aparecerán aquí.");
        es("Completed · %s", "Completado · %s");
        es("Failed", "Error");
        es("Paused", "En pausa");
        es("Downloading %d%% · %s", "Descargando %d%% · %s");
        es("Downloading…", "Descargando…");
        es("File", "Archivo");

        // ---- shields panel
        es("⛨  ASTER HALO", "⛨  ASTER HALO");
        es("New tab", "Nueva pestaña");
        es("Halo is off everywhere", "Halo desactivado en todas partes");
        es("Halo is ON for this site", "Halo ACTIVADO en este sitio");
        es("Halo is OFF for this site", "Halo DESACTIVADO en este sitio");
        es("Blocked all time", "Bloqueado en total");
        es("Data saved", "Datos ahorrados");
        es("Time saved", "Tiempo ahorrado");
        es("Ads & trackers", "Anuncios y rastreadores");
        es("%d ads · %d trackers on this page", "%d anuncios · %d rastreadores en esta página");
        es("Advanced controls for %s", "Controles avanzados para %s");
        es("Block fingerprinting", "Bloquear fingerprinting");
        es("Adds noise to canvas, audio and hardware details", "Añade ruido a canvas, audio y datos del hardware");
        es("Hide ad elements", "Ocultar elementos de anuncios");
        es("Cosmetic filtering of ad slots and banners", "Filtrado cosmético de huecos publicitarios y banners");
        es("Block scripts", "Bloquear scripts");
        es("Disables JavaScript on this site (may break pages)", "Desactiva JavaScript en este sitio (puede romper páginas)");
        es("Allow third-party cookies", "Permitir cookies de terceros");
        es("Fixes sign-in on sites that need it", "Arregla el inicio de sesión en sitios que lo necesitan");
        es("Global", "Global");
        es("Ads & trackers blocking", "Bloqueo de anuncios y rastreadores");
        es("Aggressive — also blocks first-party ads", "Agresivo: también bloquea anuncios propios del sitio");
        es("Standard — blocks third-party ads & trackers", "Estándar: bloquea anuncios y rastreadores de terceros");
        es("Blocked requests on this page", "Peticiones bloqueadas en esta página");
        es("See which domains were stopped", "Mira qué dominios se bloquearon");
        es("Updating filter lists…", "Actualizando listas de filtros…");
        es("Update filter lists", "Actualizar listas de filtros");
        es("%s network rules · %s cosmetic · updated %s", "%s reglas de red · %s cosméticas · actualizado %s");
        es("never", "nunca");
        es("Turn Halo off everywhere", "Desactivar Halo en todas partes");
        es("Turn Halo on everywhere", "Activar Halo en todas partes");
        es("ASTER runs on Android System WebView. Network and cosmetic rules block most ads and trackers, but video ads served from the same servers as the video (e.g. some YouTube ads) can't always be removed.",
            "ASTER funciona con Android System WebView. Las reglas de red y cosméticas bloquean la mayoría de anuncios y rastreadores, pero los anuncios de vídeo servidos desde los mismos servidores que el vídeo (como algunos de YouTube) no siempre se pueden quitar.");

        // ---- ASTER Core
        es("⚡  ASTER CORE", "⚡  ASTER CORE");
        es("RAM", "RAM");
        es("ASTER: %d MB", "ASTER: %d MB");
        es("Device: %d MB used of %d MB · %d of %d tabs active", "Dispositivo: %d MB usados de %d MB · %d de %d pestañas activas");
        es("Memory saver", "Ahorro de memoria");
        es("Puts the least-used tabs to sleep to free memory", "Duerme las pestañas menos usadas para liberar memoria");
        es("Max active tabs: %d", "Máximo de pestañas activas: %d");
        es("Open tabs", "Pestañas abiertas");
        es("Active", "Activa");
        es("Sleeping 💤", "Dormida 💤");
        es("Running in background", "En segundo plano");
        es("Sleep", "Dormir");
        es("Sleep all background tabs", "Dormir todas las pestañas de fondo");
        es("Network", "Red");
        es("Data saver", "Ahorro de datos");
        es("Don't load images — faster pages on slow or metered networks", "No carga imágenes: páginas más rápidas en redes lentas o con datos limitados");
        es("Clear web cache", "Borrar caché web");
        es("Free storage used by cached pages", "Libera el espacio usado por páginas en caché");
        es("Android doesn't let apps cap CPU or bandwidth, so ASTER Core manages memory by sleeping tabs and saves data by skipping images. WebView renderer memory is reported separately by Android.",
            "Android no deja a las apps limitar CPU ni ancho de banda, así que ASTER Core gestiona la memoria durmiendo pestañas y ahorra datos sin cargar imágenes. La memoria del motor WebView la mide Android por separado.");

        // ---- themes
        es("Dark · violet", "Oscuro · violeta");
        es("Glass · dark", "Cristal · oscuro");
        es("Glass · light", "Cristal · claro");
        es("Dark · crimson", "Oscuro · carmesí");
        es("Dark · purple", "Oscuro · morado");
        es("Dark · green", "Oscuro · verde");
        es("Dark · indigo", "Oscuro · índigo");
        es("Dark · amber", "Oscuro · ámbar");
        es("Dark · cyan", "Oscuro · cian");
        es("Graphite · orange", "Grafito · naranja");
        es("ASTER Core", "ASTER Core");
        es("Arcade", "Arcade");
        es("🎨  THEMES", "🎨  TEMAS");
        es("Start page", "Página de inicio");
        es("Wallpaper", "Fondo");
        es("Nebula (animated stars)", "Nebulosa (estrellas animadas)");
        es("Synthwave grid (animated)", "Cuadrícula synthwave (animada)");
        es("Aurora (animated)", "Aurora (animada)");
        es("Code rain (animated)", "Lluvia de código (animada)");
        es("Minimal gradient", "Degradado minimalista");

        // ---- settings
        es("Appearance", "Apariencia");
        es("Wallpaper, stats, clock, speed dial, Arcade", "Fondo, estadísticas, reloj, marcación rápida, Arcade");
        es("Night mode for websites", "Modo noche para webs");
        es("Darkens light web pages", "Oscurece las páginas claras");
        es("Interface sounds", "Sonidos de interfaz");
        es("Interface sound effects", "Efectos de sonido de la interfaz");
        es("Haptic feedback", "Vibración al tocar");
        es("Vibrate lightly on taps", "Vibra suavemente al tocar");
        es("Search suggestions", "Sugerencias de búsqueda");
        es("Ask your search engine for suggestions while typing", "Pide sugerencias a tu buscador mientras escribes");
        es("Halo & privacy", "Halo y privacidad");
        es("Block ads and trackers on all sites", "Bloquea anuncios y rastreadores en todos los sitios");
        es("Cosmetic filtering from EasyList & AdGuard", "Filtrado cosmético de EasyList y AdGuard");
        es("Randomize canvas, audio and hardware signals", "Aleatoriza canvas, audio y señales del hardware");
        es("%s network rules", "%s reglas de red");
        es("%d customized sites", "%d sitios personalizados");
        es("Camera, microphone and location", "Cámara, micrófono y ubicación");
        es("Reset Halo statistics", "Restablecer estadísticas de Halo");
        es("%s blocked so far", "%s bloqueados hasta ahora");
        es("History, cookies, site data and cache", "Historial, cookies, datos de sitios y caché");
        es("Clear site data on exit", "Borrar datos de sitios al salir");
        es("Wipe cookies and cache when you exit ASTER", "Borra cookies y caché al salir de ASTER");
        es("Browsing", "Navegación");
        es("Restore tabs on start", "Restaurar pestañas al iniciar");
        es("Desktop site by default", "Versión de escritorio por defecto");
        es("Pull to refresh", "Deslizar para recargar");
        es("Hide toolbar while scrolling", "Ocultar barra al desplazarse");
        es("More room for pages; scroll up to bring it back", "Más espacio para las páginas; desliza hacia arriba para que vuelva");
        es("Background media playback", "Reproducción en segundo plano");
        es("Keep audio and video playing when ASTER is in the background", "Sigue reproduciendo audio y vídeo con ASTER en segundo plano");
        es("Picture-in-picture", "Imagen en imagen");
        es("Full-screen videos keep playing in a small window when you leave ASTER", "Los vídeos a pantalla completa siguen en una ventana pequeña al salir de ASTER");
        es("YouTube & Rewards", "YouTube y Rewards");
        es("Remove YouTube ads", "Quitar anuncios de YouTube");
        es("Experimental: strips ad data from the player and blocks ad endpoints", "Experimental: elimina los datos de anuncios del reproductor y bloquea sus servidores");
        es("Microsoft Rewards", "Microsoft Rewards");
        es("Open your Rewards dashboard (search with Bing while signed in to earn points)", "Abre tu panel de Rewards (busca con Bing con la sesión iniciada para ganar puntos)");
        es("Translation language", "Idioma de traducción");
        es("Reader mode style", "Estilo del modo lectura");
        es("Data", "Datos");
        es("Bookmarks, history, speed dial, settings and tabs", "Marcadores, historial, marcación rápida, ajustes y pestañas");
        es("Set as default browser", "Usar como navegador predeterminado");
        es("Open Android's default apps settings", "Abre los ajustes de apps predeterminadas de Android");
        es("About", "Acerca de");
        es("Engine: Android System WebView · %s", "Motor: Android System WebView · %s");
        es("Check for updates", "Buscar actualizaciones");
        es("Looks for new releases on GitHub", "Busca nuevas versiones en GitHub");
        es("Source code", "Código fuente");

        // ---- updater
        es("ASTER %s is available", "ASTER %s está disponible");
        es("A newer version of ASTER Browser is ready. Download it now? Android will ask you to confirm the installation.",
            "Hay una versión nueva de ASTER Browser. ¿Descargarla ahora? Android te pedirá que confirmes la instalación.");
        es("ASTER %s is up to date", "ASTER %s está actualizado");
        es("Could not reach GitHub Releases", "No se pudo conectar con GitHub Releases");
        es("Update check failed", "Error al buscar actualizaciones");
        es("Downloading ASTER update…", "Descargando actualización de ASTER…");
        es("Could not download update", "No se pudo descargar la actualización");
        es("Allow ASTER updates", "Permitir actualizaciones de ASTER");
        es("Allow ASTER Browser to install apps, then come back to finish the update.", "Permite que ASTER Browser instale apps y vuelve para terminar la actualización.");
        es("Open settings", "Abrir ajustes");
        es("Android could not open the installer", "Android no pudo abrir el instalador");

        // ---- start page
        es("You are in a private window.", "Estás en una ventana privada.");
        es("ASTER won't save your history, session or site data from this window. Cookies and storage live in a separate, isolated profile and are wiped when you close it. Halo still blocks ads and trackers.",
            "ASTER no guardará el historial, la sesión ni los datos de sitios de esta ventana. Las cookies y el almacenamiento están en un perfil aislado que se borra al cerrarla. Halo sigue bloqueando anuncios y rastreadores.");
        es("Your internet provider, employer or the sites you visit can still see your activity.", "Tu proveedor de internet, tu empresa o los sitios que visitas aún pueden ver tu actividad.");
        es("EDIT", "EDITAR");
        es("ALL", "TODO");
        es("Free games", "Juegos gratis");
        es("REFRESH", "ACTUALIZAR");
        es("CUSTOMIZE", "PERSONALIZAR");
        es("Loading free games from GamerPower… tap Refresh if nothing appears.", "Cargando juegos gratis de GamerPower… toca Actualizar si no aparece nada.");
        es("FREE", "GRATIS");
        es("FREE · was %s", "GRATIS · antes %s");
        es("No free games right now. Check back later.", "Ahora no hay juegos gratis. Vuelve más tarde.");
        es("Free game", "Juego gratis");
    }
}
