# Integración con Docker Compose

## Objetivo

Usar Docker Compose como fuente de verdad y evitar duplicar lógica del spec.

La V1 habla con Docker a través del executable `docker` instalado en el sistema.

## Descubrimiento del archivo

Desde `cwd`, buscar en cada directorio:

```text
compose.yaml
compose.yml
docker-compose.yaml
docker-compose.yml
```

Si no aparece, subir al padre hasta llegar al root.

Ejemplo:

```text
/home/nacho/code/platform/challenge/src/main/java
                     ↑
          /platform/compose.yaml
```

El root del proyecto pasa a ser el directorio que contiene el Compose elegido.

### Symlinks

No hace falta resolver casos exóticos en V1. Usar `Path.toAbsolutePath().normalize()` y documentar el comportamiento. Si los symlinks crean un problema real, se revisa.

## Preflight

Antes de abrir la TUI completa, comprobar:

```bash
docker compose version --short
```

y cargar el config. Un `docker info` adicional puede distinguir CLI presente de daemon inaccesible, pero no es obligatorio si los mensajes de error de los comandos ya son claros.

## Cargar definición

Comando base:

```bash
docker compose config --format json
```

Razón: Compose combina archivos, resuelve variables y normaliza la configuración. Tambo debería consumir ese resultado.

### Qué mapear

No mapear todo el JSON a dominio. Crear DTOs tolerantes y extraer solo:

- project name si está disponible;
- services;
- image;
- build context si importa para UI;
- depends_on;
- ports;
- profiles;
- healthcheck presence;
- labels relevantes si luego se necesitan.

Jackson puede ignorar propiedades desconocidas.

## Runtime snapshot

```bash
docker compose ps --all --format json
```

Docker Compose devuelve entradas por container con service, state, health, exit code y ports publicados.

La correlación principal es el campo `Service`.

### Service sin container

Si el service aparece en config pero no en `ps --all`, su runtime es `NOT_CREATED`.

### Múltiples containers

Agrupar todas las entradas con el mismo service.

Aunque V1 no exponga replicas como feature, el mapper no debería descartar silenciosamente la segunda instancia.

## Start / up

Service:

```bash
docker compose up -d challenge
```

Global:

```bash
docker compose up -d
```

`up` crea o recrea containers según configuración e imagen. También puede iniciar dependencias. Por eso es la acción correcta para "poner este service en marcha" desde un estado desconocido.

Una opción futura interesante es `--wait`, que espera hasta running/healthy, pero no la usaría en la primera implementación porque queremos que la UI siga viva y observe el progreso.

## Stop

Service:

```bash
docker compose stop challenge
```

Global:

```bash
docker compose stop
```

Stop no elimina containers. Luego pueden volver a iniciarse.

## Restart

```bash
docker compose restart challenge
```

Importante para UX: restart no aplica cambios recientes del archivo Compose, por ejemplo cambios de variables de configuración que requieran recreación.

Podemos mostrar una ayuda breve:

```text
restart: restarts existing container
up: applies compose/image changes when recreation is needed
```

## Down

```bash
docker compose down
```

No tratarlo como sinónimo de stop. Remueve containers y networks creados por `up`, con opciones adicionales para volumes/images.

Si entra al producto:

- keybinding menos fácil de tocar accidentalmente;
- confirmación;
- por defecto nunca agregar `--volumes` ni `--rmi`.

## Logs

Selected:

```bash
docker compose logs \
  --follow \
  --tail 200 \
  --timestamps \
  challenge
```

All:

```bash
docker compose logs \
  --follow \
  --tail 200 \
  --timestamps
```

### ANSI

Para simplificar parsing y rendering, considerar `--no-color`. TamboUI aplica el estilo visual.

No usar `--no-log-prefix` en modo all porque necesitamos distinguir service. En selected podría usarse, pero mantener formato consistente reduce casos especiales.

## Events

```bash
docker compose events --json
```

Es un proceso largo que emite JSON line by line.

Usos:

- detectar start/stop/die/restart;
- solicitar refresh de runtime;
- mostrar un event feed futuro;
- reducir latencia entre una caída externa y el cambio visual.

No asumir que events reemplaza toda reconciliación.

## Stats

Snapshot simple:

```bash
docker compose stats --no-stream --format json
```

No tiene sentido ejecutarlo a 60 fps. Para una TUI humana, un intervalo del orden de 1 a 3 segundos probablemente sea suficiente. Medir antes de fijarlo.

Podemos incluso dejar stats fuera del primer milestone visual y agregarlo después de lifecycle + logs.

## ProcessRunner

No repartir `new ProcessBuilder(...)` por todos los adapters.

Un componente central puede tener dos operaciones conceptuales:

```java
ProcessResult run(Command command, Duration timeout);
RunningProcess startStreaming(Command command);
```

`RunningProcess` debería permitir:

```text
pid/process handle
stdout subscription
stderr subscription
isAlive
cancel/close
exit future
```

## Comandos como objetos

Evitar construir shell strings:

```java
List.of("docker", "compose", "ps", "--all", "--format", "json")
```

No ejecutar vía `sh -c` salvo necesidad real. Así evitamos quoting, escaping e inyección accidental.

## Working directory

Cada proceso Compose debe ejecutarse en el project root detectado.

Eso mantiene el comportamiento cercano al usuario que ejecutaría el comando a mano.

## Environment

Heredar environment del proceso Tambo por defecto. Variables como `DOCKER_HOST`, `DOCKER_CONTEXT` o `COMPOSE_PROJECT_NAME` pueden afectar Compose y deben seguir funcionando.

No copiar variables a una configuración propia si Docker ya las entiende.

## Timeouts

Procesos cortos:

- config;
- ps;
- stats snapshot;
- lifecycle commands.

Deben tener timeout razonable y cancelación.

Procesos largos:

- logs --follow;
- events.

No usan timeout normal. Se cancelan por lifecycle de subscription.

## stderr

No tratar todo stderr como error fatal automáticamente. La semántica final depende del exit code y del comando.

Guardar stderr para diagnóstico.

## Exit codes

Cualquier comando corto con exit code distinto de 0 produce un resultado de error de infraestructura, que application traduce a una notificación comprensible.

No filtrar el mensaje útil de Docker. Mostrar un resumen y permitir ver detalles si es largo.
