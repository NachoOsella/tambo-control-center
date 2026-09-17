package app.tambo.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectLocatorTest {
    private final ProjectLocator locator = new ProjectLocator();

    @TempDir
    Path projectDirectory;

    @ParameterizedTest
    @ValueSource(strings = {
            "compose.yaml",
            "compose.yml",
            "docker-compose.yaml",
            "docker-compose.yml"
    })
    void findsEachSupportedComposeFilename(String filename) throws Exception {
        var composeFile = Files.createFile(projectDirectory.resolve(filename));

        var result = locator.locate(projectDirectory);

        assertEquals(composeFile, result.orElseThrow().composeFile());
        assertEquals(projectDirectory, result.orElseThrow().root());
    }

    @Test
    void findsComposeFileInAParentDirectory() throws Exception {
        var childDirectory = Files.createDirectory(projectDirectory.resolve("src"));
        var composeFile = Files.createFile(projectDirectory.resolve("docker-compose.yml"));

        var result = locator.locate(childDirectory);

        var context = result.orElseThrow();

        assertEquals(composeFile, context.composeFile());
        assertEquals(projectDirectory, context.root());
    }

    @Test
    void prefersComposeYamlWhenSeveralNamesExist() throws Exception {
        var preferredFile = Files.createFile(projectDirectory.resolve("compose.yaml"));
        Files.createFile(projectDirectory.resolve("docker-compose.yml"));

        var result = locator.locate(projectDirectory);

        assertEquals(preferredFile, result.orElseThrow().composeFile());
    }

    @Test
    void returnsEmptyWhenNoComposeFileExists() throws Exception {
        var childDirectory = Files.createDirectory(projectDirectory.resolve("src"));

        var result = locator.locate(childDirectory);

        assertTrue(result.isEmpty());
    }
}
