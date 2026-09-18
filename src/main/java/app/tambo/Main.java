package app.tambo;

import app.tambo.application.service.RefreshRuntimeSnapshot;
import app.tambo.application.service.RunServiceOperation;
import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.infrastructure.compose.ComposeCliConfigReader;
import app.tambo.infrastructure.compose.ComposeCliRuntimeReader;
import app.tambo.infrastructure.compose.ComposeCliServiceLifecycle;
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
        var configReader = new ComposeCliConfigReader(processRunner, objectMapper);
        var runtimeReader = new ComposeCliRuntimeReader(processRunner, objectMapper);

        List<ComposeService> services;
        Map<String, ServiceRuntime> runtime;
        try {
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

        var refreshRuntime = new RefreshRuntimeSnapshot(runtimeReader, projectContext, services);
        var serviceOperations = new RunServiceOperation(
                new ComposeCliServiceLifecycle(processRunner),
                projectContext
        );
        new TamboApp(projectContext, services, runtime, refreshRuntime, serviceOperations).run();
    }
}
