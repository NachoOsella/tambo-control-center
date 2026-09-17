# Tambo Control Center

Documentación de diseño para una TUI en Java orientada a operar proyectos Docker Compose desde el directorio del proyecto.

El nombre es de trabajo. El ejecutable se asume como `tambo` porque ese es el flujo que queremos: entrar a un repositorio, ejecutar `tambo` y obtener una vista operativa del stack definido por Compose.

## Qué problema resuelve

Durante desarrollo local, un proyecto con varios servicios suele obligar a alternar entre comandos como `docker compose ps`, `docker compose logs`, `docker compose up`, `docker compose restart`, `docker compose stats` y `docker compose events`. La información está disponible, pero queda repartida entre comandos y terminales.

Tambo concentra ese ciclo en una TUI. La unidad principal no es el container sino el **service de Docker Compose**. Si el archivo declara `gateway`, `challenge`, `bank`, `postgres` y `kafka`, esos son los elementos que aparecen en la pantalla principal.

La primera versión no intenta reemplazar Docker Desktop, lazydocker, k9s ni una plataforma de observabilidad. Está pensada para una tarea concreta: desarrollar y operar un proyecto Compose local sin salir de la terminal.

## Flujo esperado

```text
$ cd ~/code/challenge-platform
$ tambo

┌ challenge-platform ──────────────────────────────────────────────────┐
│ Services                   │ Details                                 │
│ ● gateway      running     │ service     challenge                   │
│ ● challenge    running     │ runtime     running                     │
│ ◐ roadmap      starting    │ health      healthy                     │
│ ! bank         unhealthy   │ image       challenge:dev               │
│ ○ kafka        stopped     │ containers  1                           │
│ ● postgres     running     │ ports       8081 -> 8080                │
├────────────────────────────┴─────────────────────────────────────────┤
│ Logs: challenge                                      [selected/all] │
│ 16:42:03 Started ChallengeApplication                              │
│ 16:42:04 Kafka connection established                             │
│ 16:42:06 GET /actuator/health 200                                 │
├─────────────────────────────────────────────────────────────────────┤
│ u up  s stop  r restart  U up all  S stop all  L logs mode  q quit │
└─────────────────────────────────────────────────────────────────────┘
```

## Decisiones ya tomadas

- Proyecto Docker Compose como contexto principal.
- `tambo` busca un archivo Compose desde el directorio actual hacia arriba.
- La UI muestra services de Compose, no containers como entidad principal.
- Control individual y global de services.
- Una sola pantalla principal con paneles.
- Los paneles deben poder ajustar su tamaño. La V1 puede empezar con proporciones configurables por teclado y dejar drag con mouse para después.
- Los logs pueden mostrar el service seleccionado o todos los services.
- Runtime state y health state son conceptos separados.
- Integración CLI-first con Docker Compose para la V1.
- El modelo Compose se obtiene con `docker compose config --format json`, en vez de parsear YAML a mano.
- La UI no ejecuta comandos Docker directamente. Pasa por casos de uso y puertos de aplicación.

## Por qué CLI-first

Docker Compose ya resuelve detalles que no vale la pena reimplementar: interpolación de variables, archivos combinados, profiles, `depends_on`, nombres de proyecto, expansión de sintaxis corta y otros detalles del modelo Compose. Tambo consume el resultado canónico de Compose y usa comandos estructurados con salida JSON cuando existen.

Esto también deja un proyecto muy bueno para aprender Java: `ProcessBuilder`, lifecycle de procesos, streams, JSON, records, sealed interfaces, concurrencia, virtual threads, modelado de estados, manejo de errores, testing de adapters y diseño de fronteras.

## Qué contiene este ZIP

- `docs/00-decisions.md`: decisiones tomadas y temas que todavía no conviene cerrar.
- `docs/01-product.md`: alcance y requisitos del producto.
- `docs/02-architecture.md`: arquitectura propuesta y límites entre capas.
- `docs/03-domain-model.md`: modelo de dominio.
- `docs/04-state-events.md`: estados, eventos y transiciones.
- `docs/05-compose-integration.md`: integración concreta con Docker Compose.
- `docs/06-tui-ux.md`: diseño de pantalla, navegación y paneles ajustables.
- `docs/07-logs-stats.md`: logs, stats y consumo de recursos.
- `docs/08-concurrency.md`: concurrencia, streams y cancelación.
- `docs/09-errors-lifecycle.md`: errores y lifecycle.
- `docs/10-testing.md`: estrategia de tests.
- `docs/11-roadmap.md`: milestones de implementación.
- `docs/12-java-learning.md`: cómo usar el proyecto para aprender Java sin escribir todo a mano.
- `docs/13-kafka-future.md`: cómo podría crecer hacia un explorador Kafka sin contaminar la V1.
- `adrs/`: decisiones arquitectónicas registradas individualmente.
- `examples/`: Compose de ejemplo y mockups.
- `PI_WORKFLOW.md`: reglas propuestas para trabajar con un coding agent sin delegarle tus decisiones.
- `BACKLOG.md`: backlog priorizado.
- `REFERENCES.md`: documentación oficial consultada.

## Orden recomendado de lectura

1. `docs/00-decisions.md`
2. `docs/01-product.md`
3. `docs/02-architecture.md`
4. `docs/03-domain-model.md`
5. `docs/05-compose-integration.md`
6. `docs/06-tui-ux.md`
7. `docs/11-roadmap.md`
8. `PI_WORKFLOW.md`

No hace falta implementar toda esta documentación literalmente. Parte del objetivo del proyecto es que las decisiones cambien cuando el código demuestre que una abstracción no sirve. Los ADR existen para hacer esos cambios explícitos, no para congelar una arquitectura prematura.
