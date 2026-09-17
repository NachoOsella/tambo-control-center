package app.tambo.ui;

import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.PublishedPort;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.project.ProjectContext;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static dev.tamboui.style.Color.CYAN;
import static dev.tamboui.style.Color.DARK_GRAY;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

public final class TamboApp extends ToolkitApp {
    private final ProjectContext project;
    private final Map<String, ServiceRuntime> runtimeByService;
    private UiState state;

    public TamboApp(
            ProjectContext project,
            List<ComposeService> services,
            Map<String, ServiceRuntime> runtimeByService
    ) {
        this.project = Objects.requireNonNull(project, "project");
        this.runtimeByService = Map.copyOf(runtimeByService);
        this.state = new UiState(services, 0);
    }

    @Override
    protected void onStart() {
        setWindowTitle("Tambo | " + projectName());
    }

    @Override
    protected Element render() {
        var overview = row(
                servicesPanel().percent(30),
                detailsPanel().fill()
        ).spacing(1).percent(55);

        var logs = standardPanel("Logs [selected: " + state.selectedService().name() + "]")
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
        var runtime = selectedRuntime();
        var details = column(
                row(
                        text("Service").dim().length(12),
                        text(state.selectedService().name()).fg(CYAN).bold().fill()
                ),
                row(
                        text("Image").dim().length(12),
                        text(state.selectedService().image().orElse("not specified")).fill()
                ),
                row(
                        text("Runtime").dim().length(12),
                        text(runtime.runtimeState().displayName()).fill()
                ),
                row(
                        text("Health").dim().length(12),
                        text(runtime.healthState().displayName()).fill()
                ),
                row(
                        text("Containers").dim().length(12),
                        text(runtime.containerCount()).fill()
                ),
                row(
                        text("Ports").dim().length(12),
                        text(formatPorts(runtime.publishedPorts())).fill()
                ),
                row(
                        text("Exit code").dim().length(12),
                        text(formatExitCode(runtime)).fill()
                )
        ).spacing(1);
        return standardPanel("Details", details)
                .id("details")
                .focusable()
                .focusedBorderColor(CYAN);
    }

    private Panel standardPanel(String title, Element... children) {
        return panel(title, children).borderColor(DARK_GRAY);
    }

    private String projectName() {
        var name = project.root().getFileName();
        return name == null ? project.root().toString() : name.toString();
    }

    private Element serviceList() {
        var rows = new Element[state.services().size()];
        for (int index = 0; index < state.services().size(); index++) {
            boolean selected = index == state.selectedIndex();
            var service = state.services().get(index);
            var name = text((selected ? "▸ " : "  ") + service.name()).fill();
            var status = text(runtimeFor(service).runtimeState().displayName()).dim();
            rows[index] = row(selected ? name.fg(CYAN).bold() : name, status);
        }
        return column(rows);
    }

    private ServiceRuntime selectedRuntime() {
        return runtimeFor(state.selectedService());
    }

    private ServiceRuntime runtimeFor(ComposeService service) {
        return runtimeByService.getOrDefault(service.name(), ServiceRuntime.notCreated());
    }

    private String formatPorts(List<PublishedPort> ports) {
        if (ports.isEmpty()) {
            return "none";
        }
        return ports.stream()
                .map(this::formatPort)
                .collect(Collectors.joining(", "));
    }

    private String formatPort(PublishedPort port) {
        var host = port.host().isBlank() ? "*" : port.host();
        return host + ":" + port.publishedPort() + " -> " + port.targetPort();
    }

    private String formatExitCode(ServiceRuntime runtime) {
        return runtime.exitCode().isPresent()
                ? Integer.toString(runtime.exitCode().getAsInt())
                : "-";
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
