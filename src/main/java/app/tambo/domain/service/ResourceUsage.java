package app.tambo.domain.service;

import java.util.Objects;

public record ResourceUsage(
        String containerName,
        double cpuPercent,
        String memoryUsage,
        String memoryLimit,
        double memoryPercent,
        String networkInput,
        String networkOutput,
        int processCount
) {
    public ResourceUsage {
        Objects.requireNonNull(containerName, "containerName");
        Objects.requireNonNull(memoryUsage, "memoryUsage");
        Objects.requireNonNull(memoryLimit, "memoryLimit");
        Objects.requireNonNull(networkInput, "networkInput");
        Objects.requireNonNull(networkOutput, "networkOutput");
        if (containerName.isBlank()) {
            throw new IllegalArgumentException("container name cannot be blank");
        }
        if (cpuPercent < 0 || memoryPercent < 0 || processCount < 0) {
            throw new IllegalArgumentException("resource values cannot be negative");
        }
    }
}
