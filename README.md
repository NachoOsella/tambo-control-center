# Tambo Control Center

Java 21 Maven scaffold for Tambo, a terminal UI for local Docker Compose projects.

This repository currently contains project setup only. The application code will be added in small increments, starting with the TamboUI spike described in the project documentation.

## Stack

- Java 21
- Maven
- TamboUI 0.5.0
- JLine 3 terminal backend
- JUnit Jupiter for tests

Spring, Docker integration, and JSON mapping are intentionally not included yet. They belong to later milestones when the code needs them.

## Build

```bash
mvn test
```

Run the application after the first UI increment with:

```bash
mvn exec:java -Dexec.mainClass=app.tambo.Main
```

## Documentation

The design documents live in [`tambo-control-center-docs`](tambo-control-center-docs/). The implementation follows its milestone order and keeps the first increment limited to scaffolding.
