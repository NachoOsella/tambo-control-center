package app.tambo;

import app.tambo.project.ProjectLocator;
import app.tambo.ui.TamboApp;

import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        var startDirectory = Path.of(".").toAbsolutePath().normalize();
        var project = new ProjectLocator().locate(startDirectory);

        if (project.isEmpty()) {
            System.err.println("No Compose file found from " + startDirectory + " or any parent directory.");
            System.exit(1);
        }

        new TamboApp(project.orElseThrow()).run();
    }
}
