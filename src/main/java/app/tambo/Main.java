package app.tambo;

import app.tambo.infrastructure.compose.ComposeCliConfigReader;
import app.tambo.infrastructure.process.ProcessRunner;
import app.tambo.project.ProjectLocator;
import app.tambo.ui.TamboApp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
        var configReader = new ComposeCliConfigReader(new ProcessRunner(), new ObjectMapper());

        List<String> services;
        try {
            services = configReader.readServices(projectContext);
        } catch (IOException | TimeoutException exception) {
            System.err.println("Unable to load Compose configuration: " + exception.getMessage());
            System.exit(1);
            return;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Unable to load Compose configuration: interrupted");
            System.exit(1);
            return;
        }

        new TamboApp(projectContext, services).run();
    }
}
