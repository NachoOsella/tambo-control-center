package app.tambo.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ComposeFileChangeDetector {
    private final Path composeFile;
    private FileStamp lastStamp;

    public ComposeFileChangeDetector(ProjectContext project) {
        this.composeFile = Objects.requireNonNull(project, "project").composeFile();
        this.lastStamp = readStamp();
    }

    public synchronized boolean hasChanged() {
        var currentStamp = readStamp();
        if (currentStamp.equals(lastStamp)) {
            return false;
        }
        lastStamp = currentStamp;
        return true;
    }

    private FileStamp readStamp() {
        try {
            return new FileStamp(
                    Files.getLastModifiedTime(composeFile).toMillis(),
                    Files.size(composeFile)
            );
        } catch (IOException exception) {
            return new FileStamp(-1, -1);
        }
    }

    private record FileStamp(long modifiedMillis, long size) {
    }
}
