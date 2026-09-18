package app.tambo.ui;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class LayoutPreferences {
    private final Path file;

    LayoutPreferences() {
        var configHome = System.getenv("XDG_CONFIG_HOME");
        var base = configHome == null || configHome.isBlank()
                ? Path.of(System.getProperty("user.home"), ".config")
                : Path.of(configHome);
        this.file = base.resolve("tambo").resolve("layout.properties");
    }

    LayoutPreferences(Path file) {
        this.file = file;
    }

    LayoutState load() {
        if (!Files.isRegularFile(file)) {
            return LayoutState.defaults();
        }

        var properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
            var servicesWidth = Integer.parseInt(properties.getProperty("services-width"));
            var overviewHeight = Integer.parseInt(properties.getProperty("overview-height"));
            return new LayoutState(servicesWidth, overviewHeight);
        } catch (IOException | RuntimeException exception) {
            return LayoutState.defaults();
        }
    }

    void save(LayoutState layout) {
        var properties = new Properties();
        properties.setProperty("services-width", Integer.toString(layout.servicesWidthPercent()));
        properties.setProperty("overview-height", Integer.toString(layout.overviewHeightPercent()));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "Tambo panel layout");
            }
        } catch (IOException ignored) {
            // Layout persistence is optional and must not prevent shutdown.
        }
    }
}
