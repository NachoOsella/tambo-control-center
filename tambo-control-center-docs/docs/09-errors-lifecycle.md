# Errores y lifecycle

## Filosofía

Docker falla de formas normales durante desarrollo. Un container que sale con code 1 no es una excepción fatal para la aplicación Tambo.

Tambo debe separar:

- error del entorno;
- error de un comando;
- estado fallido de un service;
- bug interno de Tambo.

## Errores de bootstrap

### No Compose file

Mensaje:

```text
No Compose file found from /current/path upward.
```

Salir con code no cero. No abrir una TUI vacía.

### Docker executable missing

```text
Docker CLI was not found in PATH.
```

### Compose unavailable

```text
Docker Compose is unavailable. `docker compose version` failed.
```

### Invalid Compose config

Mostrar el error de Compose de forma legible y el path del archivo.

### Daemon inaccessible

Mensaje basado en stderr real. No inventar una causa si Docker no la da.

## Errores durante sesión

### Action failed

Ejemplo:

```text
restart challenge failed
```

Status bar + acceso a details de stderr.

No cerrar Tambo.

### Runtime refresh failed

Mantener el último snapshot válido y marcarlo stale:

```text
last refresh: 12s ago [refresh failed]
```

No reemplazar todos los services por `UNKNOWN` de inmediato si fue un fallo transitorio del comando.

### Log stream died

Mostrar:

```text
logs disconnected [press l/retry]
```

Podemos auto-reintentar una vez o reiniciar según política posterior. V1 puede hacerlo explícito.

### Events stream died

Como events es una optimización, polling sigue manteniendo estado. Mostrar warning de diagnóstico, no bloquear operaciones.

## Error types

No usar exceptions genéricas hasta UI.

Ejemplo conceptual:

```java
sealed interface ComposeFailure {
    record CommandFailed(int exitCode, String stderr) implements ComposeFailure {}
    record TimedOut(Duration timeout) implements ComposeFailure {}
    record ExecutableMissing() implements ComposeFailure {}
    record Cancelled() implements ComposeFailure {}
}
```

Se puede implementar con exceptions específicas o results. Lo importante es no depender de comparar strings en la UI.

## Unexpected exceptions

Un bug interno sí merece:

- cerrar terminal correctamente;
- imprimir stack trace fuera de alternate screen o guardarlo en log file;
- exit code no cero.

Durante desarrollo, preferimos un stack trace completo antes que esconder el bug detrás de `Something went wrong`.

## Internal diagnostics

Agregar modo futuro:

```bash
tambo --debug
```

Puede escribir a:

```text
~/.local/state/tambo/tambo.log
```

No fijar path definitivo hasta implementar compatibilidad XDG.

## Lifecycle de proyecto

### Up selected

```text
IDLE
  ↓
STARTING operation
  ↓
compose up -d service
  ↓
refresh runtime
  ↓
IDLE
```

El runtime observado puede pasar por created/running/health-starting.

### Stop selected

`stop` no borra container.

### Restart selected

Reinicia existing containers. Si hay cambios de configuración, el usuario puede preferir `up`.

### Down

No meter dentro de stop. Es una operación distinta.

## Qué ocurre si Tambo se cierra

Salir de Tambo **no detiene el proyecto**.

Solo se cancelan procesos auxiliares creados por Tambo para observar Docker:

```text
logs -f
events
poll workers
```

Los containers quedan como están.

Esto debe quedar claro en UX. `q` significa cerrar dashboard, no `docker compose down`.

## Ctrl+C

Debe tener el mismo efecto de cleanup que `q`, cuando el backend/event loop lo permita.

No dejar terminal en raw mode.
