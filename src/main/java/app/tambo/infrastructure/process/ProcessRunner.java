package app.tambo.infrastructure.process;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public final class ProcessRunner {
    public ProcessResult run(Command command, Duration timeout)
            throws IOException, InterruptedException, TimeoutException {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        var process = new ProcessBuilder(command.arguments())
                .directory(command.workingDirectory().toFile())
                .start();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = executor.submit(() -> read(process.getInputStream()));
            Future<String> stderr = executor.submit(() -> read(process.getErrorStream()));

            try {
                if (!process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor();
                    throw new TimeoutException("process timed out after " + timeout);
                }

                return new ProcessResult(
                        process.exitValue(),
                        output(stdout),
                        output(stderr)
                );
            } catch (InterruptedException exception) {
                process.destroyForcibly();
                process.waitFor();
                Thread.currentThread().interrupt();
                throw exception;
            }
        }
    }

    private static String read(InputStream input) throws IOException {
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String output(Future<String> output) throws IOException, InterruptedException {
        try {
            return output.get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("failed to read process output", exception.getCause());
        }
    }
}
