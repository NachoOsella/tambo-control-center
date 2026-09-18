package app.tambo.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposeFileChangeDetectorTest {
    @TempDir
    Path directory;

    @Test
    void detectsContentChanges() throws Exception {
        var composeFile = directory.resolve("compose.yaml");
        Files.writeString(composeFile, "services: {}\n");
        var detector = new ComposeFileChangeDetector(
                new ProjectContext(directory, composeFile)
        );

        assertFalse(detector.hasChanged());
        Files.writeString(composeFile, "services:\n  api: {}\n");

        assertTrue(detector.hasChanged());
        assertFalse(detector.hasChanged());
    }
}
