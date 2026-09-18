package app.tambo.application.service;

import app.tambo.domain.service.ComposeService;
import app.tambo.project.ProjectContext;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RefreshComposeServices implements AutoCloseable {
    private final ComposeServicesReader reader;
    private final ProjectContext project;
    private final ExecutorService executor;
    private CompletableFuture<List<ComposeService>> inFlight;

    public RefreshComposeServices(ComposeServicesReader reader, ProjectContext project) {
        this(reader, project, Executors.newVirtualThreadPerTaskExecutor());
    }

    RefreshComposeServices(
            ComposeServicesReader reader,
            ProjectContext project,
            ExecutorService executor
    ) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.project = Objects.requireNonNull(project, "project");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public synchronized CompletableFuture<List<ComposeService>> execute() {
        if (inFlight != null && !inFlight.isDone()) {
            return inFlight;
        }
        var refresh = CompletableFuture.supplyAsync(this::readServices, executor);
        inFlight = refresh;
        refresh.whenComplete((result, error) -> clearCompletedRefresh(refresh));
        return refresh;
    }

    private List<ComposeService> readServices() {
        try {
            return reader.readServices(project);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CompletionException(exception);
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    private synchronized void clearCompletedRefresh(
            CompletableFuture<List<ComposeService>> completedRefresh
    ) {
        if (inFlight == completedRefresh) {
            inFlight = null;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
