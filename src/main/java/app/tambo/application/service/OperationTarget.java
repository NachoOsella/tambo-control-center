package app.tambo.application.service;

import java.util.Objects;
import java.util.Optional;

public record OperationTarget(Optional<String> serviceName) {
    public OperationTarget {
        Objects.requireNonNull(serviceName, "serviceName");
        serviceName.ifPresent(name -> {
            if (name.isBlank()) {
                throw new IllegalArgumentException("service name cannot be blank");
            }
        });
    }

    public static OperationTarget service(String serviceName) {
        return new OperationTarget(Optional.of(serviceName));
    }

    public static OperationTarget allServices() {
        return new OperationTarget(Optional.empty());
    }

    public String label() {
        return serviceName.orElse("all services");
    }
}
