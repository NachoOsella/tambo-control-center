package app.tambo.infrastructure.compose;

import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.HealthState;
import app.tambo.domain.service.PublishedPort;
import app.tambo.domain.service.RuntimeState;
import app.tambo.infrastructure.process.ProcessRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposeCliRuntimeReaderTest {
    private final ComposeCliRuntimeReader reader = new ComposeCliRuntimeReader(
            new ProcessRunner(),
            new ObjectMapper()
    );

    @Test
    void mapsRuntimeByDeclaredServiceAndKeepsMultipleInstances() throws Exception {
        var services = List.of(
                service("gateway"),
                service("worker"),
                service("api"),
                service("missing")
        );
        var jsonLines = """
                {"ID":"g-1","Name":"demo-gateway-1","Service":"gateway","State":"running","Health":"healthy","ExitCode":0,"Publishers":[{"URL":"0.0.0.0","TargetPort":80,"PublishedPort":8080,"Protocol":"tcp"}]}
                {"ID":"g-2","Name":"demo-gateway-2","Service":"gateway","State":"running","Health":"healthy","ExitCode":0,"Publishers":[]}
                {"ID":"w-1","Name":"demo-worker-1","Service":"worker","State":"exited","Health":"","ExitCode":7,"Publishers":[]}
                {"ID":"a-1","Name":"demo-api-1","Service":"api","State":"running","Health":"unhealthy","ExitCode":0,"Publishers":[]}
                {"ID":"old-1","Name":"demo-old-1","Service":"old","State":"running","Health":"healthy","ExitCode":0,"Publishers":[]}
                """;

        var runtime = reader.parseRuntime(jsonLines, services);

        var gateway = runtime.get("gateway");
        assertEquals(RuntimeState.RUNNING, gateway.runtimeState());
        assertEquals(HealthState.HEALTHY, gateway.healthState());
        assertEquals(2, gateway.containerCount());
        assertEquals(
                List.of(new PublishedPort("0.0.0.0", 80, 8080, "tcp")),
                gateway.publishedPorts()
        );
        assertTrue(gateway.exitCode().isEmpty());

        var worker = runtime.get("worker");
        assertEquals(RuntimeState.EXITED, worker.runtimeState());
        assertEquals(HealthState.NOT_CONFIGURED, worker.healthState());
        assertEquals(7, worker.exitCode().orElseThrow());

        var api = runtime.get("api");
        assertEquals(RuntimeState.RUNNING, api.runtimeState());
        assertEquals(HealthState.UNHEALTHY, api.healthState());

        var missing = runtime.get("missing");
        assertEquals(RuntimeState.NOT_CREATED, missing.runtimeState());
        assertEquals(HealthState.NOT_CONFIGURED, missing.healthState());
        assertEquals(0, missing.containerCount());
    }

    private static ComposeService service(String name) {
        return new ComposeService(name, Optional.empty());
    }
}
