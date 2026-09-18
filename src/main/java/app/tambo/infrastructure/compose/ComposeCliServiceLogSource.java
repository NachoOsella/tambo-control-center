package app.tambo.infrastructure.compose;

import app.tambo.application.logs.LogMode;
import app.tambo.application.logs.LogScope;
import app.tambo.application.logs.LogSession;
import app.tambo.application.logs.LogStreamEnd;
import app.tambo.application.logs.ServiceLogSource;
import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.StreamingProcess;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ComposeCliServiceLogSource implements ServiceLogSource {
    @Override
    public LogSession follow(
            ProjectContext project,
            LogScope scope,
            Consumer<String> onLine,
            Consumer<LogStreamEnd> onEnd
    ) throws IOException {
        var arguments = new ArrayList<>(List.of(
                "docker", "compose", "logs",
                "--follow", "--tail", "200", "--timestamps", "--no-color"
        ));
        if (scope.mode() == LogMode.SELECTED_SERVICE) {
            arguments.add("--no-log-prefix");
            arguments.add(scope.serviceName().orElseThrow());
        }

        var process = StreamingProcess.start(
                new Command(arguments, project.root()),
                onLine,
                exitCode -> onEnd.accept(LogStreamEnd.exited(exitCode)),
                error -> onEnd.accept(LogStreamEnd.failed(error.getMessage()))
        );
        return process::close;
    }
}
