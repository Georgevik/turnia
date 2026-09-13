# Formularios de las tiendas

Respuestas para **la build actual (1.01 / 1.0)**, que todavía **no lleva anuncios ni suscripciones**
(no incluye AdMob ni Play Billing / StoreKit). Cuando entren, cambian las filas marcadas con ⚠︎.

---

## Google Play

### Acceso a la aplicación — *Contenido de la aplicación*

Toda la app pide iniciar sesión, pero con la cuenta de Google del propio revisor. Elige
*Todas las funciones o algunas de ellas están restringidas* y añade estas instrucciones:

```text
Turnia only offers Google Sign-In (and Sign in with Apple); there is no username/password. Please sign in with any Google account. To try the main features: 1) Groups tab → + to create a group (it needs at least one shift type). 2) Calendar tab → tap a day → + to add a shift. 3) Tap the shift → offer it for swap. Swaps between members need a second account joined to the group through the invitation link on the group screen.
```

### Anuncios

| Pregunta | Respuesta |
|----------|-----------|
| ¿Tu aplicación contiene anuncios? | **No** ⚠︎ |

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

Tipos de datos — todos **Recogidos**, **no Compartidos**, **obligatorios** salvo donde se indica:

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
| IDs del dispositivo u otros IDs (token de notificaciones) | Funciones de la app |

⚠︎ Con AdMob: añade *IDs del dispositivo → Compartido → Publicidad o marketing*.
⚠︎ Con suscripciones: *Información financiera → Historial de compras → Funciones de la app*.

---

## App Store

### Privacidad de la app — *App Store Connect → Privacidad de la app*

| Pregunta | Respuesta |
|----------|-----------|
| URL de la política de privacidad | `https://turnia.club/privacidad` |
| ¿Recopilas datos? | Sí |
| ¿Se usan para rastrear (tracking)? | **No** en ningún tipo ⚠︎ |

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

⚠︎ Con AdMob: *Identificadores → ID de dispositivo* y *Datos de uso → Datos de publicidad*, con
rastreo **Sí**, y hará falta el aviso de App Tracking Transparency.
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
| Inicio de sesión obligatorio | Desmarcado: no hay usuario y contraseña, solo Google o Apple |
| Contacto | Jorge Gonzalez · `geoviksoft@gmail.com` · *tu teléfono* |

Notas:

```text
Turnia has no username/password login: please use Sign in with Apple (or Google). To try the main features: 1) Groups tab → + to create a group (it needs at least one shift type). 2) Calendar tab → tap a day → + to add a shift. 3) Tap the shift → offer it for swap. Swapping between members needs a second account that joins the group through the invitation link shared from the group screen. Accounts can be deleted from Settings → About → Delete account.
```

### Cumplimiento de exportación (cifrado)

| Pregunta | Respuesta |
|----------|-----------|
| ¿Usa cifrado? | Solo el estándar del sistema (HTTPS) → **exento** |

Para no responderlo en cada build, añade `ITSAppUsesNonExemptEncryption = NO` al `Info.plist`.
