package app.tambo.infrastructure.compose;

import app.tambo.infrastructure.process.ProcessRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComposeCliConfigReaderTest {
    private final ComposeCliConfigReader reader = new ComposeCliConfigReader(
            new ProcessRunner(),
            new ObjectMapper()
    );

    @Test
    void readsAndSortsServiceNamesFromComposeConfig() throws Exception {
        var json = """
                {
                  "name": "demo",
                  "services": {
                    "gateway": {"image": "nginx"},
                    "api": {"image": "api:dev"}
                  },
                  "networks": {}
                }
                """;

        var services = reader.parseServices(json);

        assertEquals(List.of("api", "gateway"), services);
    }

    @Test
    void rejectsConfigWithoutServices() {
        assertThrows(IOException.class, () -> reader.parseServices("{}"));
    }
}
