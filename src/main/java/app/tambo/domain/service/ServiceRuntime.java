package app.tambo.domain.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

public record ServiceRuntime(
        RuntimeState runtimeState,
        HealthState healthState,
        List<ContainerInstance> instances
) {
    public ServiceRuntime {
        Objects.requireNonNull(runtimeState, "runtimeState");
        Objects.requireNonNull(healthState, "healthState");
        Objects.requireNonNull(instances, "instances");
        instances = List.copyOf(instances);
    }

    public static ServiceRuntime notCreated() {
        return new ServiceRuntime(RuntimeState.NOT_CREATED, HealthState.NOT_CONFIGURED, List.of());
    }

    public static ServiceRuntime fromInstances(List<ContainerInstance> instances) {
        var copy = List.copyOf(instances);
        if (copy.isEmpty()) {
            return notCreated();
        }

        return new ServiceRuntime(
                aggregateRuntime(copy),
                aggregateHealth(copy),
                copy
        );
    }

    public int containerCount() {
        return instances.size();
    }

    public OptionalInt exitCode() {
        for (var instance : instances) {
            if (instance.exitCode().isPresent()) {
                return instance.exitCode();
            }
        }
        return OptionalInt.empty();
    }

    public List<PublishedPort> publishedPorts() {
        Set<PublishedPort> ports = new LinkedHashSet<>();
        for (var instance : instances) {
            ports.addAll(instance.ports());
        }
        return List.copyOf(ports);
    }

    private static RuntimeState aggregateRuntime(List<ContainerInstance> instances) {
        var priority = List.of(
                RuntimeState.RUNNING,
                RuntimeState.RESTARTING,
                RuntimeState.CREATED,
                RuntimeState.PAUSED,
                RuntimeState.REMOVING,
                RuntimeState.DEAD,
                RuntimeState.EXITED,
                RuntimeState.UNKNOWN
        );
        return priority.stream()
                .filter(candidate -> instances.stream()
                        .anyMatch(instance -> instance.runtimeState() == candidate))
                .findFirst()
                .orElse(RuntimeState.UNKNOWN);
    }

    private static HealthState aggregateHealth(List<ContainerInstance> instances) {
        var priority = List.of(
                HealthState.UNHEALTHY,
                HealthState.STARTING,
                HealthState.HEALTHY,
                HealthState.UNKNOWN,
                HealthState.NOT_CONFIGURED
        );
        return priority.stream()
                .filter(candidate -> instances.stream()
                        .anyMatch(instance -> instance.healthState() == candidate))
                .findFirst()
                .orElse(HealthState.UNKNOWN);
    }
}
