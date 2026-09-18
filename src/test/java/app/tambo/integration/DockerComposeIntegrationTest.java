package app.tambo.integration;

import app.tambo.infrastructure.compose.ComposeCliConfigReader;
import app.tambo.infrastructure.compose.ComposeCliRuntimeReader;
import app.tambo.infrastructure.compose.ComposeCliStatsReader;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectLocator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "tambo.integration", matches = "true")
class DockerComposeIntegrationTest {
    private final ProcessRunner processRunner = new ProcessRunner();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsTheDevelopmentComposeProjectWithoutMutatingIt() throws Exception {
        var project = new ProjectLocator()
                .locate(Path.of(".").toAbsolutePath().normalize())
                .orElseThrow();
        var configReader = new ComposeCliConfigReader(processRunner, objectMapper);
        var runtimeReader = new ComposeCliRuntimeReader(processRunner, objectMapper);
        var statsReader = new ComposeCliStatsReader(processRunner, objectMapper);

        var services = configReader.readServices(project);
        var runtime = runtimeReader.readRuntime(project, services);
        var containers = runtime.values().stream()
                .flatMap(service -> service.instances().stream())
                .toList();
        var stats = statsReader.readStats(project, containers);

        assertFalse(services.isEmpty());
        assertEquals(services.size(), runtime.size());
        assertTrue(stats.keySet().stream().allMatch(
                containerName -> containers.stream().anyMatch(
                        container -> container.name().equals(containerName)
                )
        ));
    }
}
