package app.tambo.infrastructure.diagnostics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class DebugLogger implements AutoCloseable {
    private final BufferedWriter writer;

    private DebugLogger(BufferedWriter writer) {
        this.writer = writer;
    }

    public static DebugLogger open(boolean enabled) {
        if (!enabled) {
            return new DebugLogger(null);
        }

        try {
            var stateHome = System.getenv("XDG_STATE_HOME");
            var base = stateHome == null || stateHome.isBlank()
                    ? Path.of(System.getProperty("user.home"), ".local", "state")
                    : Path.of(stateHome);
            var logPath = base.resolve("tambo").resolve("tambo.log");
            Files.createDirectories(logPath.getParent());
            var writer = Files.newBufferedWriter(
                    logPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            return new DebugLogger(writer);
        } catch (IOException exception) {
            return new DebugLogger(null);
        }
    }

    public synchronized void info(String message) {
        write("INFO", message);
    }

    public synchronized void error(String message, Throwable error) {
        write("ERROR", message + ": " + error);
    }

    private void write(String level, String message) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(Instant.now() + " " + level + " " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {
            // Debug logging must never change application behavior.
        }
    }

    @Override
    public synchronized void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ignored) {
            // Nothing useful can be done while shutting down diagnostics.
        }
    }
}
