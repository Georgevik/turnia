# Formularios de las tiendas

Respuestas para **la build actual**, que **lleva anuncios** (banner de AdMob, con el mensaje de
consentimiento de Google en el EEE) y registro con email, pero **todavía no suscripciones** (no
incluye Play Billing / StoreKit). Cuando entren, cambian las filas marcadas con ⚠︎.

---

## Google Play

### Acceso a la aplicación — *Contenido de la aplicación*

Toda la app pide iniciar sesión, pero el revisor puede crearse su propia cuenta. Elige
*Todas las funciones o algunas de ellas están restringidas* y añade estas instrucciones:

```text
Turnia needs an account, and reviewers can create their own: Continue with email → Create account (any email address, it is not verified), or sign in with Google. On first launch in the EEA or the UK, Google's ad consent message is shown before the app opens; any answer continues. To try the main features: 1) Groups tab → + to create a group (it needs at least one shift type). 2) Calendar tab → tap a day → + to add a shift. 3) Tap the shift → offer it for swap. Swaps between members need a second account joined to the group through the invitation link on the group screen. The ad banner appears after a few actions (adding events, opening calendars).
```

### Anuncios

| Pregunta | Respuesta |
|----------|-----------|
| ¿Tu aplicación contiene anuncios? | **Sí** (banner de AdMob) |

### ID de publicidad — *Contenido de la aplicación*

| Pregunta | Respuesta |
|----------|-----------|
| ¿Tu aplicación usa el ID de publicidad? | **Sí** |
| Finalidad | **Publicidad o marketing** |

El permiso `com.google.android.gms.permission.AD_ID` lo añade el SDK de anuncios; no hay que
declararlo en el manifiesto.

### Clasificación de contenido (cuestionario IARC)

| Pregunta | Respuesta |
|----------|-----------|
| Categoría | Todas las demás tipos de aplicaciones (utilidades, productividad…) |
| Violencia, sexo, lenguaje, drogas, apuestas | No |
| ¿Los usuarios pueden interactuar o intercambiar contenido? | **Sí** (grupos, calendario compartido) |
| ¿Comparte la ubicación del usuario con otros? | No |
| ¿Permite compras digitales? | No ⚠︎ |

### Público objetivo

| Pregunta | Respuesta |
|----------|-----------|
| Edades | **18 años o más** (la política dice «no pensada para menores de 16»; 18+ evita el formulario de familias) |
| ¿Puede atraer a niños? | No |

### Seguridad de los datos

| Pregunta | Respuesta |
|----------|-----------|
| ¿Recoge o comparte datos de usuario? | Sí |
| ¿Datos cifrados en tránsito? | Sí |
| ¿Los usuarios pueden pedir que se eliminen sus datos? | Sí |
| URL para eliminar la cuenta | `https://turnia.club/privacidad` (sección *Eliminación de datos*) |

Tipos de datos — **Recogidos** y **obligatorios** salvo donde se indica; *Compartido* solo donde lo
pone (el SDK de AdMob, según la guía de Google para la sección de seguridad de los datos):

| Categoría → tipo | Finalidad |
|------------------|-----------|
| Información personal → Nombre | Funciones de la app, Gestión de la cuenta |
| Información personal → Dirección de correo | Gestión de la cuenta |
| Información personal → IDs de usuario | Funciones de la app, Gestión de la cuenta |
| Calendario → Eventos del calendario | Funciones de la app |
| Mensajes → Otro contenido generado por el usuario (notas de eventos) · *opcional* | Funciones de la app |
| Actividad en la aplicación → Interacciones con la app | Analíticas |
| Información y rendimiento de la app → Registros de fallos | Analíticas |
| Información y rendimiento de la app → Diagnósticos | Analíticas |
| IDs del dispositivo u otros IDs (token de notificaciones, ID de publicidad) · **Compartido** | Funciones de la app, Publicidad o marketing, Analíticas |
| Ubicación → Ubicación aproximada (deducida de la IP por AdMob) · **Compartido** | Publicidad o marketing, Analíticas |

Marca también *Compartido* en *Interacciones con la app*, *Registros de fallos* y *Diagnósticos*,
con la finalidad *Publicidad o marketing*: AdMob los envía a Google para servir y medir anuncios.

⚠︎ Con suscripciones: *Información financiera → Historial de compras → Funciones de la app*.

---

## App Store

### Privacidad de la app — *App Store Connect → Privacidad de la app*

| Pregunta | Respuesta |
|----------|-----------|
| URL de la política de privacidad | `https://turnia.club/privacidad` |
| ¿Recopilas datos? | Sí |
| ¿Se usan para rastrear (tracking)? | **No** en ningún tipo: la app no pide App Tracking Transparency, así que AdMob no accede al IDFA |

Todos los tipos: **vinculados a la identidad del usuario** salvo donde se indica.

| Tipo de dato | Finalidad |
|--------------|-----------|
| Información de contacto → Nombre | Funcionalidad de la app |
| Información de contacto → Correo electrónico | Funcionalidad de la app |
| Contenido del usuario → Otro contenido del usuario (turnos, eventos, notas) | Funcionalidad de la app |
| Identificadores → ID de usuario | Funcionalidad de la app |
| Uso → Interacción con el producto | Análisis |
| Diagnóstico → Datos de fallos | Análisis · *no vinculado* |
| Diagnóstico → Datos de rendimiento | Análisis · *no vinculado* |
| Diagnóstico → Otros datos de diagnóstico | Análisis · *no vinculado* |
| Identificadores → ID de dispositivo | Publicidad de terceros, Análisis · *no vinculado* |
| Uso → Datos de publicidad | Publicidad de terceros, Análisis · *no vinculado* |
| Ubicación → Ubicación aproximada (deducida de la IP) | Publicidad de terceros, Análisis · *no vinculado* |

Añade también *Publicidad de terceros* como finalidad en *Uso → Interacción con el producto* y en
los tres tipos de *Diagnóstico*.

> Si algún día se pide App Tracking Transparency para servir anuncios personalizados con el IDFA,
> *ID de dispositivo* y *Datos de publicidad* pasan a usarse para rastrear, y hay que añadir
> `NSUserTrackingUsageDescription` al `Info.plist`.

⚠︎ Con suscripciones: *Compras → Historial de compras → Funcionalidad de la app*.

### Clasificación por edad

Todo **No / Ninguno** → **4+**, excepto:

| Pregunta | Respuesta |
|----------|-----------|
| Contenido generado por usuarios / comunicación entre usuarios | Sí (grupos privados por invitación) |
| Acceso web sin restricciones | No |

### Revisión de la app — *Información para la revisión*

| Campo | Valor |
|-------|-------|
| Inicio de sesión obligatorio | Desmarcado: el revisor puede crearse una cuenta con email desde la propia app |
| Contacto | Jorge Gonzalez · `geoviksoft@gmail.com` · *tu teléfono* |

Notas:

```text
Reviewers can create their own account: Continue with email → Create account (any email address, it is not verified), or use Sign in with Apple or Google. On first launch in the EEA or the UK, Google's ad consent message is shown before the app opens; any answer continues. To try the main features: 1) Groups tab → + to create a group (it needs at least one shift type). 2) Calendar tab → tap a day → + to add a shift. 3) Tap the shift → offer it for swap. Swapping between members needs a second account that joins the group through the invitation link shared from the group screen. Accounts can be deleted from Settings → My profile → Delete account.
```

### Cumplimiento de exportación (cifrado)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Usa cifrado? | Solo el estándar del sistema (HTTPS) → **exento** |

Para no responderlo en cada build, añade `ITSAppUsesNonExemptEncryption = NO` al `Info.plist`.
