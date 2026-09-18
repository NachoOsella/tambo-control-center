package app.tambo.application.events;

import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

public final class ComposeEventObserver implements AutoCloseable {
    private final ComposeEventSource source;
    private final ProjectContext project;

    private long generation;
    private ComposeEventSession session;

    public ComposeEventObserver(ComposeEventSource source, ProjectContext project) {
        this.source = Objects.requireNonNull(source, "source");
        this.project = Objects.requireNonNull(project, "project");
    }

    public synchronized void start(Consumer<String> onEvent, Consumer<String> onEnd) {
        Objects.requireNonNull(onEvent, "onEvent");
        Objects.requireNonNull(onEnd, "onEnd");

        generation++;
        closeSession();
        var observerGeneration = generation;
        try {
            session = source.follow(
                    project,
                    message -> acceptEvent(observerGeneration, onEvent, message),
                    message -> acceptEnd(observerGeneration, onEnd, message)
            );
        } catch (IOException exception) {
            onEnd.accept(exception.getMessage() == null
                    ? "unable to start Compose event stream"
                    : exception.getMessage());
        }
    }

    private void acceptEvent(
            long observerGeneration,
            Consumer<String> onEvent,
            String message
    ) {
        synchronized (this) {
            if (observerGeneration != generation) {
                return;
            }
        }
        onEvent.accept(message);
    }

    private void acceptEnd(long observerGeneration, Consumer<String> onEnd, String message) {
        synchronized (this) {
            if (observerGeneration != generation) {
                return;
            }
            session = null;
        }
        onEnd.accept(message);
    }

    @Override
    public synchronized void close() {
        generation++;
        closeSession();
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}
