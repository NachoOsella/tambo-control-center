package app.tambo.infrastructure.compose;

import app.tambo.domain.service.ComposeService;
import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public final class ComposeCliConfigReader {
    private static final Duration CONFIG_TIMEOUT = Duration.ofSeconds(10);

    private final ProcessRunner processRunner;
    private final ObjectMapper objectMapper;

    public ComposeCliConfigReader(ProcessRunner processRunner, ObjectMapper objectMapper) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public List<ComposeService> readServices(ProjectContext project)
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

    List<ComposeService> parseServices(String json) throws IOException {
        JsonNode services = objectMapper.readTree(json).path("services");
        if (!services.isObject()) {
            throw new IOException("Compose config does not contain a services object");
        }

        var composeServices = new ArrayList<ComposeService>();
        var serviceNames = services.fieldNames();
        while (serviceNames.hasNext()) {
            var name = serviceNames.next();
            var service = services.get(name);
            if (!service.isObject()) {
                throw new IOException("Compose service " + name + " is not an object");
            }

            var image = Optional.ofNullable(service.get("image"))
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::textValue)
                    .filter(value -> !value.isBlank());
            composeServices.add(new ComposeService(name, image));
        }
        if (composeServices.isEmpty()) {
            throw new IOException("Compose config does not declare any services");
        }

        return composeServices.stream()
                .sorted(Comparator.comparing(ComposeService::name))
                .toList();
    }
}
