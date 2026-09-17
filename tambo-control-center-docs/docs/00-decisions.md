# Decisiones y límites actuales

Este archivo separa lo que ya decidimos de lo que todavía está abierto. La distinción importa porque el coding agent puede implementar una decisión, pero no debería rellenar por su cuenta un vacío arquitectónico importante.

## Decisiones cerradas

### El contexto es un proyecto Docker Compose

Tambo trabaja sobre el Compose del proyecto desde el que se ejecuta. No empieza como un dashboard global de todos los containers del daemon.

La experiencia esperada es:

```text
cd repository
tambo
```

Tambo busca el archivo Compose en el directorio actual y, si no existe, sube por los directorios padre hasta encontrar uno.

### El service es la entidad visible principal

La UI muestra `gateway`, `challenge`, `postgres`, etc. El nombre concreto del container es información secundaria.

Esto evita acoplar la UX al naming de containers y deja espacio para que un service tenga una o más instancias.

### Hay acciones individuales y globales

Ambas tienen el mismo nivel de importancia conceptual.

Ejemplos:

```text
up selected
stop selected
restart selected
up all
stop all
restart all
```

No todas tienen que entrar en el primer commit, pero forman parte del producto base.

### Pantalla única con paneles

La navegación principal no abre una pantalla distinta por service. La selección cambia el contenido de los paneles de detalle y logs.

Los paneles deben poder redimensionarse. Para no complicar la primera etapa, el mecanismo recomendado es:

1. layout con proporciones guardadas en `UiLayoutState`;
2. teclas para aumentar o reducir el panel enfocado;
3. persistencia opcional más adelante;
4. drag con mouse como mejora posterior, no requisito de V1.

### Dos modos de logs

`SELECTED`: muestra logs del service seleccionado.

`ALL`: muestra logs de todos los services del proyecto.

Solo hace falta un stream activo a la vez. Al cambiar modo o service seleccionado, el log controller cancela el proceso anterior y abre el nuevo stream.

### Estado de ejecución y health son separados

Un service puede estar `RUNNING` y `UNHEALTHY` al mismo tiempo. Por eso no se usa un único enum que mezcle ambos conceptos.

### Integración Docker CLI-first

La V1 usa `docker compose` mediante un adapter basado en procesos. No se incorpora `docker-java` al comienzo.

La decisión se puede revisar más adelante si aparecen límites reales de performance, control o portabilidad.

### Compose es quien interpreta Compose

Tambo no implementa un parser funcional del spec de Compose. Ejecuta:

```bash
docker compose config --format json
```

El resultado es el modelo efectivo que Docker Compose aplicaría, con interpolación y normalización ya resueltas.

## Decisiones recomendadas, pero revisables

### Java moderno

Recomendación: JDK 25 si tu entorno ya lo usa cómodamente. Como mínimo, elegir una versión que permita records, sealed types y virtual threads sin backports.

No conviene escribir el diseño alrededor de una versión concreta hasta crear el `pom.xml` o `build.gradle` real.

### Maven

Maven es una buena opción inicial porque el objetivo es aprender Java y mantener el build aburrido. Gradle también sirve. La arquitectura no depende de esto.

### Jackson para JSON

Los comandos de Compose ofrecen JSON. Jackson es una dependencia razonable y conocida. No tiene sentido escribir un parser JSON propio.

### PicoCLI

TamboUI ofrece integración opcional con PicoCLI. Puede servir después para flags como `--file`, `--project-directory` o `--debug`, pero `tambo` sin argumentos debe seguir siendo el camino principal.

## Decisiones que no deben cerrarse todavía

### Docker Engine API

No agregar `docker-java` hasta que exista una razón concreta. Algunas razones válidas podrían ser:

- el costo de iniciar procesos CLI se vuelve medible y molesto;
- la cancelación de streams por CLI crea problemas reales;
- necesitamos capacidades que la CLI no expone de forma estructurada;
- queremos soportar un daemon remoto de una forma que el CLI adapter complique.

### Persistencia de preferencias

No hace falta decidir ahora dónde guardar tamaño de paneles, modo de logs o filtros. Primero validar la interacción.

### Plugin system

No diseñar uno. YAGNI.

### Soporte Podman

El puerto hacia infraestructura deja abierta la posibilidad, pero no agregar código específico ni requisitos de compatibilidad hasta que exista un caso real.

### Réplicas

El dominio no debe asumir que service == container, pero la V1 puede mostrar una sola fila por service y resumir sus instancias.

### Parsing semántico de logs

Los logs son texto. No intentar detectar automáticamente Spring, Logback, JSON logs, stack traces o niveles en la primera versión.

## Regla de cambio

Si durante la implementación una decisión de este documento empieza a estorbar, no se fuerza el código para respetarla. Se crea o actualiza un ADR explicando:

- qué problema apareció;
- qué alternativas existen;
- qué elegimos;
- qué costo aceptamos.

Ese proceso es parte del aprendizaje.
