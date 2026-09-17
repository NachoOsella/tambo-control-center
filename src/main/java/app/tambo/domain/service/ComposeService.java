package app.tambo.domain.service;

import java.util.Objects;
import java.util.Optional;

public record ComposeService(String name, Optional<String> image) {
    public ComposeService {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(image, "image");

        if (name.isBlank()) {
            throw new IllegalArgumentException("service name cannot be blank");
        }
    }
}
