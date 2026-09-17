package app.tambo.project;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectContextTest {
    @Test
    void normalizesRootAndComposeFile() {
        var context = new ProjectContext(
                Path.of("project/./"),
                Path.of("project/compose.yaml")
        );

        assertEquals(Path.of("project").toAbsolutePath().normalize(), context.root());
        assertEquals(Path.of("project/compose.yaml").toAbsolutePath().normalize(), context.composeFile());
    }

    @Test
    void rejectsComposeFileOutsideProjectRoot() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectContext(Path.of("project"), Path.of("other/compose.yaml"))
        );
    }
}
