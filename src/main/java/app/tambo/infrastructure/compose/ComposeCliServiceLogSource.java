package app.tambo.infrastructure.compose;

import app.tambo.application.logs.LogSession;
import app.tambo.application.logs.LogStreamEnd;
import app.tambo.application.logs.ServiceLogSource;
import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.StreamingProcess;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public final class ComposeCliServiceLogSource implements ServiceLogSource {
    @Override
    public LogSession follow(
            ProjectContext project,
            String serviceName,
            Consumer<String> onLine,
            Consumer<LogStreamEnd> onEnd
    ) throws IOException {
        var command = new Command(
                List.of(
                        "docker", "compose", "logs",
                        "--follow", "--tail", "200", "--timestamps", "--no-color",
                        serviceName
                ),
                project.root()
        );
        var process = StreamingProcess.start(
                command,
                onLine,
                exitCode -> onEnd.accept(LogStreamEnd.exited(exitCode)),
                error -> onEnd.accept(LogStreamEnd.failed(error.getMessage()))
        );
        return process::close;
    }
}
