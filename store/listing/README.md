# Fichas de las tiendas

| Archivo | Qué hay |
|---------|---------|
| [google-play.md](google-play.md) | Nombre, descripciones y novedades (es-ES, en-US, fr-FR, de-DE, it-IT), recursos gráficos, categoría y contacto |
| [app-store.md](app-store.md) | Nombre, subtítulo, texto promocional, descripción, palabras clave y novedades (es-ES, en-US, fr-FR, de-DE, it-IT), URLs, capturas y categoría |
| [formularios.md](formularios.md) | Acceso para revisión, clasificación de contenido, seguridad de los datos / privacidad de la app, cifrado |

Los textos solo cuentan lo que hace la build actual. Cuando lleguen los anuncios y Premium, hay que
revisar las filas ⚠︎ de *formularios.md* y añadir la suscripción a las descripciones.

## Capturas

Salen del modo demo (datos inventados, `demoModule`), en español, y se montan con
[`store/screenshots/generate.py`](../screenshots/generate.py). Las carpetas generadas no van a git.

1. **Android** (emulador de 1080 × 2424, app en español):

   ```bash
   adb shell run-as com.geoviksoft.turnia touch files/demo
   adb shell cmd locale set-app-locales com.geoviksoft.turnia --locales es-ES
   ```

   Barra de estado limpia con el *demo mode* de System UI (`clock -e hhmm 0941`, sin notificaciones).
2. **iOS** (iPhone 17 Pro):

   ```bash
   xcrun simctl status_bar booted override --time 9:41 --batteryState discharging --batteryLevel 100
   xcrun simctl launch booted com.geoviksoft.turnia.Turnia -TurniaDemo -AppleLanguages "(es)" -AppleLocale es_ES
   ```

3. Guarda cada pantalla en `store/screenshots/raw/{android,ios}/` con su nombre — `1_calendar`
   (mes), `2_day` (día 24 abierto, cadena de cambios), `3_colleagues` (*Cambios → De mis compañeros*),
   `4_requests` (*Cambios → Mis peticiones*), `5_group` (*Grupos → Urgencias*), `6_shared`
   (*Personas → Javier Ruiz*), `7_dark_calendar` (mes en modo oscuro) — y ejecuta:

   ```bash
   python3 store/screenshots/generate.py
   ```

## Pendiente: iPad

El target de iOS declara iPhone **y iPad** (`TARGETED_DEVICE_FAMILY = "1,2"`), así que App Store
Connect exigirá capturas de iPad 13" (2064 × 2752) antes de enviar a revisión. Dos opciones:

- **Solo iPhone**: `TARGETED_DEVICE_FAMILY = 1`. La interfaz es de teléfono; en iPad la app sigue
  pudiéndose instalar en modo compatibilidad y no piden capturas de iPad.
- **Mantener iPad**: capturar las mismas pantallas en el simulador *iPad Pro 13-inch* y añadir un
  destino `app-store-ipad` a `generate.py`.
