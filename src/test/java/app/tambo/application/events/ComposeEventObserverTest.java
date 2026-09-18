package app.tambo.application.events;

import app.tambo.project.ProjectContext;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposeEventObserverTest {
    private final ProjectContext project = new ProjectContext(
            Path.of("project"),
            Path.of("project", "compose.yaml")
    );

    @Test
    void forwardsEventsAndIgnoresEventsFromReplacedStreams() {
        var source = new FakeEventSource();
        var observer = new ComposeEventObserver(source, project);
        var eventCount = new AtomicInteger();

        observer.start(eventCount::incrementAndGet, ignored -> { });
        var first = source.sessions.getFirst();
        first.emit();

        observer.start(eventCount::incrementAndGet, ignored -> { });
        var second = source.sessions.getLast();
        first.emit();
        second.emit();

        assertTrue(first.closed);
        assertEquals(2, eventCount.get());
        observer.close();
    }

    private static final class FakeEventSource implements ComposeEventSource {
        private final List<FakeSession> sessions = new ArrayList<>();

        @Override
        public ComposeEventSession follow(
                ProjectContext project,
                Runnable onEvent,
                Consumer<String> onEnd
        ) {
            var session = new FakeSession(onEvent);
            sessions.add(session);
            return session;
        }
    }

    private static final class FakeSession implements ComposeEventSession {
        private final Runnable onEvent;
        private boolean closed;

        private FakeSession(Runnable onEvent) {
            this.onEvent = onEvent;
        }

        private void emit() {
            onEvent.run();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
