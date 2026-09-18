package app.tambo.application.service;

import app.tambo.domain.service.ContainerInstance;
import app.tambo.domain.service.ResourceUsage;
import app.tambo.project.ProjectContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RefreshResourceStats implements AutoCloseable {
    private final ResourceStatsReader reader;
    private final ProjectContext project;
    private final ExecutorService executor;
    private CompletableFuture<Map<String, ResourceUsage>> inFlightRefresh;

    public RefreshResourceStats(ResourceStatsReader reader, ProjectContext project) {
        this(reader, project, Executors.newVirtualThreadPerTaskExecutor());
    }

    RefreshResourceStats(
            ResourceStatsReader reader,
            ProjectContext project,
            ExecutorService executor
    ) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.project = Objects.requireNonNull(project, "project");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public synchronized CompletableFuture<Map<String, ResourceUsage>> execute(
            List<ContainerInstance> containers
    ) {
        Objects.requireNonNull(containers, "containers");
        if (inFlightRefresh != null && !inFlightRefresh.isDone()) {
            return inFlightRefresh;
        }

        var refresh = CompletableFuture.supplyAsync(
                () -> loadSnapshot(List.copyOf(containers)),
                executor
        );
        inFlightRefresh = refresh;
        refresh.whenComplete((result, error) -> clearCompletedRefresh(refresh));
        return refresh;
    }

    private Map<String, ResourceUsage> loadSnapshot(List<ContainerInstance> containers) {
        try {
            return reader.readStats(project, containers);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CompletionException(exception);
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    private synchronized void clearCompletedRefresh(
            CompletableFuture<Map<String, ResourceUsage>> completedRefresh
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
