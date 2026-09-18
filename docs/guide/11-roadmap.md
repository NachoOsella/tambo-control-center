# Roadmap de implementación

El orden está pensado para aprender y mantener siempre un slice ejecutable. No queremos construir veinte interfaces antes de ver una TUI.

## Milestone 0: spike de TamboUI

Objetivo: validar librería y backend terminal.

Entregable:

```text
hardcoded list of services
selection with arrows
three panels
resize terminal
q to quit
```

Sin Docker.

Aprendizaje Java/TamboUI:

- build;
- app lifecycle;
- layout;
- list state;
- key events;
- render loop.

Criterio de salida: sabemos renderizar la forma básica de la aplicación.

## Milestone 1: descubrir Compose y listar services

Agregar:

```text
ProjectLocator
ProcessRunner
ComposeConfigAdapter
```

Flujo:

```text
tambo
 -> find compose
 -> docker compose config --format json
 -> map services
 -> render real list
```

Todavía no runtime.

Criterio: cualquier service declarado aparece aunque no exista container.

## Milestone 2: runtime snapshot

Agregar:

```bash
docker compose ps --all --format json
```

Mostrar:

```text
runtime
health
container name/count
ports
exit code
```

Implementar refresh manual primero, por ejemplo `g`.

Después polling simple.

Criterio: si un container se inicia desde otra terminal, Tambo lo refleja dentro del intervalo esperado.

## Milestone 3: lifecycle individual

Agregar:

```text
u up selected
s stop selected
r restart selected
```

Con operation state y errores.

Criterio: UI no se bloquea mientras corre el comando.

## Milestone 4: logs selected

Agregar un único stream:

```bash
docker compose logs -f --tail 200 --timestamps <service>
```

Ring buffer, scroll, follow.

Criterio: cambiar service cambia stream sin procesos huérfanos.

## Milestone 5: logs all

Agregar `LogMode.ALL_SERVICES` y toggle.

Criterio: cambiar selected/all es estable y barato.

En este punto Tambo ya debería ser útil en trabajo real.

## Milestone 6: lifecycle global

Agregar:

```text
U up all
S stop all
R restart all
```

Resolver exclusión entre operaciones globales e individuales.

## Milestone 7: panel resize + responsive layout

Agregar ratios controlados por teclado.

Después layout narrow.

No persistir todavía si no hace falta.

## Milestone 8: Compose events

```bash
docker compose events --json
```

Usar eventos para solicitar refresh más rápido.

Mantener reconciliación periódica.

## Milestone 9: stats

```bash
docker compose stats --no-stream --format json
```

Mostrar CPU/memory en details. Charts solo si aportan algo.

## Milestone 10: hardening

- mejor error reporting;
- debug log;
- terminal small handling;
- cancellation robusta;
- integration tests Docker;
- release packaging;
- native image solo si interesa.

## V1 release boundary sugerido

Podemos llamar V1 al final de Milestone 7 u 8.

No esperaría stats, charts, mouse drag ni Kafka para publicar una primera versión usable.

## V1.1 ideas

- stats;
- events feed;
- filter services;
- search logs;
- copy selected log line;
- show dependencies;
- profiles;
- `up --build` action;
- explicit recreate;
- execute shell/command in service;
- persistence of layout preferences.

## V2 ideas

- workspace registry de múltiples Compose projects;
- optional Docker Engine adapter;
- remote contexts;
- compose profiles UI;
- deeper networking/volumes;
- Kafka module.

## Regla de scope

Una feature nueva entra solo si responde a una molestia encontrada usando la aplicación.

El proyecto es suficientemente rico sin inventarle features para aprender patrones.
