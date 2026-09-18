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
import java.util.Iterator;
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
        var root = objectMapper.readTree(json);
        if (root == null || !root.isObject()) {
            throw new IOException("Compose config must be a JSON object");
        }
        JsonNode services = root.path("services");
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

            var image = optionalText(service, "image");
            composeServices.add(new ComposeService(
                    name,
                    image,
                    objectKeys(service, "networks"),
                    environmentNames(service),
                    volumeNames(service),
                    optionalText(service, "restart"),
                    objectKeys(service, "depends_on"),
                    profileNames(service)
            ));
        }
        if (composeServices.isEmpty()) {
            throw new IOException("Compose config does not declare any services");
        }

        return composeServices.stream()
                .sorted(Comparator.comparing(ComposeService::name))
                .toList();
    }

    private Optional<String> optionalText(JsonNode service, String field) {
        return Optional.ofNullable(service.get(field))
                .filter(JsonNode::isTextual)
                .map(JsonNode::textValue)
                .filter(value -> !value.isBlank());
    }

    private List<String> objectKeys(JsonNode service, String field) throws IOException {
        var value = service.get(field);
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (!value.isObject()) {
            throw new IOException("Compose service " + field + " must be an object");
        }

        return collectFieldNames(value);
    }

    private List<String> environmentNames(JsonNode service) throws IOException {
        var value = service.get("environment");
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (value.isObject()) {
            return collectFieldNames(value);
        }
        if (value.isArray()) {
            var names = new ArrayList<String>();
            for (var variable : value) {
                if (!variable.isTextual() || variable.textValue().isBlank()) {
                    throw new IOException("Compose service environment must contain strings");
                }
                var separator = variable.textValue().indexOf('=');
                names.add(separator < 0
                        ? variable.textValue()
                        : variable.textValue().substring(0, separator));
            }
            return List.copyOf(names);
        }
        throw new IOException("Compose service environment must be an object or array");
    }

    private List<String> profileNames(JsonNode service) throws IOException {
        var value = service.get("profiles");
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (!value.isArray()) {
            throw new IOException("Compose service profiles must be an array");
        }

        var names = new ArrayList<String>();
        for (var profile : value) {
            if (!profile.isTextual() || profile.textValue().isBlank()) {
                throw new IOException("Compose service profiles must contain strings");
            }
            names.add(profile.textValue());
        }
        return List.copyOf(names);
    }

    private List<String> volumeNames(JsonNode service) throws IOException {
        var value = service.get("volumes");
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (!value.isArray()) {
            throw new IOException("Compose service volumes must be an array");
        }

        var names = new ArrayList<String>();
        for (var volume : value) {
            if (volume.isTextual()) {
                names.add(volume.textValue());
            } else if (volume.isObject()) {
                var source = volume.path("source").asText(null);
                var target = volume.path("target").asText(null);
                if (source == null || source.isBlank()) {
                    source = target;
                }
                if (source == null || source.isBlank()) {
                    throw new IOException("Compose service volume is missing source and target");
                }
                names.add(source);
            } else {
                throw new IOException("Compose service volumes must contain strings or objects");
            }
        }
        return List.copyOf(names);
    }

    private List<String> collectFieldNames(JsonNode object) {
        var names = new ArrayList<String>();
        Iterator<String> fields = object.fieldNames();
        fields.forEachRemaining(names::add);
        return List.copyOf(names);
    }
}
