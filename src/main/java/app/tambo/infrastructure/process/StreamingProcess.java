package app.tambo.infrastructure.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class StreamingProcess implements AutoCloseable {
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(1);

    private final Process process;
    private final ExecutorService readerExecutor;
    private final AtomicBoolean closed = new AtomicBoolean();

    private StreamingProcess(
            Process process,
            Consumer<String> onLine,
            Consumer<Integer> onExit,
            Consumer<IOException> onError
    ) {
        this.process = process;
        this.readerExecutor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
        readerExecutor.submit(() -> readOutput(onLine, onExit, onError));
    }

    public static StreamingProcess start(
            Command command,
            Consumer<String> onLine,
            Consumer<Integer> onExit,
            Consumer<IOException> onError
    ) throws IOException {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(onLine, "onLine");
        Objects.requireNonNull(onExit, "onExit");
        Objects.requireNonNull(onError, "onError");

        var process = new ProcessBuilder(command.arguments())
                .directory(command.workingDirectory().toFile())
                .redirectErrorStream(true)
                .start();
        return new StreamingProcess(process, onLine, onExit, onError);
    }

    private void readOutput(
            Consumer<String> onLine,
            Consumer<Integer> onExit,
            Consumer<IOException> onError
    ) {
        try (var reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while (!closed.get() && (line = reader.readLine()) != null) {
                onLine.accept(line);
            }

            var exitCode = process.waitFor();
            if (!closed.get()) {
                onExit.accept(exitCode);
            }
        } catch (IOException exception) {
            if (!closed.get()) {
                onError.accept(exception);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        process.destroy();
        try {
            if (!process.waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor();
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        } finally {
            readerExecutor.shutdownNow();
        }
    }
}
