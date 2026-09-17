# Forma inicial del código

Este archivo no define una arquitectura obligatoria. Propone una forma concreta de empezar sin crear clases para features que todavía no existen.

## Milestone 0

Con UI hardcodeada, el proyecto podría ser tan chico como:

```text
src/main/java/app/tambo/
  Main.java
  ui/
    TamboApp.java
    AppView.java
    UiState.java

src/test/java/app/tambo/
  ui/
    TamboAppTest.java
```

No crear todavía `domain`, `infrastructure/docker`, `ports` y veinte interfaces vacías.

## Milestone 1

Cuando entra Compose discovery:

```text
src/main/java/app/tambo/
  Main.java

  project/
    ProjectLocator.java
    ProjectContext.java

  compose/
    ComposeConfigReader.java
    ComposeCliConfigReader.java
    dto/
      ComposeConfigDto.java
      ComposeServiceDto.java

  process/
    ProcessRunner.java
    ProcessResult.java

  ui/
    TamboApp.java
    AppView.java
    UiState.java
```

Si `ComposeConfigReader` existe solo para tests y frontera externa, está justificada. Si termina siendo una interfaz con una única llamada trivial que no aporta nada, se puede simplificar.

## Milestone 2

Con runtime:

```text
service/
  ServiceName.java
  RuntimeState.java
  HealthState.java
  ContainerInstance.java
  ServiceRuntime.java

compose/
  ComposeRuntimeReader.java
  ComposeCliRuntimeReader.java
  dto/
    ComposePsEntryDto.java
```

En este momento empieza a tener sentido mover conceptos de service a un package de dominio más claro.

## Milestone 3

Con lifecycle:

```text
application/
  TamboController.java
  AppState.java
  ServiceOperation.java

compose/
  ComposeLifecycle.java
  ComposeCliLifecycle.java
```

`TamboController` puede ser una clase concreta. No necesita `ITamboController`.

## Milestone 4

Con logs:

```text
logs/
  LogMode.java
  LogScope.java
  LogLine.java
  LogBuffer.java
  LogSession.java
  LogController.java

compose/
  ComposeLogSource.java
  ComposeCliLogSource.java
```

Aquí aparecen recursos largos y cancelación. Es un buen punto para revisar si la separación application/infrastructure sigue siendo clara.

## Tipos sugeridos

### ProjectContext

```java
public record ProjectContext(
        Path root,
        Path composeFile
) {}
```

Más adelante puede incluir project name efectivo u opciones Compose si realmente se necesitan.

### ServiceName

```java
public record ServiceName(String value) {
    public ServiceName {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("service name cannot be blank");
        }
    }
}
```

No hace falta validar todo el spec de nombres. Compose ya validó la configuración.

### ProcessResult

```java
public record ProcessResult(
        int exitCode,
        String stdout,
        String stderr
) {
    public boolean succeeded() {
        return exitCode == 0;
    }
}
```

Después podemos reemplazar strings gigantes por otra representación si hace falta.

### Command

Puede ser tan simple como:

```java
public record Command(
        List<String> arguments,
        Path workingDirectory
) {}
```

No crear un DSL de comandos.

## DTOs Jackson

Mantenerlos deliberadamente aburridos.

Ejemplo aproximado:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComposePsEntryDto(
        @JsonProperty("ID") String id,
        @JsonProperty("Name") String name,
        @JsonProperty("Service") String service,
        @JsonProperty("State") String state,
        @JsonProperty("Health") String health,
        @JsonProperty("ExitCode") int exitCode,
        @JsonProperty("Publishers") List<PublisherDto> publishers
) {}
```

El mapper hace la traducción a tipos propios. No usar las strings de Docker por todo el programa.

## AppState inicial

No meter todo desde el principio. Cuando ya exista runtime + logs puede tomar esta forma:

```java
public record AppState(
        ProjectViewState project,
        SelectionState selection,
        LogViewState logs,
        UiLayoutState layout,
        NotificationState notification
) {}
```

La palabra `ViewState` es intencional: el objeto puede contener datos ya organizados para la pantalla sin pretender ser el aggregate del dominio.

## Presenter

No crear un presenter hasta que el render empiece a duplicar reglas como:

```text
RUNNING + UNHEALTHY -> icon !
EXITED + code != 0 -> label exited(1)
```

Cuando aparezca esa duplicación, un `ServiceRowPresenter` o función de mapping puede tener sentido.

## Dependency wiring

En una app chica, `Main` puede hacer wiring explícito:

```java
var processRunner = new ProcessRunner(ioExecutor);
var configReader = new ComposeCliConfigReader(processRunner, objectMapper);
var runtimeReader = new ComposeCliRuntimeReader(processRunner, objectMapper);
var lifecycle = new ComposeCliLifecycle(processRunner);
var controller = new TamboController(...);
var app = new TamboApp(controller);
app.run();
```

Esto es una ventaja, no una vergüenza. Ves las dependencias reales sin un container DI.

## Cuándo extraer módulos Maven

No hacerlo en V1 salvo que aparezca un problema concreto.

Un solo artifact con packages bien separados es más fácil de navegar y refactorizar.

## Build mínimo

Dependencias probables:

```text
TamboUI toolkit/backend
Jackson databind
JUnit
AssertJ opcional
```

No agregar Lombok. Records cubren buena parte del boilerplate que nos interesa evitar y mantienen el código Java explícito.

## Regla para cada clase nueva

Antes de aceptarla, poder terminar esta frase:

> Esta clase existe porque es responsable de ________.

Si la respuesta es "para mantener la arquitectura limpia", probablemente todavía no hace falta.
