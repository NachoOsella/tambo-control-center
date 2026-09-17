package app.tambo.infrastructure.process;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record Command(List<String> arguments, Path workingDirectory) {
    public Command {
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(workingDirectory, "workingDirectory");

        arguments = List.copyOf(arguments);
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("command must contain at least one argument");
        }
        if (arguments.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("command arguments cannot be blank");
        }

        workingDirectory = workingDirectory.toAbsolutePath().normalize();
    }
}
