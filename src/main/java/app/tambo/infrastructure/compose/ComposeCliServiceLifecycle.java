package app.tambo.infrastructure.compose;

import app.tambo.application.service.LifecycleResult;
import app.tambo.application.service.ServiceLifecycleGateway;
import app.tambo.application.service.ServiceOperation;
import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.TimeoutException;

public final class ComposeCliServiceLifecycle implements ServiceLifecycleGateway {
    private static final Duration OPERATION_TIMEOUT = Duration.ofMinutes(2);

    private final ProcessRunner processRunner;

    public ComposeCliServiceLifecycle(ProcessRunner processRunner) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
    }

    @Override
    public LifecycleResult execute(
            ProjectContext project,
            String serviceName,
            ServiceOperation operation
    ) throws InterruptedException {
        var command = new Command(arguments(operation, serviceName), project.root());

        try {
            var result = processRunner.run(command, OPERATION_TIMEOUT);
            if (result.succeeded()) {
                return new LifecycleResult.Succeeded(serviceName);
            }

            return failure(
                    serviceName,
                    LifecycleResult.FailureKind.COMMAND_FAILED,
                    OptionalInt.of(result.exitCode()),
                    result.stderr(),
                    "docker compose " + operation.commandName()
                            + " failed with exit code " + result.exitCode()
            );
        } catch (TimeoutException exception) {
            return failure(
                    serviceName,
                    LifecycleResult.FailureKind.TIMED_OUT,
                    OptionalInt.empty(),
                    exception.getMessage(),
                    "docker compose " + operation.commandName() + " timed out"
            );
        } catch (IOException exception) {
            return failure(
                    serviceName,
                    LifecycleResult.FailureKind.UNAVAILABLE,
                    OptionalInt.empty(),
                    exception.getMessage(),
                    "unable to run docker compose " + operation.commandName()
            );
        }
    }

    private List<String> arguments(ServiceOperation operation, String serviceName) {
        return switch (operation) {
            case UP -> List.of("docker", "compose", "up", "-d", serviceName);
            case STOP -> List.of("docker", "compose", "stop", serviceName);
            case RESTART -> List.of("docker", "compose", "restart", serviceName);
        };
    }

    private LifecycleResult.Failed failure(
            String serviceName,
            LifecycleResult.FailureKind kind,
            OptionalInt exitCode,
            String details,
            String fallback
    ) {
        var message = details == null ? "" : details.strip();
        return new LifecycleResult.Failed(
                serviceName,
                kind,
                exitCode,
                message.isEmpty() ? fallback : message
        );
    }
}
