package app.tambo.infrastructure.compose;

import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.ProcessResult;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class ComposeCliPreflight {
    private static final Duration PREFLIGHT_TIMEOUT = Duration.ofSeconds(5);

    private final ProcessRunner processRunner;

    public ComposeCliPreflight(ProcessRunner processRunner) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
    }

    public void verify(ProjectContext project)
            throws IOException, InterruptedException, TimeoutException {
        var command = new Command(
                List.of("docker", "compose", "version"),
                project.root()
        );
        ProcessResult result;
        try {
            result = processRunner.run(command, PREFLIGHT_TIMEOUT);
        } catch (IOException exception) {
            throw new IOException("Docker CLI was not found in PATH", exception);
        }

        if (!result.succeeded()) {
            var details = result.stderr().strip();
            throw new IOException("Docker Compose is unavailable: "
                    + (details.isEmpty() ? "exit code " + result.exitCode() : details));
        }
    }
}
