package app.tambo.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProjectLocator {
    private static final List<String> COMPOSE_FILENAMES = List.of(
            "compose.yaml",
            "compose.yml",
            "docker-compose.yaml",
            "docker-compose.yml"
    );

    public Optional<Path> locate(Path startDirectory) {
        Objects.requireNonNull(startDirectory, "startDirectory");

        var directory = startDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("start path is not a directory: " + directory);
        }

        while (directory != null) {
            for (var filename : COMPOSE_FILENAMES) {
                var composeFile = directory.resolve(filename);
                if (Files.isRegularFile(composeFile)) {
                    return Optional.of(composeFile);
                }
            }
            directory = directory.getParent();
        }

        return Optional.empty();
    }
}
