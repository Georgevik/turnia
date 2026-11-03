# Dashboard de uso en Google Analytics

Esta guía monta, a mano y paso a paso, el dashboard de uso de Turnia en Google Analytics 4 (GA4).
No hace falta saber GA: cada paso dice dónde hacer clic, qué campo rellenar y qué valor escribir.

Los nombres de menús y campos están como los muestra **la consola de GA en español**, con el nombre
en inglés entre paréntesis cuando cambia, por si la tienes en inglés.

> Nada de esto se hace desde el repositorio ni lo hace un agente: lo haces tú en la consola, con
> esta guía delante.

## Qué responde

| Pregunta | Dónde |
|---|---|
| Usuarios únicos y nuevos por día | Informe *Resumen diario* |
| Eventos añadidos por día: turnos de un tipo, puntuales y de grupo | Informe *Eventos creados* |
| Grupos creados por día, y uso de cada funcionalidad (cambios, devoluciones, calendario compartido, mover a grupo…) | Informe *Uso por funcionalidad* |
| Método de login, idioma de la app, número de grupos | Informe *Usuarios* |
| Activación: de instalar a añadir el primer evento | Exploración *Embudo de activación* |
| Configuración de turnos y prompts de crecimiento | Exploración *Onboarding y prompts* |
| Retención D1, D7, D30 | Exploración *Retención* |

Los cuatro informes viven en una colección **"Turnia"** del menú izquierdo de *Informes*
(*Reports*); las tres exploraciones, en *Explorar* (*Explore*).

## Antes de empezar

- **Solo cuentan las builds de producción.** Las builds debug nunca envían nada a GA (ni siquiera a
  *DebugView*), y tampoco los dispositivos de Test Lab. Probar la app en el emulador no hace aparecer
  ningún dato.
- **Una definición personalizada solo se rellena desde que la registras**, nunca hacia atrás, y
  tarda **de 24 a 48 horas** en mostrar datos. Por eso lo primero es registrarlas todas.
- **Permisos**: para cambiar los ajustes de *Administrar* necesitas el rol *Administrador*
  (*Administrator*); para crear informes en la *Biblioteca* (*Library*), al menos *Editor*.
- Si un menú no está donde dice la guía (GA lo mueve de vez en cuando), usa la barra de búsqueda de
  arriba y escribe el nombre del ajuste, por ejemplo `definiciones personalizadas`.

## 1. Ajustes de la propiedad

Estos dos ajustes cambian lo que muestran todos los informes desde el momento en que los guardas,
así que van primero.

### 1.1 Conservar los datos 14 meses

Las exploraciones (embudo, retención…) solo ven los datos conservados, y por defecto GA los guarda
**2 meses**. Lo que se pierde no se recupera.

1. Abre [analytics.google.com](https://analytics.google.com) y elige la propiedad de Turnia arriba a
   la izquierda.
2. Haz clic en **Administrar** (*Admin*), el engranaje abajo a la izquierda.
3. En la columna *Propiedad* (*Property*), abre **Recogida y modificación de datos** (*Data
   collection and modification*) → **Retención de datos** (*Data retention*).
4. En **Conservación de los datos de eventos** (*Event data retention*), elige **14 meses**
   (*14 months*).
5. Haz clic en **Guardar** (*Save*).

Resultado: al volver a abrir la página, el desplegable sigue en *14 meses*.

### 1.2 Contar a cada persona una vez: identidad combinada

La app identifica al usuario al iniciar sesión, así que GA puede contar como una sola persona a
quien usa Turnia en dos móviles. Para eso:

1. En **Administrar** (*Admin*), columna *Propiedad*, abre **Visualización de datos** (*Data
   display*) → **Identidad para los informes** (*Reporting identity*).
2. Elige **Combinada** (*Blended*).
3. Haz clic en **Guardar** (*Save*).

Resultado: la opción *Combinada* queda marcada. Es un ajuste de visualización: no borra ni cambia
datos, y se puede cambiar en cualquier momento (ver
[Se aplican umbrales de datos](#se-aplican-umbrales-de-datos)).

## 2. Definiciones personalizadas

GA recibe todos los parámetros que envía la app, pero no los muestra en informes hasta que los
registras como **definición personalizada**. Regístralas todas ahora, aunque hoy solo uses
algunas: el día que quieras otra, ya tendrá datos acumulados.

Si una ya existe (la lista aparece al abrir la página), sáltala. No la crees dos veces.

### 2.1 Dimensiones de evento

Para cada fila de la tabla de abajo:

1. **Administrar** (*Admin*) → columna *Propiedad* → **Visualización de datos** (*Data display*) →
   **Definiciones personalizadas** (*Custom definitions*).
2. Pestaña **Dimensiones personalizadas** (*Custom dimensions*) → botón **Crear dimensión
   personalizada** (*Create custom dimension*).
3. Rellena:
   - **Nombre de la dimensión** (*Dimension name*): la columna *Nombre*, tal cual.
   - **Ámbito** (*Scope*): **Evento** (*Event*).
   - **Descripción** (*Description*): la columna *Descripción*.
   - **Parámetro de evento** (*Event parameter*): la columna *Parámetro*, tal cual. Si no aparece en
     el desplegable (GA solo lista los que ya ha recibido), escríbelo a mano.
4. Haz clic en **Guardar** (*Save*).

| Nombre | Parámetro | Descripción | Lo envía |
|---|---|---|---|
| `method` | `method` | Método de acceso: google, apple, email u other | `sign_up`, `login` |
| `auto_approve` | `auto_approve` | El grupo admite sin aprobación | `group_created` |
| `type_count` | `type_count` | Número de tipos de evento creados | `group_created`, `onboard_shift_completed` |
| `custom_type_count` | `custom_type_count` | Turnos propios añadidos en la configuración | `onboard_shift_completed` |
| `audience` | `audience` | A quién habla el prompt de compartir | `share_prompt_*` |
| `milestone` | `milestone` | Hito de eventos añadidos que lo mostró | `share_prompt_*` |
| `all_day` | `all_day` | Evento puntual de día completo | `one_off_event_created` |
| `multi_month` | `multi_month` | Evento puntual que abarca varios meses | `one_off_event_created` |
| `via` | `via` | Por dónde llegó: configuración inicial, panel de añadir, enlace… | `onboard_shift_*`, `invitation_opened` |
| `interacted` | `interacted` | Tocó el panel antes de salir | `onboard_shift_skipped`, `onboard_shift_completed` |
| `choice` | `choice` | Respuesta a "¿Trabajas en equipo?" | `onboard_team_answered` |
| `kind` | `kind` | Primer evento: de un tipo o puntual | `first_event_added` |
| `scope` | `scope` | Mover a grupo: solo uno o todos | `personal_events_moved` |
| `event_count` | `event_count` | Eventos movidos a un grupo | `personal_events_moved` |
| `skipped_count` | `skipped_count` | Eventos que no se movieron | `personal_events_moved` |
| `type` | `type` | Tipo de notificación abierta | `notification_opened` |
| `screen_name` | `screen_name` | Pantalla vista | `screen_view` |

`screen_name` es opcional: GA ya lo muestra en la dimensión **Título de la página y nombre de la
pantalla** (*Page title and screen name*). Si al guardarla GA da un error, sáltala; no se pierde
nada.

### 2.2 Dimensiones de usuario

Igual que antes, pero con **Ámbito** (*Scope*) **Usuario** (*User*). El último campo pasa a llamarse
**Propiedad de usuario** (*User property*).

| Nombre | Propiedad de usuario | Descripción |
|---|---|---|
| `group_count` | `group_count` | Grupos de los que es miembro, sin contar donde está revocado |
| `is_admin` | `is_admin` | Administra al menos un grupo |
| `app_language` | `app_language` | Idioma elegido en Preferencias, o `system` si sigue al dispositivo |

### 2.3 Métrica: eventos movidos

Una dimensión agrupa por valor; para **sumar** los eventos movidos a un grupo hace falta además una
métrica.

1. Misma página, pestaña **Métricas personalizadas** (*Custom metrics*) → **Crear métrica
   personalizada** (*Create custom metric*).
2. Rellena:
   - **Nombre de la métrica** (*Metric name*): `events_moved`
   - **Ámbito** (*Scope*): **Evento** (*Event*)
   - **Descripción** (*Description*): `Suma de eventos movidos a un grupo`
   - **Parámetro de evento** (*Event parameter*): `event_count`
   - **Unidad de medida** (*Unit of measurement*): **Estándar** (*Standard*)
3. Haz clic en **Guardar** (*Save*).

GA permite que el mismo parámetro sea a la vez dimensión y métrica, así que `event_count` sigue
también en la tabla de dimensiones.

Resultado: la pestaña *Dimensiones personalizadas* lista 20 (17 de evento y 3 de usuario), y
*Métricas personalizadas* lista 1.
