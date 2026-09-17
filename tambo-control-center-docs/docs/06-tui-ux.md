# Diseño de la TUI

## Objetivo visual

Una sola pantalla operativa. El usuario navega por paneles y cambia la selección, pero mantiene contexto del stack.

TamboUI soporta layouts por constraints, grids, dock, listas, tablas, gauges, tabs y scrollbars. Para este proyecto conviene empezar con Toolkit DSL o TuiRunner según cuánto control necesitemos sobre eventos. La documentación actual recomienda Toolkit DSL para la mayoría de aplicaciones, pero TuiRunner ofrece un event loop más explícito.

Para aprender, una buena estrategia es comenzar con Toolkit DSL y bajar de nivel solo si una interacción concreta lo exige.

## Layout principal

```text
┌ project ──────────────────────────────────────────────────────────────┐
│ Services                        │ Details                             │
│                                │                                     │
│ ● gateway       running        │ Name       challenge               │
│ ● challenge     running        │ Runtime    running                 │
│ ! bank          running        │ Health     unhealthy               │
│ ○ kafka         not-created    │ Image      challenge:dev           │
│ ● postgres      running        │ Ports      8081 -> 8080            │
│                                │ Instances  1                        │
├────────────────────────────────┴─────────────────────────────────────┤
│ Logs [selected: challenge]                              follow: on   │
│ 16:40:01 ...                                                        │
│ 16:40:02 ...                                                        │
│ 16:40:03 ...                                                        │
├─────────────────────────────────────────────────────────────────────┤
│ status/help bar                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Focus

Paneles focusables:

```text
SERVICES
DETAILS
LOGS
```

`Tab` y `Shift+Tab` pueden mover foco. También se pueden usar shortcuts directos si la UX lo pide.

El borde enfocado cambia de estilo. No usar diez colores para comunicar estado.

## Selección de service

En SERVICES:

```text
j / Down   next
k / Up     previous
Enter      focus details or default action, decisión posterior
```

Yo evitaría que `Enter` ejecute `up` o `restart`. Es demasiado fácil activar una operación destructiva por costumbre de navegación.

## Lifecycle keybindings

Propuesta inicial:

```text
u   up selected
s   stop selected
r   restart selected

U   up all
S   stop all
R   restart all

l   toggle selected/all logs
q   quit
?   help
```

El uso de mayúsculas para acciones globales hace visible que tienen mayor alcance.

`down` no recibe un shortcut simple en V1.

## Logs

En LOGS:

```text
j/k or arrows     scroll
G                 jump to end + follow
f                 toggle follow/freeze
c                 clear local buffer
l                 selected/all
```

Clear solo vacía el buffer local. No borra logs de Docker.

### Follow behavior

Cuando follow está activo, cada línea nueva mantiene el viewport al final.

Si el usuario scrollea hacia arriba, se puede desactivar follow automáticamente. Esto es más natural que pelear contra el scroll.

## Modo selected vs all

Header explícito:

```text
Logs [selected: challenge]
```

o:

```text
Logs [all services]
```

En modo all, cada línea debe identificar service.

Podemos asignar estilos estables por service más adelante, pero no hace falta para V1. Un prefix textual alcanza.

## Paneles ajustables

### V1 recomendada

Mantener ratios en estado:

```text
services/details vertical split
upper/logs horizontal split
```

Cuando el usuario entra en resize mode:

```text
Ctrl+h / Ctrl+l   move vertical divider
Ctrl+j / Ctrl+k   move horizontal divider
```

Otra posibilidad es usar `[` `]` para ancho y `{` `}` para altura. Elegir después de probar.

### Por qué teclado primero

- fácil de testear;
- no depende de detalles de mouse drag;
- funciona por SSH;
- mantiene la filosofía TUI;
- permite aprender el layout state antes de agregar interacción extra.

Mouse resize puede agregarse cuando el layout ya esté bien modelado.

## Responsive layout

No asumir terminal 160x50.

Definir breakpoints simples:

### Wide

```text
services | details
------------------
logs
```

### Narrow

```text
services
--------
details
--------
logs
```

### Too small

Mostrar mensaje claro:

```text
Terminal too small. Minimum recommended size: 80x24.
```

No intentar comprimir todos los widgets hasta volverlos ilegibles.

## Details panel

V1:

```text
service
runtime
health
operation
image
container count
ports
exit code if applicable
```

V1.1:

```text
CPU
memory
network I/O
uptime
profiles
dependencies
```

## Status bar

Debe mostrar estado global útil, no decoración.

Ejemplos:

```text
challenge-platform | 5/6 running | logs:selected | last refresh 1.2s
```

Durante operación:

```text
Restarting challenge...
```

Error:

```text
restart failed: container challenge-1 is not running  [d details]
```

## Notifications

Errores cortos en status bar. Errores largos o confirmaciones en overlay/dialog.

No abrir modal por cada acción exitosa. El cambio visual del estado ya es feedback.

## Confirmaciones

No pedir confirmación para `up`, `stop` o `restart` seleccionado.

Considerar confirmación para:

- stop all, según experiencia real;
- down;
- operaciones futuras sobre volumes.

Demasiadas confirmaciones hacen que el usuario deje de leerlas.

## TamboUI y separación de render

Render debe ser puro desde el punto de vista de side effects externos.

Correcto:

```text
AppState -> build Element tree -> render
```

Incorrecto:

```text
render -> call docker compose ps
```

La documentación de TamboUI también recomienda separar state/controller del render en aplicaciones mantenibles.

## Testing de UI

TamboUI tiene soporte de testing programático para enviar keys, mouse y resize en sus runners. Aprovechar eso para testear:

- selección;
- focus;
- cambiar modo logs;
- resize;
- shortcuts;
- terminal narrow/wide.

No depender exclusivamente de screenshots manuales.
