package app.tambo.ui.component;

import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.RuntimeState;
import app.tambo.domain.service.ServiceRuntime;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.elements.TextElement;

import java.util.List;
import java.util.Map;

import static dev.tamboui.style.Color.CYAN;
import static dev.tamboui.style.Color.DARK_GRAY;
import static dev.tamboui.style.Color.LIGHT_GREEN;
import static dev.tamboui.style.Color.LIGHT_RED;
import static dev.tamboui.style.Color.LIGHT_YELLOW;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

public final class ServicesPanel {
    private static final Color SELECTED_BACKGROUND = Color.rgb(61, 55, 31);

    private ServicesPanel() {
    }

    public static Panel render(
            List<ComposeService> services,
            int selectedIndex,
            int totalServiceCount,
            boolean filtered,
            Map<String, ServiceRuntime> runtimeByService
    ) {
        var title = filtered
                ? "Services · " + services.size() + "/" + totalServiceCount
                : "Services · " + totalServiceCount;
        var rows = new Element[services.size()];
        for (int index = 0; index < services.size(); index++) {
            boolean selected = index == selectedIndex;
            var service = services.get(index);
            var runtime = runtimeByService.getOrDefault(service.name(), ServiceRuntime.notCreated());
            var name = text((selected ? "▸ " : "  ") + service.name()).fill();
            var status = serviceStatus(runtime);
            if (selected) {
                name = name.fg(CYAN).bold().bg(SELECTED_BACKGROUND);
                status = status.bg(SELECTED_BACKGROUND);
            }
            rows[index] = row(name, status);
        }

        return panel("󰏗 " + title, column(rows))
                .borderColor(DARK_GRAY)
                .id("services")
                .focusable()
                .focusedBorderColor(CYAN);
    }

    private static TextElement serviceStatus(ServiceRuntime runtime) {
        var state = runtime.runtimeState();
        var icon = switch (state) {
            case RUNNING -> "󰐊";
            case EXITED, DEAD -> "󰓛";
            case NOT_CREATED -> "󰝦";
            default -> "󰅙";
        };
        return text(icon + " " + runtimeLabel(state)).fg(runtimeColor(state));
    }

    private static String runtimeLabel(RuntimeState state) {
        return switch (state) {
            case EXITED, DEAD -> "stopped";
            default -> state.displayName();
        };
    }

    private static Color runtimeColor(RuntimeState state) {
        return switch (state) {
            case RUNNING -> LIGHT_GREEN;
            case EXITED, DEAD -> LIGHT_RED;
            case NOT_CREATED -> DARK_GRAY;
            default -> LIGHT_YELLOW;
        };
    }
}
