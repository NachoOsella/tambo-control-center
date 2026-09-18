package app.tambo.application.logs;

import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.function.Consumer;

public interface ServiceLogSource {
    LogSession follow(
            ProjectContext project,
            LogScope scope,
            Consumer<String> onLine,
            Consumer<LogStreamEnd> onEnd
    ) throws IOException;
}
