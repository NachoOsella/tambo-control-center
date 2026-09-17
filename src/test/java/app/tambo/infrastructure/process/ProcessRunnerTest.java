package app.tambo.infrastructure.process;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProcessRunnerTest {
    private final ProcessRunner runner = new ProcessRunner();

    @TempDir
    Path workingDirectory;

    @Test
    void capturesExitCodeAndBothOutputStreams() throws Exception {
        var command = new Command(
                List.of("sh", "-c", "printf output; printf error >&2; exit 7"),
                workingDirectory
        );

        var result = runner.run(command, Duration.ofSeconds(5));

        assertEquals(7, result.exitCode());
        assertEquals("output", result.stdout());
        assertEquals("error", result.stderr());
        assertFalse(result.succeeded());
    }

    @Test
    void stopsACommandThatExceedsTheTimeout() {
        var command = new Command(
                List.of("sh", "-c", "while true; do :; done"),
                workingDirectory
        );

        assertThrows(
                TimeoutException.class,
                () -> runner.run(command, Duration.ofMillis(100))
        );
    }
}
