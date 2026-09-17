# Estado, eventos y transiciones

## Tres clases de estado

Tambo tiene tres fuentes conceptuales de estado:

1. **definición**: lo que Compose declara;
2. **runtime observado**: lo que Docker dice que está ocurriendo;
3. **operación local**: lo que Tambo está intentando hacer ahora.

Mezclar las tres crea bugs visuales difíciles de razonar.

## Ejemplo

El service `bank` está running y healthy. El usuario presiona restart.

En el instante inicial:

```text
runtime    RUNNING
health     HEALTHY
operation  IDLE
```

Después de aceptar la tecla:

```text
runtime    RUNNING
health     HEALTHY
operation  RESTARTING
```

Cuando Docker mata el container, un refresh puede observar:

```text
runtime    EXITED / RESTARTING
health     UNKNOWN
operation  RESTARTING
```

Luego:

```text
runtime    RUNNING
health     STARTING
operation  IDLE
```

Y finalmente:

```text
runtime    RUNNING
health     HEALTHY
operation  IDLE
```

No necesitamos una máquina de estados artificial que intente predecir cada paso. Observamos Docker y mantenemos la operación local aparte.

## Application events

Puede ser útil modelar eventos internos para desacoplar workers de la actualización de estado.

Ejemplo:

```java
public sealed interface AppEvent permits
        ProjectLoaded,
        RuntimeRefreshed,
        ServiceOperationStarted,
        ServiceOperationFinished,
        ServiceOperationFailed,
        LogLineReceived,
        DockerEventReceived,
        StatsRefreshed {}
```

No hace falta un event bus sofisticado. Una cola thread-safe consumida por el controller puede alcanzar.

## Flujo recomendado

```text
background workers
      │
      ├─ runtime polling
      ├─ log process
      ├─ compose events
      └─ action commands
      │
      ▼
BlockingQueue<AppEvent>
      │
      ▼
application/controller
      │
      ▼
new AppState
      │
      ▼
render
```

Esto tiene una ventaja pedagógica fuerte: existe un lugar único donde muta el estado lógico.

## Polling + events

No hace falta elegir dogmáticamente uno.

Una estrategia robusta:

- `docker compose events --json` avisa que algo cambió;
- esos eventos disparan o adelantan un refresh;
- un polling lento, por ejemplo cada pocos segundos, sirve como reconciliación;
- acciones propias disparan refresh inmediato al terminar.

Así un evento perdido no deja la UI desactualizada para siempre.

## Debounce

Una operación puede producir varios eventos seguidos. No conviene ejecutar `compose ps` diez veces en 30 ms.

El runtime refresher puede colapsar solicitudes cercanas:

```text
request refresh
request refresh
request refresh
      │
      └── one actual refresh after small debounce
```

No implementar debounce hasta observar que hace falta. Sí dejar el refresh detrás de una abstracción para poder agregarlo.

## Estado inicial

Al arrancar:

```text
DISCOVER_PROJECT
      ↓
CHECK_DOCKER
      ↓
LOAD_DEFINITION
      ↓
LOAD_RUNTIME
      ↓
READY
```

Estos pueden ser estados del bootstrap, no del dominio Docker.

Si falla Docker:

```text
Docker CLI unavailable
Docker daemon unreachable
Compose command unavailable
Invalid compose configuration
```

Cada caso debe dar un mensaje distinto.

## Selección

La selección es UI/application state:

```java
public record SelectionState(
    Optional<ServiceName> selectedService,
    FocusedPanel focusedPanel
) {}
```

Cuando cambia la selección:

- details cambia de inmediato;
- si log mode == selected, se solicita cambio de stream;
- no hace falta reiniciar stats globales;
- no se ejecuta ninguna acción Docker de lifecycle.

## Resize state

```java
public record UiLayoutState(
    double servicesPanelRatio,
    double logsPanelRatio
) {}
```

Aplicar límites:

```text
servicesPanelRatio: 0.20 .. 0.65
logsPanelRatio:     0.20 .. 0.75
```

Los números son ejemplos, no contrato.

## Estado derivado

Muchos valores de UI deberían derivarse durante render/presentación:

```text
status icon
status label
health badge
whether action is enabled
container count label
```

No guardarlos en AppState si pueden calcularse de datos existentes.

## Consistencia

Evitar mutar listas y mapas compartidos mientras la UI los recorre.

Dos enfoques válidos:

1. records/collections inmutables y reemplazo de `AppState`;
2. mutable controller protegido por un solo thread lógico.

Para aprender y razonar más fácil, prefiero snapshots inmutables expuestos a la UI. Internamente puede haber estructuras mutables donde sea práctico.
