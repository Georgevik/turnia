# Fichas de las tiendas

| Archivo | Qué hay |
|---------|---------|
| [google-play.md](google-play.md) | Nombre, descripciones y novedades (es-ES, en-US, fr-FR, de-DE, it-IT), recursos gráficos, categoría y contacto |
| [app-store.md](app-store.md) | Nombre, subtítulo, texto promocional, descripción, palabras clave y novedades (es-ES, en-US, fr-FR, de-DE, it-IT), URLs, capturas y categoría |
| [formularios.md](formularios.md) | Acceso para revisión, clasificación de contenido, seguridad de los datos / privacidad de la app, cifrado |

Los textos solo cuentan lo que hace la build actual. Cuando lleguen los anuncios y Premium, hay que
revisar las filas ⚠︎ de *formularios.md* y añadir la suscripción a las descripciones.

## Capturas

Salen del modo demo (datos inventados, `demoModule`, que ya habla los cinco idiomas) y se montan
con [`store/screenshots/generate.py`](../screenshots/generate.py), que lleva los titulares en
es, en, fr, de e it. Las carpetas generadas no van a git.

1. **Android** (emulador de 1080 × 2424):

   ```bash
   adb shell run-as com.geoviksoft.turnia touch files/demo
   adb shell cmd locale set-app-locales com.geoviksoft.turnia --locales es-ES
   ```

   Barra de estado limpia con el *demo mode* de System UI (`clock -e hhmm 0941`, sin notificaciones).
2. **iOS** (iPhone 17 Pro y iPad Pro 13-inch):

   ```bash
   xcrun simctl status_bar booted override --time 9:41 --batteryState discharging --batteryLevel 100
   xcrun simctl launch booted com.geoviksoft.turnia.Turnia -TurniaDemo -AppleLanguages "(es)" -AppleLocale es_ES
   ```

   La primera vez aparecen el consentimiento de anuncios de prueba y el permiso de notificaciones:
   recházalos antes de capturar.

3. Guarda cada pantalla en `store/screenshots/raw/{android,ios,ipad}/{idioma}/` con su nombre —
   `1_calendar` (mes actual), `2_day` (el día 4 del mes siguiente abierto: la cadena Yo → Sofía →
   Carlos), `3_colleagues` (*Cambios → De mis compañeros*), `4_requests` (*Cambios → Mis
   peticiones*), `5_group` (*Grupos → Urgencias*), `6_shared` (*Personas → Javier Ruiz*),
   `7_dark_calendar` (el mes siguiente en modo oscuro) — y ejecuta:

   ```bash
   python3 store/screenshots/generate.py
   ```

   Un idioma al que le falte alguna captura se salta sin error.
