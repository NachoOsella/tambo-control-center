package app.tambo.application.service;

import app.tambo.project.ProjectContext;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RunServiceOperation implements AutoCloseable {
    private final ServiceLifecycleGateway lifecycle;
    private final ProjectContext project;
    private final ExecutorService executor;
    private final Set<String> activeServices = ConcurrentHashMap.newKeySet();
    private final Object operationLock = new Object();
    private boolean globalOperationActive;

    public RunServiceOperation(ServiceLifecycleGateway lifecycle, ProjectContext project) {
        this(lifecycle, project, Executors.newVirtualThreadPerTaskExecutor());
    }

    RunServiceOperation(
            ServiceLifecycleGateway lifecycle,
            ProjectContext project,
            ExecutorService executor
    ) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.project = Objects.requireNonNull(project, "project");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<LifecycleResult> execute(
            String serviceName,
            ServiceOperation operation
    ) {
        return execute(OperationTarget.service(serviceName), operation);
    }

    public CompletableFuture<LifecycleResult> executeAll(ServiceOperation operation) {
        return execute(OperationTarget.allServices(), operation);
    }

    private CompletableFuture<LifecycleResult> execute(
            OperationTarget target,
            ServiceOperation operation
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(operation, "operation");
        if (!acquire(target)) {
            return CompletableFuture.completedFuture(new LifecycleResult.Rejected(target.label()));
        }

        return CompletableFuture
                .supplyAsync(() -> run(target, operation), executor)
                .whenComplete((result, error) -> release(target));
    }

    private boolean acquire(OperationTarget target) {
        synchronized (operationLock) {
            if (target.serviceName().isEmpty()) {
                if (globalOperationActive || !activeServices.isEmpty()) {
                    return false;
                }
                globalOperationActive = true;
                return true;
            }

            if (globalOperationActive || !activeServices.add(target.serviceName().orElseThrow())) {
                return false;
            }
            return true;
        }
    }

    private void release(OperationTarget target) {
        synchronized (operationLock) {
            if (target.serviceName().isEmpty()) {
                globalOperationActive = false;
            } else {
                activeServices.remove(target.serviceName().orElseThrow());
            }
        }
    }

    private LifecycleResult run(OperationTarget target, ServiceOperation operation) {
        try {
            return lifecycle.execute(project, target, operation);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new LifecycleResult.Failed(
                    target.label(),
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
