package app.tambo.application.service;

import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.project.ProjectContext;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshRuntimeSnapshotTest {
    private final ProjectContext project = new ProjectContext(
            Path.of("project"),
            Path.of("project", "compose.yaml")
    );
    private final List<ComposeService> services = List.of(
            new ComposeService("api", Optional.of("example/api:latest"))
    );

    @Test
    void loadsRuntimeSnapshotInTheBackground() throws Exception {
        var expected = Map.of("api", ServiceRuntime.notCreated());
        RuntimeSnapshotReader reader = (ignoredProject, ignoredServices) -> expected;

        try (var refresh = new RefreshRuntimeSnapshot(reader, project, services)) {
            assertEquals(expected, refresh.execute().get());
        }
    }

    @Test
    void reusesAnInFlightRefresh() throws Exception {
        var readCount = new AtomicInteger();
        var readStarted = new CountDownLatch(1);
        var finishRead = new CountDownLatch(1);
        RuntimeSnapshotReader reader = (ignoredProject, ignoredServices) -> {
            readCount.incrementAndGet();
            readStarted.countDown();
            finishRead.await();
            return Map.of("api", ServiceRuntime.notCreated());
        };

        try (var refresh = new RefreshRuntimeSnapshot(reader, project, services)) {
            var first = refresh.execute();
            assertTrue(readStarted.await(1, TimeUnit.SECONDS));
            var second = refresh.execute();

            assertSame(first, second);
            finishRead.countDown();
            first.get();
            assertEquals(1, readCount.get());
        }
    }

    @Test
    void preservesReaderFailureForTheUi() {
        RuntimeSnapshotReader reader = (ignoredProject, ignoredServices) -> {
            throw new IOException("Docker is unavailable");
        };

        try (var refresh = new RefreshRuntimeSnapshot(reader, project, services)) {
            var exception = assertThrows(ExecutionException.class, () -> refresh.execute().get());

            assertInstanceOf(IOException.class, exception.getCause());
            assertEquals("Docker is unavailable", exception.getCause().getMessage());
        }
    }
}
