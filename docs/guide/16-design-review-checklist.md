# Checklist de revisión de diseño

Usar antes de aceptar un cambio grande del agente.

## Dominio

```text
[ ] ¿La entidad principal sigue siendo Compose service?
[ ] ¿Container está tratado como runtime instance?
[ ] ¿Runtime y health siguen separados?
[ ] ¿Guardamos algún dato derivado que podría calcularse?
[ ] ¿Estamos mapeando más del Compose spec de lo que usamos?
```

## Docker boundary

```text
[ ] ¿La UI llama Docker indirectamente a través de application?
[ ] ¿Los comandos usan argumentos, no shell strings?
[ ] ¿El working directory es el project root correcto?
[ ] ¿Se conserva stderr/exit code útil?
[ ] ¿Un éxito de comando dispara observación en vez de inventar estado?
```

## Procesos y recursos

```text
[ ] ¿Todo proceso largo tiene owner claro?
[ ] ¿Tiene cancelación?
[ ] ¿Se lee stdout y stderr sin riesgo de bloquear pipes?
[ ] ¿Puede quedar vivo al cambiar de service?
[ ] ¿Puede quedar vivo al salir de Tambo?
[ ] ¿Existe una race entre una sesión vieja y una nueva?
```

## Concurrencia

```text
[ ] ¿Docker I/O ocurre fuera del UI thread?
[ ] ¿Quién puede mutar AppState?
[ ] ¿Una actualización puede pisar otra?
[ ] ¿Los tests de concurrencia evitan sleeps arbitrarios?
```

## TUI

```text
[ ] ¿Render tiene side effects externos?
[ ] ¿La selección y el focus son estados distintos?
[ ] ¿La pantalla sigue usable en terminal más angosta?
[ ] ¿La acción global se distingue de la individual?
[ ] ¿El modo de logs actual es visible?
```

## Abstracciones

```text
[ ] ¿Cada interfaz protege una frontera real o facilita un test real?
[ ] ¿Hay una Factory/Manager/Strategy que podría ser una función o clase concreta?
[ ] ¿Agregamos algo pensando en Podman/Kafka/V2 que V1 no necesita?
[ ] ¿Una dependencia nueva resuelve un problema suficientemente grande?
```

## Java

```text
[ ] ¿Un record simplificaría un DTO/snapshot?
[ ] ¿Un sealed type expresa un conjunto cerrado real?
[ ] ¿Optional representa ausencia real o solo hace ruido?
[ ] ¿Los nombres dicen qué hace el código?
[ ] ¿El código depende de framework donde Java estándar alcanzaba?
```

## Tests

```text
[ ] ¿Existe test del comportamiento nuevo?
[ ] ¿Existe test del error principal?
[ ] ¿La lógica se puede probar sin Docker real?
[ ] ¿Los adapters tienen fixtures representativos?
[ ] ¿Hay un integration test solo donde aporta algo distinto?
```

## Pregunta final

Antes de mergear:

> Si borro esta abstracción o dependencia, ¿qué problema real vuelve a aparecer?

Si no hay una respuesta concreta, probablemente se pueda simplificar.
