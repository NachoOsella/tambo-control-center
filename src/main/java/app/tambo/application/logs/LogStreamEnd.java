package app.tambo.application.logs;

import java.util.Objects;
import java.util.OptionalInt;

public record LogStreamEnd(OptionalInt exitCode, String message) {
    public LogStreamEnd {
        Objects.requireNonNull(exitCode, "exitCode");
        Objects.requireNonNull(message, "message");
    }

    public static LogStreamEnd exited(int exitCode) {
        return new LogStreamEnd(OptionalInt.of(exitCode), "log stream exited");
    }

    public static LogStreamEnd failed(String message) {
        return new LogStreamEnd(OptionalInt.empty(), message);
    }
}
