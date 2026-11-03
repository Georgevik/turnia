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

## 3. La colección "Turnia"

Primero se crean los cuatro informes en la **Biblioteca** (*Library*) y después se juntan en una
colección que aparece en el menú izquierdo.

### Cómo se crea un informe de detalles

Los cuatro informes se crean igual; solo cambia lo que pones en el panel de la derecha:

1. En el menú izquierdo, **Informes** (*Reports*) → abajo del todo, **Biblioteca** (*Library*).
   Si no la ves, te falta el rol *Editor*.
2. Debajo de las colecciones, en la tabla *Informes* (*Reports*), haz clic en **Crear nuevo
   informe** (*Create new report*) → **Crear informe de detalles** (*Create detail report*) →
   **Crear desde cero** (*Blank*).
3. En el panel de la derecha, sección **Datos del informe** (*Report data*):
   - **Dimensiones** (*Dimensions*) → **Añadir dimensión** (*Add dimension*): busca la dimensión
     por su nombre y márcala. Con varias, la que tiene el tic de **Establecer como predeterminada**
     (*Set as default*) en su menú `⋮` es la que se ve al abrir el informe.
   - **Métricas** (*Metrics*) → **Añadir métrica** (*Add metric*): igual.
   - **Filtro del informe** (*Report filter*) → **Añadir filtro** (*Add filter*): elige la
     **Dimensión** (*Dimension*), el **Tipo de coincidencia** (*Match type*) y el **Valor**
     (*Value*), y pulsa **Aplicar** (*Apply*).
4. En **Visualizaciones del informe** (*Report visualizations*), deja solo el gráfico que dice cada
   informe (con el ojo de los demás tachado).
5. Arriba a la derecha, **Guardar** (*Save*) → **Guardar como informe nuevo** (*Save as a new
   report*), escribe el **Nombre del informe** (*Report name*) y pulsa **Guardar** (*Save*).
6. **Volver** (*Back*), la flecha arriba a la izquierda, para regresar a la Biblioteca.

### 3.1 Resumen diario

Usuarios únicos y nuevos, día a día.

| Campo | Valor |
|---|---|
| Dimensiones | **Fecha** (*Date*) |
| Métricas | **Usuarios activos** (*Active users*), **Usuarios nuevos** (*New users*) |
| Filtro | ninguno |
| Gráfico | **Gráfico de líneas** (*Line chart*) |
| Nombre del informe | `Resumen diario` |

Resultado: una línea con los usuarios activos de cada día y una tabla con una fila por día. Haz clic
en el título de la columna *Fecha* para ordenar del más reciente al más antiguo.

La fila de **Total** de arriba no es la suma de los días: son los usuarios únicos de todo el periodo
elegido. Quien abre la app tres días cuenta una vez en el total y una vez en cada uno de esos días.

### 3.2 Eventos creados

Cuántos eventos se añaden al calendario, y de qué clase.

| Campo | Valor |
|---|---|
| Dimensiones | **Nombre del evento** (*Event name*) |
| Métricas | **Recuento de eventos** (*Event count*), **Usuarios totales** (*Total users*), **Recuento de eventos por usuario activo** (*Event count per active user*) |
| Filtro | *Nombre del evento* · **coincide con la expresión regular** (*matches regex*) · el valor de abajo |
| Gráfico | **Gráfico de líneas** (*Line chart*) |
| Nombre del informe | `Eventos creados` |

Valor del filtro, en una sola línea:

```
personal_event_created|one_off_event_created|group_event_created
```

Resultado: tres filas y tres líneas, una por clase de evento:

- `personal_event_created`: turnos de un tipo propio (los "normales");
- `one_off_event_created`: eventos puntuales;
- `group_event_created`: eventos de grupo.

La fila de **Total** es el número de **eventos añadidos** en el periodo. *Recuento de eventos por
usuario activo* dice cuántos añade, de media, cada usuario que añade alguno. Para la media sobre
**todos** los usuarios, divide el total entre los *Usuarios activos* del *Resumen diario* en el
mismo periodo.

Para ver los números de cada día en la tabla, pulsa el **+** junto al título de la columna *Nombre
del evento* y elige **Fecha** (*Date*) como dimensión secundaria.

Los eventos **movidos** a un grupo no están aquí a propósito: ya se contaron cuando se crearon como
turnos propios, y sumarlos los contaría dos veces. Salen en *Uso por funcionalidad*.

### 3.3 Uso por funcionalidad

Grupos creados por día, y cuánto se usa cada funcionalidad.

| Campo | Valor |
|---|---|
| Dimensiones | **Nombre del evento** (*Event name*) |
| Métricas | **Recuento de eventos** (*Event count*), **Usuarios totales** (*Total users*), `events_moved` |
| Filtro | *Nombre del evento* · **coincide con la expresión regular** (*matches regex*) · el valor de abajo |
| Gráfico | **Gráfico de líneas** (*Line chart*) |
| Nombre del informe | `Uso por funcionalidad` |

Valor del filtro, en una sola línea:

```
swap_.*|.*calendar.*|personal_events_moved|.*notes_saved|group_(created|invite_shared|left|deleted)|invitation_opened|join_.*|member_removed|.*_event_type_created|notification_opened|account_deleted
```

Resultado: una fila por evento. Para leerlas:

| Funcionalidad | Eventos |
|---|---|
| Grupos | `group_created` (grupos creados), `group_invite_shared`, `invitation_opened`, `join_group_requested`, `join_group` (solicitud aceptada), `join_request_rejected`, `group_left`, `member_removed`, `group_deleted` |
| Cambios de turno | `swap_offered`, `swap_withdrawn`, `swap_taken`, `swap_take_lost` (otro llegó antes) |
| Devoluciones | `swap_returned` |
| Calendario compartido | `calendar_shared`, `calendar_share_revoked`, `shared_calendar_viewed`, `shared_calendar_hidden` |
| Mover a grupo | `personal_events_moved` (una vez por movimiento); los eventos movidos son su columna `events_moved` |
| Notas | `event_notes_saved` (turno propio), `group_event_notes_saved` (evento de grupo) |
| Tipos de turno | `personal_event_type_created`, `group_event_type_created` |
| Otros | `notification_opened`, `account_deleted` |

**Grupos creados por día**: en la tabla, marca solo la casilla de la fila `group_created` y el
gráfico dibuja únicamente esa línea. Lo mismo sirve para cualquier otro evento (hasta cinco a la
vez).

**Eventos de grupo creados contando los movidos**: `group_event_created` en *Eventos creados* más el
`events_moved` de la fila `personal_events_moved` de aquí.

### 3.4 Usuarios

Quién usa la app: método de acceso, idioma y grupos.

| Campo | Valor |
|---|---|
| Dimensiones | `app_language` (predeterminada), `method`, `group_count`, `is_admin`, **Plataforma** (*Platform*) |
| Métricas | **Usuarios activos** (*Active users*), **Usuarios nuevos** (*New users*) |
| Filtro | ninguno |
| Gráfico | **Gráfico de barras** (*Bar chart*) |
| Nombre del informe | `Usuarios` |

Resultado: una barra por idioma. Para cambiar de desglose, usa el desplegable del título de la
primera columna de la tabla y elige otra dimensión.

- `app_language`: `system` si la app sigue al idioma del móvil; si no, `en`, `es`, `fr`, `de` o `it`.
- `method`: `google`, `apple`, `email` u `other`. Solo se envía al iniciar sesión o al crear la
  cuenta, así que quien en el periodo solo abrió la app con la sesión ya iniciada sale como
  `(not set)`. Es normal: mira el resto de filas.
- `group_count`: `0` es quien usa Turnia sin grupo; `1` o más, con equipo.
- `is_admin`: `true` si administra al menos un grupo.

### 3.5 Juntar los informes en la colección

1. En la **Biblioteca** (*Library*), arriba, haz clic en **Crear nueva colección** (*Create new
   collection*) → **Colección en blanco** (*Blank*).
2. Arriba a la izquierda, cambia *Colección sin título* (*Untitled collection*) por `Turnia`.
3. Haz clic en **Crear nuevo tema** (*Create new topic*) y llámalo `Uso`.
4. De la lista **Informes** (*Reports*) de la derecha, arrastra al tema `Uso`, en este orden:
   `Resumen diario`, `Eventos creados`, `Uso por funcionalidad`, `Usuarios`. Usa la búsqueda de la
   lista si no los ves.
5. Haz clic en **Guardar** (*Save*) → **Volver a la biblioteca** (*Back to library*).
6. En la tarjeta **Turnia**, abre el menú `⋮` → **Publicar** (*Publish*).

Resultado: en el menú izquierdo de *Informes* aparece **Turnia** → **Uso**, con los cuatro informes
dentro. Arriba a la derecha de cada uno eliges el periodo; para el acumulado desde el lanzamiento,
elige un intervalo personalizado (*Custom*) que empiece ese día.

## 4. Exploraciones

Los embudos y la retención solo existen en **Explorar** (*Explore*), en el menú izquierdo. Una
exploración se **guarda sola** al hacer cambios; solo hay que ponerle nombre. Las exploraciones son
personales: si alguien más del equipo tiene que verlas, en la lista de exploraciones, menú `⋮` →
**Compartir** (*Share*).

En todas, la columna de la izquierda (**Variables**) tiene lo que se puede usar, y la del centro
(**Configuración de la pestaña**, *Tab settings*) lo que se está usando. Para añadir una dimensión o
métrica a *Variables*, pulsa el **+** junto a **Dimensiones** (*Dimensions*) o **Métricas**
(*Metrics*), búscala, márcala y pulsa **Importar** (*Import*).

### 4.1 Embudo de activación

Cuántos llegan desde la instalación hasta añadir su primer evento, y dónde se quedan.

1. **Explorar** (*Explore*) → **Exploración de embudo** (*Funnel exploration*).
2. Arriba a la izquierda, cambia *Exploración sin título* (*Untitled exploration*) por
   `Embudo de activación`.
3. En **Intervalo de fechas** (*Date range*), elige **Últimos 90 días** (*Last 90 days*).
4. En *Configuración de la pestaña*, junto a **Pasos** (*Steps*), pulsa el lápiz. Se abre
   **Editar los pasos del embudo** (*Edit funnel steps*). Borra los pasos de ejemplo y crea estos
   cuatro: para cada uno, escribe el nombre, pulsa **Añadir nueva condición** (*Add new
   condition*), busca el evento por su nombre y elígelo. **Añadir paso** (*Add step*) crea el
   siguiente.

   | Paso | Nombre | Condición |
   |---|---|---|
   | 1 | `Instalación` | evento `first_open` |
   | 2 | `Cuenta creada` | evento `sign_up` |
   | 3 | `Configuración de turnos` | evento `onboard_shift_completed` **O** (*OR*) evento `onboard_shift_skipped` |
   | 4 | `Primer evento` | evento `first_event_added` |

   Para el **O** del paso 3: tras elegir `onboard_shift_completed`, pulsa **O** (*Or*) a su derecha
   y elige `onboard_shift_skipped`.
5. Pulsa **Aplicar** (*Apply*).
6. Deja **Hacer que el embudo sea abierto** (*Make open funnel*) desactivado: así cada paso solo
   cuenta a quien pasó por los anteriores.

Resultado: cuatro barras que bajan de izquierda a derecha, y debajo una tabla con el porcentaje que
pasa de cada paso al siguiente (**Tasa de finalización**, *Completion rate*) y el que abandona
(**Tasa de abandono**, *Abandonment rate*).

- Para ver la evolución por día, cambia **Visualización** (*Visualization*) a **Embudo de
  tendencias** (*Trended funnel*).
- Para separar quien completa la configuración de quien la salta, mira la pestaña *Configuración de
  turnos* de la exploración siguiente.

Ten en cuenta al leerlo:

- `sign_up` solo se envía al **crear** una cuenta. Quien reinstala y entra con una cuenta que ya
  tenía envía `login` y se queda en el paso 1.
- `first_event_added` se envía **una vez por móvil**: un segundo dispositivo o una reinstalación lo
  envían otra vez.
- Un usuario que no vio la configuración de turnos (ya tenía turnos o grupo) no pasa del paso 3.

### 4.2 Onboarding y prompts

Qué hace la gente con la configuración de turnos, con la pregunta de si trabaja en equipo y con el
prompt de compartir. Es una exploración con tres pestañas.

**Primera pestaña:**

1. **Explorar** (*Explore*) → **Exploración de formato libre** (*Free form*).
2. Cambia el nombre de la exploración por `Onboarding y prompts`, y el **Intervalo de fechas**
   (*Date range*) por **Últimos 90 días** (*Last 90 days*).
3. En *Variables*, importa las dimensiones **Nombre del evento** (*Event name*), `via`,
   `interacted`, `choice`, `audience` y `milestone`, y las métricas **Recuento de eventos** (*Event
   count*) y **Usuarios totales** (*Total users*).
4. Haz doble clic en el nombre de la pestaña (*Formato libre 1*) y llámala
   `Configuración de turnos`.
5. En *Configuración de la pestaña*:
   - **Visualización** (*Visualization*): **Tabla** (*Table*).
   - **Filas** (*Rows*): arrastra **Nombre del evento**, `via` e `interacted`, en ese orden.
   - **Columnas** (*Columns*): vacío. Si hay algo, quítalo con la `x`.
   - **Valores** (*Values*): arrastra **Recuento de eventos** y **Usuarios totales**.
   - **Filtros** (*Filters*): arrastra **Nombre del evento**, elige **coincide con la expresión
     regular** (*matches regex*), escribe `onboard_shift_.*` y pulsa **Aplicar** (*Apply*).

Resultado: filas como `onboard_shift_shown · onboarding`, `onboard_shift_completed · onboarding ·
true` u `onboard_shift_skipped · add_pane · false`. En `via`, `onboarding` es la configuración tras
crear la cuenta y `add_pane` la que se abre desde el panel de añadir. En `interacted`, `false`
significa que salió sin tocar nada. La tasa de configuración es `onboard_shift_completed` entre
`onboard_shift_shown`.

En las filas de `onboard_shift_shown`, `interacted` sale `(not set)`: ese evento no lo lleva. Es
normal.

**Segunda pestaña:**

1. En la pestaña `Configuración de turnos`, menú de la flecha junto a su nombre → **Duplicar**
   (*Duplicate*). Llama a la copia `Equipo`.
2. En **Filas** (*Rows*), quita `via` e `interacted` y arrastra `choice`.
3. Cambia el valor del filtro a `onboard_team_.*`.

Resultado: `onboard_team_shown` (veces que se mostró la pregunta) y `onboard_team_answered` por
`choice`: `create` (crear un grupo), `join` (tengo un código) o `dismissed` (ahora no).

**Tercera pestaña:**

1. Duplica otra vez la pestaña `Configuración de turnos` y llama a la copia `Compartir`.
2. En **Filas** (*Rows*), quita `via` e `interacted` y arrastra `audience` y `milestone`.
3. Cambia el valor del filtro a `share_prompt_.*`.

Resultado: para cada hito (`milestone`, el número de eventos añadidos que lo mostró) y cada público
(`audience`: `coworkers` si el evento que llegó al hito era un turno, `friends` si era puntual), cuántas veces se mostró (`share_prompt_shown`), se
compartió (`share_prompt_shared`) o se descartó (`share_prompt_dismissed`).

Si una pestaña sale vacía, el prompt puede estar apagado: el de compartir y el de equipo se
encienden desde Remote Config (`sharePromptEnabled`, `teamPromptEnabled`).

### 4.3 Retención

Qué parte de quienes instalan un día vuelve 1, 7 y 30 días después.

1. **Explorar** (*Explore*) → **Exploración de cohortes** (*Cohort exploration*).
2. Cambia el nombre por `Retención` y el **Intervalo de fechas** (*Date range*) por **Últimos 90
   días** (*Last 90 days*). Para ver el D30 hacen falta más de 30 días de datos.
3. En *Configuración de la pestaña*:
   - **Inclusión en la cohorte** (*Cohort inclusion*): **Primer contacto (fecha de adquisición)**
     (*First touch (acquisition date)*).
   - **Criterios de regreso** (*Return criteria*): **Cualquier evento** (*Any event*).
   - **Granularidad de la cohorte** (*Cohort granularity*): **Diaria** (*Daily*).
   - **Cálculo** (*Calculation*): **Estándar** (*Standard*).
   - **Métrica** (*Metric*): **Usuarios activos** (*Active users*).

Resultado: una tabla en la que cada fila es el día en que un grupo de usuarios llegó por primera
vez, y cada columna, los días que han pasado (**Día 0**, **Día 1**, …). Cuanto más oscura la celda,
más usuarios volvieron.

- **D1** es la columna *Día 1*; **D7**, *Día 7*; **D30**, *Día 30* (desplázate a la derecha).
- La retención en porcentaje es la celda entre la de *Día 0* de la misma fila. La fila **Todos los
  usuarios** (*All users*) de arriba da la media de todas las cohortes.
- Si es difícil de leer día a día, cambia la granularidad a **Semanal** (*Weekly*): la columna
  *Semana 1* es una buena aproximación del D7.

Para GA, "volver" es abrir la app, no añadir un turno.

Para un vistazo rápido sin montar nada, GA también trae el informe **Informes** (*Reports*) →
**Interacción** (*Engagement*) → **Retención** (*Retention*). La exploración es la que da exactamente
D1, D7 y D30.
