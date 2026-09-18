package app.tambo.infrastructure.compose;

import app.tambo.application.service.LifecycleResult;
import app.tambo.application.service.OperationTarget;
import app.tambo.application.service.ServiceLifecycleGateway;
import app.tambo.application.service.ServiceOperation;
import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
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
            OperationTarget target,
            ServiceOperation operation
    ) throws InterruptedException {
        var command = new Command(arguments(target, operation), project.root());

        try {
            var result = processRunner.run(command, OPERATION_TIMEOUT);
            if (result.succeeded()) {
                return new LifecycleResult.Succeeded(target.label());
            }

            return failure(
                    target.label(),
                    LifecycleResult.FailureKind.COMMAND_FAILED,
                    OptionalInt.of(result.exitCode()),
                    result.stderr(),
                    "docker compose " + operation.commandName()
                            + " failed with exit code " + result.exitCode()
            );
        } catch (TimeoutException exception) {
            return failure(
                    target.label(),
                    LifecycleResult.FailureKind.TIMED_OUT,
                    OptionalInt.empty(),
                    exception.getMessage(),
                    "docker compose " + operation.commandName() + " timed out"
            );
        } catch (IOException exception) {
            return failure(
                    target.label(),
                    LifecycleResult.FailureKind.UNAVAILABLE,
                    OptionalInt.empty(),
                    exception.getMessage(),
                    "unable to run docker compose " + operation.commandName()
            );
        }
    }

    private List<String> arguments(OperationTarget target, ServiceOperation operation) {
        var composeCommand = switch (operation) {
            case UP_BUILD, RECREATE -> "up";
            default -> operation.commandName();
        };
        var arguments = new ArrayList<>(List.of(
                "docker", "compose", composeCommand
        ));
        switch (operation) {
            case UP, RECREATE -> arguments.addAll(List.of("-d"));
            case UP_BUILD -> arguments.addAll(List.of("-d", "--build"));
            case DOWN -> {
                arguments.add("--remove-orphans");
                return arguments;
            }
            case STOP, RESTART -> {
                // Compose adds the service name below.
            }
        }
        if (operation == ServiceOperation.RECREATE) {
            arguments.add("--force-recreate");
        }
        target.serviceName().ifPresent(arguments::add);
        return arguments;
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
