package app.tambo.application.service;

import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.project.ProjectContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RefreshRuntimeSnapshot implements AutoCloseable {
    private final RuntimeSnapshotReader reader;
    private final ProjectContext project;
    private final List<ComposeService> services;
    private final ExecutorService executor;

    public RefreshRuntimeSnapshot(
            RuntimeSnapshotReader reader,
            ProjectContext project,
            List<ComposeService> services
    ) {
        this(reader, project, services, Executors.newVirtualThreadPerTaskExecutor());
    }

    RefreshRuntimeSnapshot(
            RuntimeSnapshotReader reader,
            ProjectContext project,
            List<ComposeService> services,
            ExecutorService executor
    ) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.project = Objects.requireNonNull(project, "project");
        this.services = List.copyOf(services);
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<Map<String, ServiceRuntime>> execute() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return reader.readRuntime(project, services);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CompletionException(exception);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
