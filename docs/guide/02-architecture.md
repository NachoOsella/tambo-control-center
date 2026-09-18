# Arquitectura propuesta

## Principio principal

La TUI es una entrada al sistema, no el sistema.

Si una clase de TamboUI sabe cómo construir un comando Docker, hay acoplamiento de más. Si un adapter Docker sabe cuál es el panel enfocado, también.

La estructura inicial puede mantenerse simple:

```text
┌──────────────────────────────────────────────────────┐
│ UI                                                   │
│ TamboUI views, focus, key bindings, UiLayoutState    │
└────────────────────────┬─────────────────────────────┘
                         │ intents / commands
                         ▼
┌──────────────────────────────────────────────────────┐
│ Application                                          │
│ use cases, orchestration, AppState, subscriptions    │
└────────────────────────┬─────────────────────────────┘
                         │ ports
                         ▼
┌──────────────────────────────────────────────────────┐
│ Domain                                               │
│ ComposeProject, Service, RuntimeState, HealthState   │
└────────────────────────┬─────────────────────────────┘
                         │ implemented by
                         ▼
┌──────────────────────────────────────────────────────┐
│ Infrastructure                                       │
│ Docker Compose CLI, filesystem, JSON mapping         │
└──────────────────────────────────────────────────────┘
```

No hace falta crear cuatro módulos Maven el primer día. La separación puede empezar por packages y tests. Modularizar físicamente se justifica cuando el proyecto crezca.

## Packages sugeridos

```text
src/main/java/
  app/tambo/
    Main.java

    domain/
      project/
      service/
      logs/

    application/
      project/
      service/
      logs/
      state/

    infrastructure/
      compose/
      process/
      filesystem/
      json/

    ui/
      screen/
      component/
      input/
      state/
      presenter/
```

No crear packages vacíos para cumplir el diagrama. Se crean cuando aparece código que los necesita.

## Domain

El dominio contiene conceptos propios de Tambo, no representaciones crudas de Docker CLI.

Ejemplos:

```java
public record ServiceName(String value) {}

public enum RuntimeState {
    NOT_CREATED,
    CREATED,
    RUNNING,
    RESTARTING,
    PAUSED,
    EXITED,
    DEAD,
    UNKNOWN
}

public enum HealthState {
    NOT_CONFIGURED,
    STARTING,
    HEALTHY,
    UNHEALTHY,
    UNKNOWN
}
```

Los nombres exactos pueden cambiar cuando veamos datos reales de Compose.

## Application

La capa de aplicación traduce intenciones del usuario a operaciones.

Ejemplos de casos de uso:

```text
LoadProject
RefreshProjectStatus
StartService
StopService
RestartService
StartProject
StopProject
RestartProject
SwitchLogMode
SelectService
ResizePanel
```

`SelectService` y `ResizePanel` son locales a la aplicación/UI. `StartService` necesita infraestructura.

No hace falta implementar cada caso de uso como una clase de ceremonia. Lo importante es que exista un límite claro. Una `TamboController` pequeña puede coordinar varios al comienzo.

## Ports

En vez de una interfaz `DockerClient` enorme, conviene separar por responsabilidad a medida que el código lo pida.

Una forma inicial:

```java
public interface ComposeProjectReader {
    ComposeProjectDefinition load(Path projectRoot);
}

public interface ComposeRuntimeReader {
    ProjectRuntimeSnapshot snapshot(Path projectRoot);
}

public interface ComposeLifecycle {
    CommandResult up(Path projectRoot, Optional<ServiceName> service);
    CommandResult stop(Path projectRoot, Optional<ServiceName> service);
    CommandResult restart(Path projectRoot, Optional<ServiceName> service);
}

public interface ComposeLogSource {
    LogSubscription follow(Path projectRoot, LogScope scope, Consumer<LogLine> sink);
}
```

No copiar estas firmas ciegamente. Son una dirección. Al implementar podemos descubrir que `projectRoot` pertenece a un `ProjectContext`, que `CommandResult` necesita más datos o que logs necesitan un publisher.

## Infrastructure

### `ComposeCli`

Responsable de construir y ejecutar comandos `docker compose` dentro de un proyecto.

Debe centralizar:

- working directory;
- environment;
- timeout para procesos cortos;
- captura stdout/stderr;
- exit code;
- cancelación de procesos largos;
- logging de diagnóstico.

### `ComposeConfigAdapter`

Usa `docker compose config --format json`.

### `ComposePsAdapter`

Usa `docker compose ps --all --format json`.

### `ComposeLifecycleAdapter`

Ejecuta `up`, `stop`, `restart` y, más adelante, `down`.

### `ComposeLogsAdapter`

Mantiene el proceso largo de `docker compose logs --follow`.

### `ComposeEventsAdapter`

Opcional al comienzo. Mantiene `docker compose events --json`.

### `ComposeStatsAdapter`

Hace snapshots con `docker compose stats --no-stream --format json`.

## UI

La UI recibe un snapshot inmutable de estado y renderiza.

No debería hacer esto:

```java
onKey('r', () -> processBuilder.command("docker", "compose", "restart", selected).start());
```

Debería expresar una intención:

```java
onKey('r', controller::restartSelectedService);
```

El controller decide si la acción es válida, marca operation state y ejecuta el caso de uso fuera del event loop.

## AppState

Para una TUI immediate/declarative, un estado central de aplicación es práctico si no se convierte en una bolsa de objetos mutables.

Ejemplo conceptual:

```java
public record AppState(
    ProjectState project,
    SelectionState selection,
    LogViewState logs,
    UiLayoutState layout,
    NotificationState notification
) {}
```

Las actualizaciones deberían ser atómicas desde la perspectiva del render. La UI siempre renderiza un estado coherente.

## Flujo de una acción

Ejemplo: `restart challenge`.

```text
Key 'r'
  │
  ▼
UI handler
  │
  ▼
controller.restartSelectedService()
  │
  ├─ validates selection
  ├─ marks operation RESTARTING
  └─ schedules background task
            │
            ▼
      ComposeLifecycle.restart()
            │
            ▼
 docker compose restart challenge
            │
            ▼
       command result
            │
            ├─ failure -> notification/error
            └─ success -> request runtime refresh
                              │
                              ▼
                      compose ps snapshot
                              │
                              ▼
                         AppState update
```

La UI no inventa que el service volvió a estar healthy. Espera datos observados.

## Por qué no Spring

No hay una necesidad inicial de DI container, HTTP server, ORM ni application context. Java puro permite ver las dependencias directamente.

Si más adelante aparece una razón real para un framework, se evalúa. Agregar Spring solo porque el proyecto es Java quitaría visibilidad sobre justamente las cosas que queremos aprender.

## Regla de dependencias

```text
ui -> application -> domain
infrastructure -> application/domain ports
```

Domain no importa TamboUI, Jackson ni clases de procesos.

Eso es suficiente. No hace falta convertir cada valor en una interfaz o aplicar hexagonal como religión.
