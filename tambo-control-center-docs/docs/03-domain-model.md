# Modelo de dominio

## Idea central

Compose define **services**. Docker crea **containers** para ejecutar esos services. Tambo muestra el service como entidad principal y usa container instances para describir su estado real.

Por eso no se modela `Service == Container`.

## Aggregate conceptual

```text
ComposeProject
├── ProjectName
├── ProjectRoot
├── ComposeFile
└── Service[]
    ├── ServiceName
    ├── ServiceDefinition
    └── ServiceRuntime
        └── ContainerInstance[]
```

No hace falta implementar esto como DDD estricto. Es un mapa mental para evitar mezclar datos.

## ComposeProject

Representa el proyecto detectado.

Posibles campos:

```java
public record ComposeProject(
    ProjectName name,
    Path root,
    Path composeFile,
    List<Service> services
) {}
```

`Path` es aceptable en este dominio porque el proyecto local vive en filesystem. No hace falta envolver todo por pureza.

## Service

Representa el service declarado en Compose.

```java
public record Service(
    ServiceName name,
    ServiceDefinition definition,
    ServiceRuntime runtime
) {}
```

Separar definición de runtime es útil porque un service existe aunque no haya sido creado.

## ServiceDefinition

Solo guardar datos que la UI o una decisión necesite.

No mapear todo el Compose spec.

Primera versión posible:

```java
public record ServiceDefinition(
    Optional<String> image,
    Optional<String> buildContext,
    List<ServiceName> dependencies,
    List<PortDefinition> declaredPorts,
    boolean healthCheckConfigured,
    List<String> profiles
) {}
```

Incluso esto puede ser demasiado para el primer milestone. Empezar con nombre + image + dependencias puede alcanzar.

## ServiceRuntime

Estado observado, no deseado.

```java
public record ServiceRuntime(
    RuntimeState runtimeState,
    HealthState healthState,
    List<ContainerInstance> instances,
    OptionalInt exitCode
) {}
```

Si hay múltiples instances, el estado del service necesita una política de agregación.

No resolverla hasta soportar réplicas. Para V1 se puede mostrar:

```text
challenge  running  1/1
```

y mantener la lista internamente.

## ContainerInstance

Dato secundario pero real.

```java
public record ContainerInstance(
    ContainerId id,
    String name,
    RuntimeState runtimeState,
    HealthState healthState,
    List<PublishedPort> ports,
    OptionalInt exitCode
) {}
```

No usar container name como identidad estable. Docker proporciona ID.

## RuntimeState

El estado debe representar lo que realmente observamos del runtime.

Propuesta inicial:

```java
public enum RuntimeState {
    NOT_CREATED,
    CREATED,
    RUNNING,
    RESTARTING,
    PAUSED,
    EXITED,
    DEAD,
    REMOVING,
    UNKNOWN
}
```

No todos esos valores tienen que mostrarse con el mismo nivel de detalle. La UI puede presentar `EXITED` y el exit code.

## HealthState

```java
public enum HealthState {
    NOT_CONFIGURED,
    STARTING,
    HEALTHY,
    UNHEALTHY,
    UNKNOWN
}
```

`NOT_CONFIGURED` no es un error. Si el Compose no define healthcheck, la UI no debería mostrarlo como unhealthy.

## OperationState

Runtime y health vienen de Docker. Operation state representa una acción que Tambo está ejecutando.

```java
public sealed interface OperationState {
    record Idle() implements OperationState {}
    record Starting(Instant since) implements OperationState {}
    record Stopping(Instant since) implements OperationState {}
    record Restarting(Instant since) implements OperationState {}
    record Failed(String operation, String message) implements OperationState {}
}
```

No es necesario que sea sealed desde el primer día. El concepto sí importa.

Esto permite:

```text
runtime: RUNNING
health: HEALTHY
operation: RESTARTING
```

sin falsear el estado observado.

## ProjectState

Puede resumir el stack:

```java
public record ProjectState(
    ComposeProject project,
    ProjectRuntimeSnapshot runtime,
    Map<ServiceName, OperationState> operations,
    Instant lastRefresh
) {}
```

## Logs

No conviene almacenar logs dentro de `Service`.

Son un flujo y pertenecen a otro modelo:

```java
public enum LogMode {
    SELECTED_SERVICE,
    ALL_SERVICES
}

public record LogScope(
    LogMode mode,
    Optional<ServiceName> selectedService
) {}

public record LogLine(
    Instant timestamp,
    Optional<ServiceName> service,
    String text,
    StreamKind stream
) {}
```

`StreamKind` puede ser `STDOUT`, `STDERR` o `UNKNOWN`, si podemos conservar esa información.

## Ring buffer de logs

La UI no debe acumular logs infinitamente.

Usar un buffer acotado por cantidad de líneas, por ejemplo 5.000 o 10.000. El valor final se mide.

Operaciones deseadas:

```text
append
clear
snapshot
follow / freeze scroll
```

Cuando el usuario sube para leer logs viejos, el buffer puede seguir recibiendo líneas sin forzar el scroll al final. Cuando vuelve a follow mode, salta al tail.

## Stats

Los stats tampoco pertenecen a la definición del service.

```java
public record ResourceStats(
    double cpuPercent,
    MemoryUsage memory,
    NetworkIo network,
    BlockIo block,
    int pids
) {}
```

Docker CLI puede devolver strings formateados. La primera versión puede mantenerlos como texto y recién parsear unidades si necesitamos charts o comparaciones.

## Value objects que sí aportan

Buenos candidatos:

- `ServiceName`
- `ProjectName`
- `ContainerId`

Candidatos dudosos para V1:

- `CpuPercentage`
- `PortNumber`
- `LogMessage`
- `Uptime`

No envolver primitivos por deporte. Crear un tipo cuando evita errores o expresa una regla.

## DTO no es dominio

Ejemplo de JSON de `docker compose ps`:

```json
{
  "ID": "...",
  "Name": "example-api-1",
  "Project": "example",
  "Service": "api",
  "State": "running",
  "Health": "healthy",
  "ExitCode": 0,
  "Publishers": []
}
```

Ese shape puede mapearse a `ComposePsEntryDto`. Después un mapper convierte a `ContainerInstance`.

No contaminar el core con propiedades con mayúsculas porque Docker las llama así.
