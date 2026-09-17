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
    void rejectsConfigWithoutServices() {
        assertThrows(IOException.class, () -> reader.parseServices("{}"));
    }
}
