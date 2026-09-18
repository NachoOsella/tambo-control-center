package app.tambo.ui.component;

import app.tambo.application.logs.LogsController;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;

import java.util.Locale;

import static dev.tamboui.style.Color.CYAN;
import static dev.tamboui.style.Color.DARK_GRAY;
import static dev.tamboui.style.Color.LIGHT_YELLOW;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

public final class LogsPanel {
    private LogsPanel() {
    }

    public static Panel render(
            LogsController.LogView view,
            int visibleStart,
            int visibleEnd,
            boolean following,
            boolean searchActive,
            String searchQuery
    ) {
        Element content;
        if (view.lines().isEmpty()) {
            content = text(emptyMessage(view)).dim();
        } else {
            var lines = view.lines().subList(visibleStart, visibleEnd).stream()
                    .map(line -> {
                        var element = text(line);
                        if (!searchQuery.isBlank()
                                && line.toLowerCase(Locale.ROOT)
                                .contains(searchQuery.toLowerCase(Locale.ROOT))) {
                            element = element.fg(LIGHT_YELLOW);
                        }
                        return (Element) element;
                    })
                    .toArray(Element[]::new);
            content = column(lines);
        }

        return panel("󰆍 Logs · " + view.scope().label(), content)
                .borderColor(DARK_GRAY)
                .bottomTitle(statusLabel(view.status())
                        + " · follow: " + (following ? "on" : "off")
                        + (searchActive ? " · search: " + searchQuery : ""))
                .id("logs")
                .focusable()
                .focusedBorderColor(CYAN)
                .fill();
    }

    private static String emptyMessage(LogsController.LogView view) {
        return switch (view.status()) {
            case CONNECTING -> "Connecting to log stream...";
            case FOLLOWING -> "Waiting for output...";
            case DISCONNECTED -> "Log stream disconnected";
            case FAILED -> view.message().isBlank() ? "Unable to follow logs" : view.message();
        };
    }

    private static String statusLabel(LogsController.LogStatus status) {
        return switch (status) {
            case CONNECTING -> "connecting";
            case FOLLOWING -> "follow: on";
            case DISCONNECTED -> "disconnected";
            case FAILED -> "failed";
        };
    }
}
