package app.tambo.infrastructure.compose;

import app.tambo.domain.service.ComposeService;
import app.tambo.infrastructure.process.ProcessRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComposeCliConfigReaderTest {
    private final ComposeCliConfigReader reader = new ComposeCliConfigReader(
            new ProcessRunner(),
            new ObjectMapper()
    );

    @Test
    void readsAndSortsServicesFromComposeConfig() throws Exception {
        var json = """
                {
                  "name": "demo",
                  "services": {
                    "gateway": {"image": "nginx"},
                    "api": {"image": "api:dev"},
                    "worker": {}
                  },
                  "networks": {}
                }
                """;

        var services = reader.parseServices(json);

        assertEquals(List.of(
                new ComposeService("api", Optional.of("api:dev")),
                new ComposeService("gateway", Optional.of("nginx")),
                new ComposeService("worker", Optional.empty())
        ), services);
    }

    @Test
    void readsServiceMetadataForDetails() throws Exception {
        var json = """
                {
                  "services": {
                    "database": {
                      "image": "postgres:17-alpine",
                      "restart": "unless-stopped",
                      "depends_on": {
                        "redis": {"condition": "service_started"}
                      },
                      "profiles": ["database", "local"],
                      "environment": {
                        "POSTGRES_DB": "demo",
                        "POSTGRES_USER": "demo"
                      },
                      "networks": {
                        "default": null,
                        "backend": null
                      },
                      "volumes": [
                        {"type": "volume", "source": "database_data", "target": "/var/lib/postgresql/data"}
                      ]
                    }
                  }
                }
                """;

        var service = reader.parseServices(json).getFirst();

        assertEquals(Optional.of("postgres:17-alpine"), service.image());
        assertEquals(Optional.of("unless-stopped"), service.restartPolicy());
        assertEquals(List.of("default", "backend"), service.networks());
        assertEquals(List.of("POSTGRES_DB", "POSTGRES_USER"), service.environmentVariables());
        assertEquals(List.of("database_data"), service.volumes());
        assertEquals(List.of("redis"), service.dependencies());
        assertEquals(List.of("database", "local"), service.profiles());
    }

    @Test
    void rejectsConfigWithoutServices() {
        assertThrows(IOException.class, () -> reader.parseServices("{}"));
    }
}
