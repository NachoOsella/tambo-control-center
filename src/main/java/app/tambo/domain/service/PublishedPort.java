package app.tambo.domain.service;

import java.util.Objects;

public record PublishedPort(String host, int targetPort, int publishedPort, String protocol) {
    public PublishedPort {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(protocol, "protocol");
        if (targetPort <= 0 || publishedPort <= 0) {
            throw new IllegalArgumentException("ports must be positive");
        }
    }
}
