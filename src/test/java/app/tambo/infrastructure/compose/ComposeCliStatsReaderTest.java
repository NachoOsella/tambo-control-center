package app.tambo.infrastructure.compose;

import app.tambo.domain.service.ContainerInstance;
import app.tambo.domain.service.HealthState;
import app.tambo.domain.service.RuntimeState;
import app.tambo.infrastructure.process.ProcessRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComposeCliStatsReaderTest {
    private final ComposeCliStatsReader reader = new ComposeCliStatsReader(
            new ProcessRunner(),
            new ObjectMapper()
    );

    @Test
    void mapsStatsByRuntimeContainerName() throws Exception {
        var containers = List.of(new ContainerInstance(
                "api-id",
                "demo-api-1",
                RuntimeState.RUNNING,
                HealthState.NOT_CONFIGURED,
                OptionalInt.empty(),
                List.of()
        ));
        var jsonLines = """
                {"Name":"demo-api-1","CPUPerc":"0.12%","MemUsage":"24.5MiB / 1GiB","MemPerc":"2.4%","NetIO":"539kB / 606kB","PIDs":"2"}
                {"Name":"demo-old-1","CPUPerc":"10.00%","MemUsage":"1MiB / 1GiB","MemPerc":"0.1%","NetIO":"1kB / 2kB","PIDs":"1"}
                """;

        var stats = reader.parseStats(jsonLines, containers);

        var usage = stats.get("demo-api-1");
        assertEquals(0.12, usage.cpuPercent());
        assertEquals("24.5MiB", usage.memoryUsage());
        assertEquals("1GiB", usage.memoryLimit());
        assertEquals(2.4, usage.memoryPercent());
        assertEquals("539kB", usage.networkInput());
        assertEquals("606kB", usage.networkOutput());
        assertEquals(2, usage.processCount());
    }

    @Test
    void returnsNoStatsWhenThereAreNoRuntimeContainers() throws Exception {
        assertEquals(java.util.Map.of(), reader.parseStats("", List.of()));
    }
}
