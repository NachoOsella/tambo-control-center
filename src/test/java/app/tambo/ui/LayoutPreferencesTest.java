package app.tambo.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutPreferencesTest {
    @TempDir
    Path directory;

    @Test
    void savesAndLoadsLayoutRatios() {
        var preferences = new LayoutPreferences(directory.resolve("layout.properties"));
        var expected = new LayoutState(40, 55);

        preferences.save(expected);

        assertEquals(expected, preferences.load());
    }

    @Test
    void invalidPreferencesUseDefaults() throws Exception {
        var file = directory.resolve("layout.properties");
        java.nio.file.Files.writeString(file, "services-width=invalid\n");
        var preferences = new LayoutPreferences(file);

        assertEquals(LayoutState.defaults(), preferences.load());
    }
}
