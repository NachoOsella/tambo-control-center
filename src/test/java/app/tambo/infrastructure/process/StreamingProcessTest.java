package app.tambo.infrastructure.process;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingProcessTest {
    @TempDir
    Path workingDirectory;

    @Test
    void deliversLinesUntilTheProcessExits() throws Exception {
        var lines = new CopyOnWriteArrayList<String>();
        var exited = new CountDownLatch(1);
        var command = new Command(
                List.of("sh", "-c", "printf 'first\\nsecond\\n'"),
                workingDirectory
        );

        try (var ignored = StreamingProcess.start(
                command,
                lines::add,
                exitCode -> exited.countDown(),
                error -> { }
        )) {
            assertTrue(exited.await(1, TimeUnit.SECONDS));
        }

        assertEquals(List.of("first", "second"), lines);
    }

    @Test
    void closingStopsFurtherOutput() throws Exception {
        var lines = new CopyOnWriteArrayList<String>();
        var firstLine = new CountDownLatch(1);
        var command = new Command(
                List.of("sh", "-c", "printf 'first\\n'; sleep 1; printf 'late\\n'"),
                workingDirectory
        );
        var process = StreamingProcess.start(
                command,
                line -> {
                    lines.add(line);
                    firstLine.countDown();
                },
                exitCode -> { },
                error -> { }
        );

        assertTrue(firstLine.await(1, TimeUnit.SECONDS));
        process.close();
        Thread.sleep(100);

        assertEquals(List.of("first"), lines);
    }
}
