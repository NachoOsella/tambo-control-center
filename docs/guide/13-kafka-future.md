# Futuro módulo Kafka

La idea del explorador Kafka sigue siendo buena, pero no debe mezclarse con V1.

## Por qué encaja después

Un stack Compose de microservicios muchas veces incluye Kafka. Una vez que Tambo ya sabe:

- cuál es el proyecto;
- qué services están corriendo;
- cómo manejar paneles;
- cómo manejar streams;
- cómo representar eventos;

podemos agregar una pestaña o workspace Kafka sin rehacer la aplicación.

## UX futura

```text
Services | Kafka
```

Kafka:

```text
┌ Topics ───────────────┬ Partitions / Groups ───────────────────────┐
│ challenge-results     │ partition 0  offset ...                    │
│ bank-events           │ partition 1  offset ...                    │
│ notifications         │ consumer group roadmap ...                 │
├───────────────────────┴────────────────────────────────────────────┤
│ Messages                                                            │
│ key=user-12  ChallengeResultRegisteredV1 ...                        │
└─────────────────────────────────────────────────────────────────────┘
```

## Separación importante

Docker module y Kafka module no deberían conocerse directamente.

Application podría resolver una conexión Kafka a partir de configuración explícita o metadata del proyecto, pero no asumir que todo service llamado `kafka` usa una configuración concreta.

## Qué no hacer ahora

- no agregar Kafka client dependency;
- no crear `BrokerPort` vacío;
- no reservar medio package tree;
- no meter tabs genéricas por una feature futura.

Primero terminar el control center.

## Qué sí dejamos preparado

Nada especial, salvo buena separación entre UI, application e infrastructure. Si esa arquitectura sirve de verdad, agregar otra fuente externa debería ser posible sin tocar el dominio Docker de forma invasiva.
