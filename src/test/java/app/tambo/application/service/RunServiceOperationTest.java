package app.tambo.application.service;

import app.tambo.project.ProjectContext;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunServiceOperationTest {
    private final ProjectContext project = new ProjectContext(
            Path.of("project"),
            Path.of("project", "compose.yaml")
    );

    @Test
    void returnsTheGatewayResult() throws Exception {
        ServiceLifecycleGateway lifecycle = (ignoredProject, target, ignoredOperation) ->
                new LifecycleResult.Succeeded(target.label());

        try (var operations = new RunServiceOperation(lifecycle, project)) {
            var result = operations.execute("api", ServiceOperation.UP).get();

            assertInstanceOf(LifecycleResult.Succeeded.class, result);
        }
    }

    @Test
    void rejectsAnIndividualOperationWhileGlobalOperationIsActive() throws Exception {
        var operationStarted = new CountDownLatch(1);
        var finishOperation = new CountDownLatch(1);
        ServiceLifecycleGateway lifecycle = (ignoredProject, target, ignoredOperation) -> {
            operationStarted.countDown();
            finishOperation.await();
            return new LifecycleResult.Succeeded(target.label());
        };

        try (var operations = new RunServiceOperation(lifecycle, project)) {
            var global = operations.executeAll(ServiceOperation.STOP);
            assertTrue(operationStarted.await(1, TimeUnit.SECONDS));
            var individual = operations.execute("api", ServiceOperation.UP).get();

            assertInstanceOf(LifecycleResult.Rejected.class, individual);
            finishOperation.countDown();
            global.get();
        }
    }

    @Test
    void rejectsAnotherOperationForTheSameService() throws Exception {
        var operationCount = new AtomicInteger();
        var operationStarted = new CountDownLatch(1);
        var finishOperation = new CountDownLatch(1);
        ServiceLifecycleGateway lifecycle = (ignoredProject, target, ignoredOperation) -> {
            operationCount.incrementAndGet();
            operationStarted.countDown();
            finishOperation.await();
            return new LifecycleResult.Succeeded(target.label());
        };

        try (var operations = new RunServiceOperation(lifecycle, project)) {
            var first = operations.execute("api", ServiceOperation.UP);
            assertTrue(operationStarted.await(1, TimeUnit.SECONDS));
            var second = operations.execute("api", ServiceOperation.STOP).get();

            assertInstanceOf(LifecycleResult.Rejected.class, second);
            finishOperation.countDown();
            first.get();
            assertEquals(1, operationCount.get());
        }
    }
}
