package app.tambo.ui;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;

import java.util.List;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

public final class TamboApp extends ToolkitApp {
    private UiState state = new UiState(
            List.of("gateway", "challenge", "bank", "postgres"),
            0
    );

    @Override
    protected Element render() {
        var overview = row(
                panel("Services", serviceList())
                        .id("services")
                        .focusable()
                        .onKeyEvent(this::handleServiceKey)
                        .percent(40),
                panel("Details", text("Service: " + state.selectedService())).fill()
        ).percent(60);

        return column(
                overview,
                panel("Logs").fill(),
                text(" up/down or j/k select | q quit").length(1)
        );
    }

    private Element serviceList() {
        var rows = new Element[state.services().size()];
        for (int index = 0; index < state.services().size(); index++) {
            var marker = index == state.selectedIndex() ? "> " : "  ";
            rows[index] = text(marker + state.services().get(index));
        }
        return column(rows);
    }

    private EventResult handleServiceKey(KeyEvent event) {
        if (event.isDown() || event.isChar('j')) {
            state = state.selectNext();
            return EventResult.HANDLED;
        }
        if (event.isUp() || event.isChar('k')) {
            state = state.selectPrevious();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}
