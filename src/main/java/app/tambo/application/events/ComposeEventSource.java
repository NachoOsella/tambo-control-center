package app.tambo.application.events;

import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.function.Consumer;

public interface ComposeEventSource {
    ComposeEventSession follow(
            ProjectContext project,
            Runnable onEvent,
            Consumer<String> onEnd
    ) throws IOException;
}
