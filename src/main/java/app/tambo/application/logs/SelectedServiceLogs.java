package app.tambo.application.logs;

import app.tambo.domain.logs.LogBuffer;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class SelectedServiceLogs implements AutoCloseable {
    private final ServiceLogSource source;
    private final ProjectContext project;
    private final LogBuffer buffer;

    private long generation;
    private LogSession session;
    private String serviceName = "";
    private LogStatus status = LogStatus.DISCONNECTED;
    private String message = "";
    private Runnable onChange = () -> { };

    public SelectedServiceLogs(ServiceLogSource source, ProjectContext project, int capacity) {
        this.source = Objects.requireNonNull(source, "source");
        this.project = Objects.requireNonNull(project, "project");
        this.buffer = new LogBuffer(capacity);
    }

    public synchronized void follow(String nextServiceName, Runnable nextOnChange) {
        Objects.requireNonNull(nextServiceName, "nextServiceName");
        Objects.requireNonNull(nextOnChange, "nextOnChange");

        generation++;
        closeSession();
        buffer.clear();
        serviceName = nextServiceName;
        status = LogStatus.CONNECTING;
        message = "";
        onChange = nextOnChange;
        long sessionGeneration = generation;

        try {
            session = source.follow(
                    project,
                    nextServiceName,
                    line -> acceptLine(sessionGeneration, line),
                    end -> acceptEnd(sessionGeneration, end)
            );
        } catch (IOException exception) {
            status = LogStatus.FAILED;
            message = exception.getMessage() == null
                    ? "unable to start log stream"
                    : exception.getMessage();
            onChange.run();
        }
    }

    public synchronized LogView view() {
        return new LogView(serviceName, status, buffer.snapshot(), message);
    }

    private void acceptLine(long sessionGeneration, String line) {
        Runnable notify;
        synchronized (this) {
            if (sessionGeneration != generation) {
                return;
            }
            buffer.add(line);
            status = LogStatus.FOLLOWING;
            notify = onChange;
        }
        notify.run();
    }

    private void acceptEnd(long sessionGeneration, LogStreamEnd end) {
        Runnable notify;
        synchronized (this) {
            if (sessionGeneration != generation) {
                return;
            }
            status = end.exitCode().isPresent() && end.exitCode().getAsInt() == 0
                    ? LogStatus.DISCONNECTED
                    : LogStatus.FAILED;
            message = end.message();
            notify = onChange;
        }
        notify.run();
    }

    @Override
    public synchronized void close() {
        generation++;
        closeSession();
        status = LogStatus.DISCONNECTED;
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    public enum LogStatus {
        CONNECTING,
        FOLLOWING,
        DISCONNECTED,
        FAILED
    }

    public record LogView(
            String serviceName,
            LogStatus status,
            List<String> lines,
            String message
    ) {
        public LogView {
            lines = List.copyOf(lines);
        }
    }
}
