package app.tambo.application.service;

import app.tambo.project.ProjectContext;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpService implements AutoCloseable {
    private final ServiceLifecycleGateway lifecycle;
    private final ProjectContext project;
    private final ExecutorService executor;
    private final Set<String> activeServices = ConcurrentHashMap.newKeySet();

    public UpService(ServiceLifecycleGateway lifecycle, ProjectContext project) {
        this(lifecycle, project, Executors.newVirtualThreadPerTaskExecutor());
    }

    UpService(
            ServiceLifecycleGateway lifecycle,
            ProjectContext project,
            ExecutorService executor
    ) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.project = Objects.requireNonNull(project, "project");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<LifecycleResult> execute(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName");
        if (!activeServices.add(serviceName)) {
            return CompletableFuture.completedFuture(new LifecycleResult.Rejected(serviceName));
        }

        return CompletableFuture
                .supplyAsync(() -> runUp(serviceName), executor)
                .whenComplete((result, error) -> activeServices.remove(serviceName));
    }

    private LifecycleResult runUp(String serviceName) {
        try {
            return lifecycle.up(project, serviceName);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new LifecycleResult.Failed(
                    serviceName,
                    LifecycleResult.FailureKind.CANCELLED,
                    OptionalInt.empty(),
                    "operation cancelled"
            );
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
