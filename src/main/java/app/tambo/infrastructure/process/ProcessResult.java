package app.tambo.infrastructure.process;

import java.util.Objects;

public record ProcessResult(int exitCode, String stdout, String stderr) {
    public ProcessResult {
        Objects.requireNonNull(stdout, "stdout");
        Objects.requireNonNull(stderr, "stderr");
    }

    public boolean succeeded() {
        return exitCode == 0;
    }
}
