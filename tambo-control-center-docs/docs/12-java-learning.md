# Cómo usar Tambo para aprender Java sin escribir todo a mano

## Objetivo real

No querés practicar velocidad de tipeo. Querés aprender a diseñar software Java, leer implementaciones, detectar problemas y tomar decisiones técnicas.

Ese objetivo es compatible con usar un coding agent, siempre que el agente no tome las decisiones importantes por vos.

## Qué deberías decidir vos

Antes de que el agente implemente una feature, deberías poder responder preguntas como:

```text
¿Qué entidad estoy modelando?
¿Quién es la fuente de verdad?
¿Dónde vive esta responsabilidad?
¿Este estado es observado o deseado?
¿Este proceso debe ser largo o corto?
¿Cómo se cancela?
¿Qué pasa cuando falla?
¿La UI conoce Docker?
¿Necesito realmente esta interfaz?
¿Cómo lo testeo sin Docker real?
```

No hace falta saber la sintaxis exacta de memoria.

## Qué puede escribir el agente

Perfectamente delegable:

- records;
- DTOs Jackson;
- boilerplate de TamboUI;
- tests repetitivos;
- mapping;
- command builders después de definir el contrato;
- Maven config;
- refactors mecánicos;
- fixtures.

## Qué no debería decidir sin preguntarte

- crear una nueva capa;
- agregar framework;
- cambiar CLI por Docker Engine API;
- introducir Reactor/RxJava;
- agregar cache/persistence;
- elegir estado global vs event bus;
- hacer un plugin system;
- cambiar el service por container como entidad principal;
- cambiar semántica de `stop`, `restart`, `down`;
- agregar dependencias grandes.

## Rutina de cada feature

### 1. Pedí análisis, no código

Ejemplo:

```text
We are implementing runtime status refresh.
Read the relevant code first.
Do not edit anything yet.
Explain the current flow, the smallest design that fits it, and the decisions I need to make.
```

### 2. Elegí la arquitectura

El agente puede darte opciones, pero vos elegís.

Ejemplo:

```text
Use a periodic snapshot for now. Do not add compose events yet.
Keep Docker JSON DTOs inside infrastructure.
```

### 3. Pedí implementación pequeña

```text
Implement only the runtime snapshot slice.
Do not refactor unrelated code.
```

### 4. Pedí explicación del diff

No una explicación genérica. Que responda:

```text
What Java concepts does this implementation use?
Why is each interface needed?
What could be simpler?
Where are the failure modes?
```

### 5. Revisá código real

Elegí 2 o 3 archivos y leelos completos. No hace falta leer cada mapper generado.

## Conceptos Java que vas a tocar naturalmente

### Records

Buenos para snapshots y DTOs inmutables.

### Enums

Estados de runtime/health.

### Sealed interfaces

Events, operation states o failures cuando realmente haya un conjunto cerrado.

### Pattern matching

Procesar events/states sin `instanceof` chains horribles.

### Generics

Results, collections, publishers, mappers.

### Optional

Datos realmente ausentes. No usar Optional en cada campo por estética.

### ProcessBuilder / Process

Corazón de la integración CLI-first.

### Streams de I/O

Leer stdout/stderr, line buffering, charset.

### Virtual threads

I/O bloqueante concurrente sin callbacks complejos.

### CompletableFuture

Puede aparecer para resultados de operaciones, aunque no hay que usarlo si un virtual thread + event queue queda más claro.

### Executors

Lifecycle y cleanup.

### Collections

Maps por service name, lists de instances, ring buffer.

### Jackson

Separar DTO y dominio.

### Exceptions / result types

Errores de infraestructura y traducción a UI.

### Testing

Fakes, fixtures, concurrency control.

## Ejercicio intelectual útil

Cada vez que el agente proponga una interfaz, preguntate:

> Si tuviera una sola implementación para siempre, ¿igual necesito esta interfaz?

En `ComposeLifecycle` la respuesta puede ser sí porque separa aplicación de proceso externo y facilita tests.

En `ServiceNameFormatterStrategyFactory` probablemente no.

## Evitar arquitectura de portfolio

No metas patterns para poder decir que usaste patterns.

Este proyecto ya tiene problemas reales:

```text
external process boundary
stream lifecycle
state synchronization
UI state
runtime reconciliation
error handling
resource cleanup
```

No necesita CQRS, Kafka interno, Spring, database y plugin architecture para ser serio.

## Señal de aprendizaje

Vas bien cuando podés mirar un PR del agente y decir cosas como:

```text
This DTO leaked into the application layer.
We are storing a derived value twice.
This worker can outlive the selected service.
Restart should not optimistically set runtime to RUNNING.
This interface has no useful boundary.
This mutable list can be read while another thread updates it.
```

Eso vale mucho más que recordar de memoria cómo escribir un getter.
