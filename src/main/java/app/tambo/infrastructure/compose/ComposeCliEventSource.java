package app.tambo.infrastructure.compose;

import app.tambo.application.events.ComposeEventSession;
import app.tambo.application.events.ComposeEventSource;
import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.StreamingProcess;
import app.tambo.project.ProjectContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ComposeCliEventSource implements ComposeEventSource {
    private final ObjectMapper objectMapper;

    public ComposeCliEventSource(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public ComposeEventSession follow(
            ProjectContext project,
            Consumer<String> onEvent,
            Consumer<String> onEnd
    ) throws IOException {
        var process = StreamingProcess.start(
                new Command(
                        List.of("docker", "compose", "events", "--json"),
                        project.root()
                ),
                line -> parseEvent(line, onEvent, onEnd),
                exitCode -> onEnd.accept("Compose event stream exited with code " + exitCode),
                error -> onEnd.accept(error.getMessage() == null
                        ? "Compose event stream failed"
                        : error.getMessage())
        );
        return process::close;
    }

    private void parseEvent(String line, Consumer<String> onEvent, Consumer<String> onEnd) {
        try {
            var event = objectMapper.readTree(line);
            if (event != null && event.isObject()) {
                onEvent.accept(formatEvent(event));
            }
        } catch (IOException exception) {
            onEnd.accept("Compose event stream returned invalid JSON: " + exception.getMessage());
        }
    }

    private String formatEvent(com.fasterxml.jackson.databind.JsonNode event) {
        var service = event.path("service").asText("");
        var action = event.path("action").asText("");
        var type = event.path("type").asText("event");
        var summary = service.isBlank() ? type : service;
        return action.isBlank() ? summary : summary + " " + action;
    }
}
