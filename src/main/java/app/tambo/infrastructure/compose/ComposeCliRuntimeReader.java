package app.tambo.infrastructure.compose;

import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.ContainerInstance;
import app.tambo.domain.service.HealthState;
import app.tambo.domain.service.PublishedPort;
import app.tambo.domain.service.RuntimeState;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.TimeoutException;

public final class ComposeCliRuntimeReader {
    private static final Duration RUNTIME_TIMEOUT = Duration.ofSeconds(10);

    private final ProcessRunner processRunner;
    private final ObjectMapper objectMapper;

    public ComposeCliRuntimeReader(ProcessRunner processRunner, ObjectMapper objectMapper) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public Map<String, ServiceRuntime> readRuntime(
            ProjectContext project,
            List<ComposeService> services
    ) throws IOException, InterruptedException, TimeoutException {
        var command = new Command(
                List.of("docker", "compose", "ps", "--all", "--format", "json"),
                project.root()
        );
        var result = processRunner.run(command, RUNTIME_TIMEOUT);

        if (!result.succeeded()) {
            var details = result.stderr().strip();
            throw new IOException("docker compose ps failed: "
                    + (details.isEmpty() ? "exit code " + result.exitCode() : details));
        }

        return parseRuntime(result.stdout(), services);
    }

    Map<String, ServiceRuntime> parseRuntime(
            String jsonLines,
            List<ComposeService> services
    ) throws IOException {
        var declaredServices = new LinkedHashMap<String, ComposeService>();
        for (var service : services) {
            declaredServices.put(service.name(), service);
        }

        var instancesByService = new HashMap<String, List<ContainerInstance>>();
        for (var line : jsonLines.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }

            var entry = objectMapper.readTree(line);
            var serviceName = requiredText(entry, "Service");
            if (!declaredServices.containsKey(serviceName)) {
                continue;
            }

            instancesByService
                    .computeIfAbsent(serviceName, ignored -> new ArrayList<>())
                    .add(parseInstance(entry));
        }

        var runtimeByService = new LinkedHashMap<String, ServiceRuntime>();
        for (var service : services) {
            var instances = instancesByService.getOrDefault(service.name(), List.of());
            runtimeByService.put(service.name(), ServiceRuntime.fromInstances(instances));
        }
        return Map.copyOf(runtimeByService);
    }

    private ContainerInstance parseInstance(JsonNode entry) throws IOException {
        var publishers = parsePublishers(entry.path("Publishers"));
        var runtimeState = RuntimeState.fromCompose(entry.path("State").asText(null));
        var exitCode = parseExitCode(entry.get("ExitCode"), runtimeState);

        return new ContainerInstance(
                requiredText(entry, "ID"),
                requiredText(entry, "Name"),
                runtimeState,
                HealthState.fromCompose(entry.path("Health").asText(null)),
                exitCode,
                publishers
        );
    }

    private List<PublishedPort> parsePublishers(JsonNode publishers) throws IOException {
        if (publishers.isMissingNode() || publishers.isNull()) {
            return List.of();
        }
        if (!publishers.isArray()) {
            throw new IOException("Compose runtime Publishers must be an array");
        }

        var ports = new ArrayList<PublishedPort>();
        for (var publisher : publishers) {
            try {
                ports.add(new PublishedPort(
                        publisher.path("URL").asText(""),
                        positiveInt(publisher, "TargetPort"),
                        positiveInt(publisher, "PublishedPort"),
                        publisher.path("Protocol").asText("unknown")
                ));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Compose runtime contains an invalid published port", exception);
            }
        }
        return List.copyOf(ports);
    }

    private OptionalInt parseExitCode(JsonNode exitCode, RuntimeState runtimeState) throws IOException {
        if (exitCode == null || exitCode.isNull()
                || (runtimeState != RuntimeState.EXITED && runtimeState != RuntimeState.DEAD)) {
            return OptionalInt.empty();
        }
        if (!exitCode.canConvertToInt()) {
            throw new IOException("Compose runtime ExitCode must be an integer");
        }
        return OptionalInt.of(exitCode.intValue());
    }

    private int positiveInt(JsonNode node, String field) throws IOException {
        var value = node.get(field);
        if (value == null || !value.canConvertToInt() || value.intValue() <= 0) {
            throw new IOException("Compose runtime " + field + " must be a positive integer");
        }
        return value.intValue();
    }

    private String requiredText(JsonNode node, String field) throws IOException {
        var value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IOException("Compose runtime is missing " + field);
        }
        return value;
    }
}
