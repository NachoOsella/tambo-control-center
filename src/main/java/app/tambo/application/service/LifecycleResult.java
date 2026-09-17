package app.tambo.application.service;

import java.util.Objects;
import java.util.OptionalInt;

public sealed interface LifecycleResult {
    String serviceName();

    record Succeeded(String serviceName) implements LifecycleResult {
        public Succeeded {
            requireServiceName(serviceName);
        }
    }

    record Failed(
            String serviceName,
            FailureKind kind,
            OptionalInt exitCode,
            String message
    ) implements LifecycleResult {
        public Failed {
            requireServiceName(serviceName);
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(exitCode, "exitCode");
            Objects.requireNonNull(message, "message");
            if (message.isBlank()) {
                throw new IllegalArgumentException("failure message cannot be blank");
            }
        }
    }

    record Rejected(String serviceName) implements LifecycleResult {
        public Rejected {
            requireServiceName(serviceName);
        }
    }

    enum FailureKind {
        COMMAND_FAILED,
        TIMED_OUT,
        UNAVAILABLE,
        CANCELLED
    }

    private static void requireServiceName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName");
        if (serviceName.isBlank()) {
            throw new IllegalArgumentException("service name cannot be blank");
        }
    }
}
