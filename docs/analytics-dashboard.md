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
