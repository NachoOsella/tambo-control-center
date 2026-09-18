package app.tambo.application.service;

import app.tambo.domain.service.ContainerInstance;
import app.tambo.domain.service.HealthState;
import app.tambo.domain.service.ResourceUsage;
import app.tambo.domain.service.RuntimeState;
import app.tambo.project.ProjectContext;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshResourceStatsTest {
    private final ProjectContext project = new ProjectContext(
            Path.of("project"),
            Path.of("project", "compose.yaml")
    );
    private final ContainerInstance container = new ContainerInstance(
            "api-id",
            "demo-api-1",
            RuntimeState.RUNNING,
            HealthState.NOT_CONFIGURED,
            OptionalInt.empty(),
            List.of()
    );

    @Test
    void returnsTheLatestStatsSnapshot() throws Exception {
        var expected = Map.of(
                "demo-api-1",
                new ResourceUsage("demo-api-1", 1.2, "10MiB", "1GiB", 1.0,
                        "1kB", "2kB", 2)
        );
        ResourceStatsReader reader = (ignoredProject, ignoredContainers) -> expected;

        try (var refresh = new RefreshResourceStats(reader, project)) {
            assertEquals(expected, refresh.execute(List.of(container)).get());
        }
    }

    @Test
    void preservesReaderFailureForTheUi() {
        ResourceStatsReader reader = (ignoredProject, ignoredContainers) -> {
            throw new java.io.IOException("stats unavailable");
        };

        try (var refresh = new RefreshResourceStats(reader, project)) {
            var exception = assertThrows(ExecutionException.class,
                    () -> refresh.execute(List.of(container)).get());

            assertInstanceOf(java.io.IOException.class, exception.getCause());
            assertEquals("stats unavailable", exception.getCause().getMessage());
        }
    }
}
