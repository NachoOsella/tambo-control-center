package app.tambo.ui;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

public final class TamboApp extends ToolkitApp {
    @Override
    protected Element render() {
        var overview = row(
                panel("Services").percent(40),
                panel("Details").fill()
        ).percent(60);

        return column(
                overview,
                panel("Logs").fill(),
                text(" q quit").length(1)
        );
    }
}
