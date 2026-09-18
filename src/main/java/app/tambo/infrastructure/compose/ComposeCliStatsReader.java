package app.tambo.infrastructure.compose;

import app.tambo.application.service.ResourceStatsReader;
import app.tambo.domain.service.ContainerInstance;
import app.tambo.domain.service.ResourceUsage;
import app.tambo.infrastructure.process.Command;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class ComposeCliStatsReader implements ResourceStatsReader {
    private static final Duration STATS_TIMEOUT = Duration.ofSeconds(10);

    private final ProcessRunner processRunner;
    private final ObjectMapper objectMapper;

    public ComposeCliStatsReader(ProcessRunner processRunner, ObjectMapper objectMapper) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public Map<String, ResourceUsage> readStats(
            ProjectContext project,
            List<ContainerInstance> containers
    ) throws IOException, InterruptedException, TimeoutException {
        var command = new Command(
                List.of("docker", "compose", "stats", "--no-stream", "--format", "json"),
                project.root()
        );
        var result = processRunner.run(command, STATS_TIMEOUT);
        if (!result.succeeded()) {
            var details = result.stderr().strip();
            throw new IOException("docker compose stats failed: "
                    + (details.isEmpty() ? "exit code " + result.exitCode() : details));
        }

        return parseStats(result.stdout(), containers);
    }

    Map<String, ResourceUsage> parseStats(
            String jsonLines,
            List<ContainerInstance> containers
    ) throws IOException {
        var knownContainers = containers.stream()
                .map(ContainerInstance::name)
                .collect(java.util.stream.Collectors.toSet());
        var usages = new LinkedHashMap<String, ResourceUsage>();

        for (var line : jsonLines.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            var usage = parseUsage(objectMapper.readTree(line));
            if (knownContainers.contains(usage.containerName())) {
                usages.put(usage.containerName(), usage);
            }
        }
        return Map.copyOf(usages);
    }

    private ResourceUsage parseUsage(JsonNode entry) throws IOException {
        var containerName = requiredText(entry, "Name");
        var memory = memoryValues(entry);
        var network = ioValues(entry, "NetIO");
        return new ResourceUsage(
                containerName,
                percentage(entry, "CPUPerc"),
                memory.used(),
                memory.limit(),
                percentage(entry, "MemPerc"),
                network.input(),
                network.output(),
                integer(entry, "PIDs")
        );
    }

    private MemoryValues memoryValues(JsonNode entry) throws IOException {
        var values = splitValues(requiredText(entry, "MemUsage"), "MemUsage");
        return new MemoryValues(values[0], values[1]);
    }

    private IoValues ioValues(JsonNode entry, String field) throws IOException {
        var values = splitValues(requiredText(entry, field), field);
        return new IoValues(values[0], values[1]);
    }

    private String[] splitValues(String value, String field) throws IOException {
        var values = value.split("/", 2);
        if (values.length != 2 || values[0].isBlank() || values[1].isBlank()) {
            throw new IOException("Compose stats " + field + " must contain two values");
        }
        return new String[]{values[0].strip(), values[1].strip()};
    }

    private double percentage(JsonNode entry, String field) throws IOException {
        var value = requiredText(entry, field).replace("%", "").strip();
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Compose stats " + field + " must be a percentage", exception);
        }
    }

    private int integer(JsonNode entry, String field) throws IOException {
        var value = requiredText(entry, field);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Compose stats " + field + " must be an integer", exception);
        }
    }

    private String requiredText(JsonNode node, String field) throws IOException {
        var value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IOException("Compose stats is missing " + field);
        }
        return value;
    }

    private record MemoryValues(String used, String limit) {
    }

    private record IoValues(String input, String output) {
    }
}
