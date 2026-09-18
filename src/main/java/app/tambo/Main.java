package app.tambo;

import app.tambo.application.events.ComposeEventObserver;
import app.tambo.application.logs.LogsController;
import app.tambo.application.service.RefreshResourceStats;
import app.tambo.application.service.RefreshRuntimeSnapshot;
import app.tambo.application.service.RunServiceOperation;
import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.ContainerInstance;
import app.tambo.domain.service.ResourceUsage;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.infrastructure.compose.ComposeCliConfigReader;
import app.tambo.infrastructure.compose.ComposeCliPreflight;
import app.tambo.infrastructure.compose.ComposeCliRuntimeReader;
import app.tambo.infrastructure.compose.ComposeCliServiceLogSource;
import app.tambo.infrastructure.compose.ComposeCliEventSource;
import app.tambo.infrastructure.compose.ComposeCliServiceLifecycle;
import app.tambo.infrastructure.compose.ComposeCliStatsReader;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectLocator;
import app.tambo.ui.TamboApp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        var startDirectory = Path.of(".").toAbsolutePath().normalize();
        var project = new ProjectLocator().locate(startDirectory);

        if (project.isEmpty()) {
            System.err.println("No Compose file found from " + startDirectory + " or any parent directory.");
            System.exit(1);
            return;
        }

        var projectContext = project.orElseThrow();
        var processRunner = new ProcessRunner();
        var objectMapper = new ObjectMapper();
        var preflight = new ComposeCliPreflight(processRunner);
        var configReader = new ComposeCliConfigReader(processRunner, objectMapper);
        var runtimeReader = new ComposeCliRuntimeReader(processRunner, objectMapper);
        var statsReader = new ComposeCliStatsReader(processRunner, objectMapper);

        List<ComposeService> services;
        Map<String, ServiceRuntime> runtime;
        try {
            preflight.verify(projectContext);
            services = configReader.readServices(projectContext);
            runtime = runtimeReader.readRuntime(projectContext, services);
        } catch (IOException | TimeoutException exception) {
            System.err.println("Unable to load Compose project: " + exception.getMessage());
            System.exit(1);
            return;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Unable to load Compose project: interrupted");
            System.exit(1);
            return;
        }

        Map<String, ResourceUsage> stats;
        try {
            stats = statsReader.readStats(projectContext, containers(runtime));
        } catch (IOException | TimeoutException exception) {
            stats = Map.of();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            stats = Map.of();
        }

        var refreshRuntime = new RefreshRuntimeSnapshot(runtimeReader, projectContext, services);
        var refreshStats = new RefreshResourceStats(statsReader, projectContext);
        var serviceOperations = new RunServiceOperation(
                new ComposeCliServiceLifecycle(processRunner),
                projectContext
        );
        var logsController = new LogsController(
                new ComposeCliServiceLogSource(),
                projectContext,
                1_000
        );
        var composeEvents = new ComposeEventObserver(
                new ComposeCliEventSource(objectMapper),
                projectContext
        );
        new TamboApp(
                projectContext,
                services,
                runtime,
                stats,
                refreshRuntime,
                refreshStats,
                serviceOperations,
                logsController,
                composeEvents
        ).run();
    }

    private static List<ContainerInstance> containers(Map<String, ServiceRuntime> runtime) {
        return runtime.values().stream()
                .flatMap(service -> service.instances().stream())
                .toList();
    }
}
