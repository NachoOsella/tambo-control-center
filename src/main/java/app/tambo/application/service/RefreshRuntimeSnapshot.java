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
    private List<ComposeService> services;
    private final ExecutorService executor;
    private CompletableFuture<Map<String, ServiceRuntime>> inFlightRefresh;

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

    public synchronized CompletableFuture<Map<String, ServiceRuntime>> execute() {
        if (inFlightRefresh != null && !inFlightRefresh.isDone()) {
            return inFlightRefresh;
        }

        var servicesSnapshot = services;
        var refresh = CompletableFuture.supplyAsync(
                () -> loadSnapshot(servicesSnapshot),
                executor
        );
        inFlightRefresh = refresh;
        refresh.whenComplete((result, error) -> clearCompletedRefresh(refresh));
        return refresh;
    }

    private Map<String, ServiceRuntime> loadSnapshot(List<ComposeService> services) {
        try {
            return reader.readRuntime(project, services);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CompletionException(exception);
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    public synchronized void replaceServices(List<ComposeService> services) {
        this.services = List.copyOf(services);
    }

    private synchronized void clearCompletedRefresh(
            CompletableFuture<Map<String, ServiceRuntime>> completedRefresh
    ) {
        if (inFlightRefresh == completedRefresh) {
            inFlightRefresh = null;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
