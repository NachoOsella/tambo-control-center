package app.tambo.project;

import java.nio.file.Path;
import java.util.Objects;

public record ProjectContext(Path root, Path composeFile) {
    public ProjectContext {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(composeFile, "composeFile");

        root = root.toAbsolutePath().normalize();
        composeFile = composeFile.toAbsolutePath().normalize();

        if (!root.equals(composeFile.getParent())) {
            throw new IllegalArgumentException("Compose file must be directly inside the project root");
        }
    }
}
