package app.tambo.ui;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.text;

public final class TamboApp extends ToolkitApp {
    @Override
    protected Element render() {
        return text("Tambo\n\nPress q to quit");
    }
}
