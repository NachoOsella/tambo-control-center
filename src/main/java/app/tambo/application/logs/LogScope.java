package app.tambo.application.logs;

import java.util.Objects;
import java.util.Optional;

public record LogScope(LogMode mode, Optional<String> serviceName) {
    public LogScope {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(serviceName, "serviceName");

        if (mode == LogMode.SELECTED_SERVICE && serviceName.isEmpty()) {
            throw new IllegalArgumentException("selected log scope needs a service name");
        }
        if (mode == LogMode.ALL_SERVICES && serviceName.isPresent()) {
            throw new IllegalArgumentException("all-service log scope cannot have a service name");
        }
    }

    public static LogScope selected(String serviceName) {
        return new LogScope(LogMode.SELECTED_SERVICE, Optional.of(serviceName));
    }

    public static LogScope all() {
        return new LogScope(LogMode.ALL_SERVICES, Optional.empty());
    }

    public String label() {
        return mode == LogMode.ALL_SERVICES
                ? "all services"
                : serviceName.orElseThrow();
    }
}
