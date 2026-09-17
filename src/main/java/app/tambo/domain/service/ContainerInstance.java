package app.tambo.domain.service;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public record ContainerInstance(
        String id,
        String name,
        RuntimeState runtimeState,
        HealthState healthState,
        OptionalInt exitCode,
        List<PublishedPort> ports
) {
    public ContainerInstance {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(runtimeState, "runtimeState");
        Objects.requireNonNull(healthState, "healthState");
        Objects.requireNonNull(exitCode, "exitCode");
        Objects.requireNonNull(ports, "ports");

        if (id.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("container id and name cannot be blank");
        }
        ports = List.copyOf(ports);
    }
}
