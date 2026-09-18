package app.tambo.application.logs;

import app.tambo.project.ProjectContext;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectedServiceLogsTest {
    private final ProjectContext project = new ProjectContext(
            Path.of("project"),
            Path.of("project", "compose.yaml")
    );

    @Test
    void switchingServiceReplacesTheSessionAndRejectsLateLines() {
        var source = new FakeLogSource();
        var logs = new SelectedServiceLogs(source, project, 10);

        logs.follow("api", () -> { });
        var apiSession = source.sessions.getFirst();
        apiSession.emit("api line");

        logs.follow("worker", () -> { });
        var workerSession = source.sessions.getLast();
        apiSession.emit("late api line");
        workerSession.emit("worker line");

        assertTrue(apiSession.closed);
        assertEquals("worker", logs.view().serviceName());
        assertEquals(List.of("worker line"), logs.view().lines());
    }

    private static final class FakeLogSource implements ServiceLogSource {
        private final List<FakeSession> sessions = new ArrayList<>();

        @Override
        public LogSession follow(
                ProjectContext project,
                String serviceName,
                Consumer<String> onLine,
                Consumer<LogStreamEnd> onEnd
        ) {
            var session = new FakeSession(onLine);
            sessions.add(session);
            return session;
        }
    }

    private static final class FakeSession implements LogSession {
        private final Consumer<String> onLine;
        private boolean closed;

        private FakeSession(Consumer<String> onLine) {
            this.onLine = onLine;
        }

        private void emit(String line) {
            onLine.accept(line);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
