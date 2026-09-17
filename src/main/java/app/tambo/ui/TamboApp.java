package app.tambo.ui;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.tui.event.KeyEvent;

import java.util.List;

import static dev.tamboui.style.Color.CYAN;
import static dev.tamboui.style.Color.DARK_GRAY;
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
                servicesPanel().percent(30),
                detailsPanel().fill()
        ).spacing(1).percent(55);

        var logs = standardPanel("Logs [selected: " + state.selectedService() + "]")
                .id("logs")
                .focusable()
                .focusedBorderColor(CYAN)
                .fill();

        return column(
                overview,
                logs,
                statusBar()
        ).spacing(1);
    }

    private Panel servicesPanel() {
        return standardPanel("Services", serviceList())
                .id("services")
                .focusable()
                .focusedBorderColor(CYAN)
                .onKeyEvent(this::handleServiceKey);
    }

    private Panel detailsPanel() {
        var service = row(
                text("Service").dim().length(12),
                text(state.selectedService()).fg(CYAN).bold().fill()
        );
        return standardPanel("Details", service)
                .id("details")
                .focusable()
                .focusedBorderColor(CYAN);
    }

    private Panel standardPanel(String title, Element... children) {
        return panel(title, children).borderColor(DARK_GRAY);
    }

    private Element serviceList() {
        var rows = new Element[state.services().size()];
        for (int index = 0; index < state.services().size(); index++) {
            boolean selected = index == state.selectedIndex();
            var service = text((selected ? "▸ " : "  ") + state.services().get(index));
            rows[index] = selected ? service.fg(CYAN).bold() : service;
        }
        return column(rows);
    }

    private Element statusBar() {
        return row(
                text("Tab").fg(CYAN).bold(),
                text("focus").dim(),
                text("↑↓ j/k").fg(CYAN).bold(),
                text("select").dim(),
                text("q").fg(CYAN).bold(),
                text("quit").dim()
        ).spacing(1).length(1);
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
