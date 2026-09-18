package app.tambo.domain.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ComposeService(
        String name,
        Optional<String> image,
        List<String> networks,
        List<String> environmentVariables,
        List<String> volumes,
        Optional<String> restartPolicy,
        List<String> dependencies,
        List<String> profiles
) {
    public ComposeService(String name, Optional<String> image) {
        this(name, image, List.of(), List.of(), List.of(), Optional.empty(), List.of(), List.of());
    }

    public ComposeService {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(networks, "networks");
        Objects.requireNonNull(environmentVariables, "environmentVariables");
        Objects.requireNonNull(volumes, "volumes");
        Objects.requireNonNull(restartPolicy, "restartPolicy");
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(profiles, "profiles");

        if (name.isBlank()) {
            throw new IllegalArgumentException("service name cannot be blank");
        }
        networks = List.copyOf(networks);
        environmentVariables = List.copyOf(environmentVariables);
        volumes = List.copyOf(volumes);
        dependencies = List.copyOf(dependencies);
        profiles = List.copyOf(profiles);
    }
}
