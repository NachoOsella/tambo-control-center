# Concurrencia y cancelación

## Por qué este proyecto es bueno para aprender concurrencia

Una TUI de Docker tiene varias actividades independientes:

- input del usuario;
- render;
- stream de logs;
- stream de events;
- snapshots de runtime;
- snapshots de stats;
- lifecycle commands.

Si todo se hace en el mismo thread, la UI se congela. Si todos los threads mutan estado libremente, aparecen races.

## Modelo recomendado

```text
                 ┌───────────────┐
                 │ UI event loop │
                 └───────┬───────┘
                         │ intents
                         ▼
                 ┌───────────────┐
                 │ Controller    │
                 └───────┬───────┘
                         │ schedules
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
      lifecycle        logs          refresh
       worker          reader          worker
          │              │              │
          └─────── events/results ──────┘
                         │
                         ▼
                 application queue
                         │
                         ▼
                    state reducer
```

No hace falta llamarlo Redux. La idea es una dirección clara para los cambios.

## Virtual threads

Java moderno hace que sea razonable usar un thread virtual por tarea bloqueante de I/O.

Buenos candidatos:

- esperar un proceso corto;
- leer stdout de logs;
- leer stderr;
- leer events;
- esperar exit del child process.

Eso permite código imperativo sencillo sin Reactor.

## StructuredTaskScope

Puede evaluarse si la versión de Java elegida y su estado de API lo hacen conveniente, pero no debe convertirse en requisito del proyecto. Un `ExecutorService` con virtual threads es suficiente.

## Executors

Diseño simple:

```java
ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
ScheduledExecutorService scheduler = ...;
```

El scheduler puede encargarse de runtime/stats refresh. No hace falta que el scheduler use muchos threads.

## Un solo escritor lógico de AppState

Esta es la regla que más simplifica.

Workers producen mensajes:

```text
RuntimeRefreshed
LogLineReceived
OperationFailed
StatsRefreshed
```

Un componente procesa esos mensajes y publica un nuevo AppState.

Así los workers no hacen:

```java
state.services().get(name).setHealth(...);
```

desde threads arbitrarios.

## AtomicReference

Una implementación práctica puede guardar el último snapshot en:

```java
AtomicReference<AppState>
```

pero `AtomicReference` no reemplaza el diseño. Si cinco threads hacen read-modify-write independientes, todavía pueden perder updates.

Preferir reducer/queue o sincronización central.

## Cancelación de procesos

`LogSession.close()` debe:

1. indicar cancelación;
2. cerrar readers si corresponde;
3. destruir child process;
4. esperar un tiempo corto;
5. usar destroy forcibly solo si hace falta;
6. completar futures pendientes.

La secuencia real se ajusta según comportamiento de `docker compose logs -f`.

## ProcessHandle

Java ofrece `Process` y `ProcessHandle`. Aprovecharlos para inspección y terminación sin shell hacks.

## App shutdown

Orden sugerido:

```text
stop accepting actions
cancel log session
cancel events session
cancel scheduled refreshes
wait/interrupt workers
close executors
restore terminal
exit
```

La restauración del terminal es crítica. TamboUI debería manejar parte del lifecycle de backend, pero nuestro código no debe dejar workers colgados.

## Acción repetida

Si el usuario presiona `rrrrr` sobre el mismo service mientras restart sigue activo, decidir una política.

V1 recomendada:

- una lifecycle operation por service a la vez;
- si ya está busy, ignorar y mostrar `restart already in progress`.

No encolar cinco restarts.

Global vs individual también requiere exclusión. Mientras corre `stop all`, bloquear lifecycle commands individuales hasta que termine.

## Refresh durante operación

Sí se permite. De hecho es deseable ver cómo cambia runtime mientras una operación está activa.

## Errores en background

Nunca perder una excepción dentro de un future.

Todo worker debe terminar en uno de:

```text
success event
failure event
cancelled
```

## Testing concurrencia

No llenar tests de `Thread.sleep(1000)`.

Usar fakes con latches/futures para controlar el orden:

```text
start operation
assert busy
release fake command
assert completion event
```

Para log switching:

```text
start selected A
switch to B
assert A closed
emit late A line
assert ignored
emit B line
assert visible
```

Ese último caso evita que un stream cancelado ensucie el buffer nuevo.
