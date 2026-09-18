# Logs, stats y costo de recursos

## Pregunta original

Queremos poder alternar entre:

- logs del service seleccionado;
- logs de todos los services.

La preocupación es si esto consume demasiados recursos.

## Respuesta corta

No hace falta mantener todos los logs abiertos por separado. La implementación recomendada mantiene **un solo proceso de logs activo**.

### Selected mode

```bash
docker compose logs --follow --tail 200 --timestamps challenge
```

### All mode

```bash
docker compose logs --follow --tail 200 --timestamps
```

Cuando cambia scope, se cancela el proceso actual y se inicia otro.

Eso hace que el costo sea razonable para desarrollo local.

## LogSession

Concepto de aplicación:

```java
public interface LogSession extends AutoCloseable {
    LogScope scope();
    boolean isAlive();
    void close();
}
```

Un `LogController` posee como máximo una sesión activa:

```text
current session -> selected(challenge)
```

Cambio de selección:

```text
close current
start selected(bank)
```

Cambio a all:

```text
close current
start all
```

## Race al cambiar rápido

Usuario mantiene `j` y pasa por cinco services. No queremos abrir cinco procesos completos.

Opciones:

1. debounce de 100-250 ms antes de cambiar log stream;
2. cambiar el details panel inmediatamente y logs después de una pausa corta;
3. arrancar/cancelar de inmediato y medir si realmente molesta.

Para V1 elegiría 3 por simplicidad. Si se nota churn, agregamos debounce.

## Buffer

Los logs recibidos se guardan en un ring buffer.

Ejemplo:

```text
max lines: 5000
```

No existe motivo para mantener horas infinitas de output en heap solo porque la terminal estuvo abierta.

El valor debe ser configurable internamente y medirse.

## Cambio de scope y buffer

Hay dos UX posibles:

A. vaciar buffer al cambiar scope;
B. mantener buffers independientes por scope/service.

Para V1 recomiendo A. Es más simple y evita consumo oculto.

Después se puede mantener un pequeño cache por service si volver atrás constantemente se siente molesto.

## Parsing

`docker compose logs` agrega prefixes de service/container. No escribir un parser frágil basado solo en columnas visuales si Compose puede variar formato.

Para selected mode, podemos tratar la línea completa como texto.

Para all mode necesitamos service. Si el prefix CLI resulta difícil de parsear robustamente, podemos conservar el texto completo y renderizarlo tal como viene en V1. El modelo de `LogLine.service` puede ser optional.

No bloquear el proyecto por un parser perfecto de logs.

## stderr del proceso de logs

Leer stderr en paralelo para evitar llenar el pipe y bloquear el child process.

Los mensajes de stderr operativos pueden convertirse en error de `LogSession`.

## Stats

Stats son distintos de logs. No requieren stream permanente.

Comando:

```bash
docker compose stats --no-stream --format json
```

Estrategia inicial:

```text
READY -> every ~2s -> fetch stats -> update snapshot
```

No ejecutar stats para services stopped si el comando ya no devuelve datos útiles.

## Costo estimado conceptual

Con el diseño recomendado:

```text
1 long-lived logs process
1 optional long-lived events process
1 short ps process every few seconds or on invalidation
1 short stats process every 1-3 seconds when enabled
short action processes on user commands
```

Para un entorno local esto es pequeño. Lo importante es no lanzar `ps` o `stats` en cada render frame.

## Render frequency

La UI no necesita redibujarse porque existe un loop de 60 fps si nada cambió.

Si TamboUI runner usa tick events, mantenerlos moderados y usar state changes para refrescar. El frame renderer debe ser barato, pero Docker I/O jamás ocurre en render.

## Future: Engine API

Si profiling muestra que crear procesos CLI de stats/ps es el cuello de botella, ahí aparece una razón concreta para evaluar Docker Engine API.

Ese cambio no debería afectar UI ni dominio si los ports están bien puestos.

## Backpressure

Un service puede escribir logs más rápido de lo que la UI puede renderizar.

No intentar renderizar cada línea como un frame independiente.

El reader agrega líneas al buffer y solicita redraw de forma coalescida. Si entran 1000 líneas en 50 ms, la UI puede dibujar el estado más nuevo una vez.

## Líneas gigantes

Poner un límite razonable por línea o manejar truncado visual. Un payload JSON de varios megabytes no debería congelar el terminal.

Podemos conservar la línea completa hasta un máximo y marcar:

```text
[truncated]
```

Esto puede quedar para hardening.
