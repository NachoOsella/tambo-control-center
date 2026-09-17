# Producto y alcance

## Visión

Tambo es una TUI para desarrollo local con Docker Compose. Su trabajo es mostrar el estado operativo del stack y permitir acciones comunes sin repartir la atención entre varias terminales.

El producto está deliberadamente centrado en Compose. No intenta ser un administrador general del daemon Docker.

## Usuario objetivo

Desarrollador que trabaja en repositorios con varios servicios, por ejemplo:

```text
gateway
challenge
roadmap
bank
notifications
kafka
postgres
redis
```

El usuario ya conoce Docker y podría ejecutar los comandos a mano. Tambo reduce fricción, pero no esconde los conceptos de Docker.

## Objetivos de V1

Al ejecutar `tambo` dentro de un proyecto válido:

1. encontrar el Compose del proyecto;
2. validar que Docker y Compose están disponibles;
3. cargar los services declarados;
4. mostrar estado runtime y health;
5. seleccionar un service;
6. iniciar, detener y reiniciar el service seleccionado;
7. iniciar, detener y reiniciar el proyecto completo;
8. mostrar logs en vivo del service seleccionado;
9. cambiar a logs globales del proyecto;
10. mostrar detalles básicos del service;
11. refrescar estado sin bloquear la UI;
12. salir limpiamente, cancelando procesos hijos iniciados por Tambo.

## Fuera de alcance de V1

- editar `compose.yaml`;
- crear services;
- gestionar imágenes globalmente;
- gestionar Docker Swarm;
- Kubernetes;
- construir un editor de Dockerfile;
- shell interactiva dentro del container;
- file browser dentro del container;
- networking visual avanzado;
- gestión completa de volumes;
- secret management;
- remote Docker contexts como requisito;
- métricas históricas persistidas;
- alertas;
- parseo de logs por framework;
- soporte explícito para Podman;
- explorador Kafka integrado.

Algunas de estas ideas pueden aparecer después. Dejarlas fuera permite que V1 sea terminable.

## Requisitos funcionales

### RF-01 Descubrir proyecto

Tambo busca Compose desde `cwd` hacia la raíz del filesystem.

Nombres reconocidos inicialmente, en orden de preferencia:

```text
compose.yaml
compose.yml
docker-compose.yaml
docker-compose.yml
```

Si hay más de uno en el mismo directorio, la política debe ser explícita. Para V1 se recomienda usar el orden anterior y mostrar qué archivo fue seleccionado.

### RF-02 Cargar modelo Compose efectivo

Ejecutar desde el directorio del proyecto:

```bash
docker compose config --format json
```

El adapter convierte el JSON en DTOs y luego en el modelo interno necesario para Tambo.

### RF-03 Mostrar services

La lista debe incluir todos los services declarados, aunque todavía no exista un container para ellos.

Un service no creado sigue existiendo en Compose. Su estado runtime puede ser `NOT_CREATED`.

### RF-04 Snapshot runtime

Obtener información de containers con una salida estructurada, por ejemplo:

```bash
docker compose ps --all --format json
```

El resultado debe correlacionarse por nombre de service.

### RF-05 Acciones por service

Acciones base:

```bash
docker compose up -d <service>
docker compose stop <service>
docker compose restart <service>
```

Nota: `up` puede crear/recrear y levantar dependencias. `stop` conserva el container. `restart` no aplica cambios de configuración del Compose, por lo que la UI no debe presentarlo como equivalente a recrear.

### RF-06 Acciones globales

Base recomendada:

```bash
docker compose up -d
docker compose stop
docker compose restart
```

`down` es semánticamente más destructivo porque elimina containers y networks del proyecto. No debe confundirse con `stop`. Puede agregarse como acción separada y con una confirmación clara.

### RF-07 Logs seleccionados

```bash
docker compose logs --follow --tail <N> --timestamps <service>
```

### RF-08 Logs globales

```bash
docker compose logs --follow --tail <N> --timestamps
```

### RF-09 Cambiar modo de logs

Cambiar de selected a all, o cambiar de service mientras estamos en selected, cancela el stream anterior e inicia otro.

No mantener N streams por service si no son visibles.

### RF-10 Stats

Puede entrar al final de V1 o en V1.1:

```bash
docker compose stats --no-stream --format json
```

Se recomienda polling moderado, no streaming continuo para cada service.

### RF-11 Eventos Docker Compose

Mejora útil para reducir polling:

```bash
docker compose events --json
```

Puede alimentar invalidaciones del snapshot. La primera implementación puede seguir haciendo un polling lento como respaldo.

### RF-12 Paneles ajustables

Debe existir estado de layout separado del estado de dominio.

Ejemplo conceptual:

```text
leftWidth = 0.35
logHeight = 0.45
```

Los límites evitan que un panel desaparezca accidentalmente.

## Requisitos no funcionales

### Responsividad

Ningún comando Docker debe ejecutarse en el thread que procesa input/render.

### Cancelación

Todo stream de logs o events iniciado por Tambo debe poder cancelarse al cambiar de contexto o cerrar la app.

### Errores visibles

Un fallo de Docker no debe matar la TUI salvo que el entorno sea irrecuperable. Los errores operativos se muestran en un status area o dialog y quedan disponibles para inspección.

### Sin estado fantasma

Una acción `up` exitosa no debe marcar directamente el service como `RUNNING` solo porque el proceso terminó con exit code 0. La fuente de verdad del runtime vuelve a ser `compose ps` o eventos.

### Testeabilidad

El core no depende de `ProcessBuilder`, de TamboUI ni de Docker DTOs.

### Performance razonable

No hace falta optimización prematura. Sí hay que evitar procesos redundantes permanentes. Un diseño típico de V1 puede sostener:

- 1 stream de logs;
- 0 o 1 stream de events;
- polling periódico de `ps`;
- polling más lento de stats;
- procesos cortos para acciones.

Eso es pequeño para una máquina de desarrollo.

## Criterios de aceptación V1

La V1 se considera usable cuando, en un Compose real de varios services, es posible trabajar durante una sesión normal sin volver a otra terminal para:

- ver qué está arriba;
- ver health;
- levantar o parar services;
- reiniciar;
- leer logs del service actual;
- cambiar a logs globales;
- detectar una caída y ver el estado actualizado.

No hace falta que la UI sea preciosa todavía. Sí tiene que ser predecible.
