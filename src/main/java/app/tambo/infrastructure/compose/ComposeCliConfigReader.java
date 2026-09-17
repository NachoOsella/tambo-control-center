package app.tambo.infrastructure.compose;

import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public final class ComposeCliConfigReader {
    private static final Duration CONFIG_TIMEOUT = Duration.ofSeconds(10);

    private final ProcessRunner processRunner;
    private final ObjectMapper objectMapper;

    public ComposeCliConfigReader(ProcessRunner processRunner, ObjectMapper objectMapper) {
        this.processRunner = processRunner;
        this.objectMapper = objectMapper;
    }

    public List<String> readServices(ProjectContext project)
            throws IOException, InterruptedException, TimeoutException {
        var command = new Command(
                List.of("docker", "compose", "config", "--format", "json"),
                project.root()
        );
        var result = processRunner.run(command, CONFIG_TIMEOUT);

        if (!result.succeeded()) {
            var details = result.stderr().strip();
            throw new IOException("docker compose config failed: "
                    + (details.isEmpty() ? "exit code " + result.exitCode() : details));
        }

        return parseServices(result.stdout());
    }

    List<String> parseServices(String json) throws IOException {
        JsonNode services = objectMapper.readTree(json).path("services");
        if (!services.isObject()) {
            throw new IOException("Compose config does not contain a services object");
        }

        var names = new ArrayList<String>();
        services.fieldNames().forEachRemaining(names::add);
        if (names.isEmpty()) {
            throw new IOException("Compose config does not declare any services");
        }

        return names.stream().sorted().toList();
    }
}
