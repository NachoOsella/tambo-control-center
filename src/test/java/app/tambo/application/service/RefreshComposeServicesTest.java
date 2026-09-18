package app.tambo.application.service;

import app.tambo.domain.service.ComposeService;
import app.tambo.project.ProjectContext;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RefreshComposeServicesTest {
    private final ProjectContext project = new ProjectContext(
            Path.of("project"),
            Path.of("project", "compose.yaml")
    );

    @Test
    void coalescesInFlightServiceReloads() throws Exception {
        var expected = List.of(new ComposeService("api", Optional.empty()));
        var started = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        ComposeServicesReader reader = ignored -> {
            started.countDown();
            finish.await();
            return expected;
        };

        try (var refresh = new RefreshComposeServices(reader, project)) {
            var first = refresh.execute();
            started.await(1, TimeUnit.SECONDS);
            var second = refresh.execute();

            assertSame(first, second);
            finish.countDown();
            assertEquals(expected, first.get());
        }
    }
}
