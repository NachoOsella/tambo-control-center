package app.tambo.ui.component;

import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.PublishedPort;
import app.tambo.domain.service.RuntimeState;
import app.tambo.domain.service.ServiceRuntime;

import dev.tamboui.toolkit.elements.Panel;

import java.util.List;
import java.util.stream.Collectors;

import static dev.tamboui.style.Color.CYAN;
import static dev.tamboui.style.Color.DARK_GRAY;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

public final class DetailsPanel {
    private static final int LABEL_WIDTH = 16;

    private DetailsPanel() {
    }

    public static Panel render(ComposeService service, ServiceRuntime runtime, String operation) {
        var details = column(
                row(
                        text("Service").dim().length(LABEL_WIDTH),
                        text(service.name()).fg(CYAN).bold().fill()
                ),
                row(
                        text("Image").dim().length(LABEL_WIDTH),
                        text(service.image().orElse("not specified")).fill()
                ),
                row(
                        text("Status").dim().length(LABEL_WIDTH),
                        text(runtimeLabel(runtime.runtimeState())).fill()
                ),
                row(
                        text("Operation").dim().length(LABEL_WIDTH),
                        text(operation).fill()
                ),
                row(
                        text("Health").dim().length(LABEL_WIDTH),
                        text(runtime.healthState().displayName()).fill()
                ),
                row(
                        text("Container").dim().length(LABEL_WIDTH),
                        text(formatContainerNames(runtime)).fill()
                ),
                row(
                        text("Containers").dim().length(LABEL_WIDTH),
                        text(runtime.containerCount()).fill()
                ),
                row(
                        text("Ports").dim().length(LABEL_WIDTH),
                        text(formatPorts(runtime.publishedPorts())).fill()
                ),
                row(
                        text("Network").dim().length(LABEL_WIDTH),
                        text(formatList(service.networks())).fill()
                ),
                row(
                        text("Depends on").dim().length(LABEL_WIDTH),
                        text(formatList(service.dependencies())).fill()
                ),
                row(
                        text("Profiles").dim().length(LABEL_WIDTH),
                        text(formatList(service.profiles())).fill()
                ),
                row(
                        text("Restart policy").dim().length(LABEL_WIDTH),
                        text(service.restartPolicy().orElse("not specified")).fill()
                ),
                row(
                        text("Environment").dim().length(LABEL_WIDTH),
                        text(formatEnvironment(service.environmentVariables())).fill()
                ),
                row(
                        text("Volumes").dim().length(LABEL_WIDTH),
                        text(formatVolumes(service.volumes())).fill()
                ),
                row(
                        text("Exit code").dim().length(LABEL_WIDTH),
                        text(formatExitCode(runtime)).fill()
                )
        ).spacing(0);
        return panel("󰒓 Details · " + service.name(), details)
                .borderColor(DARK_GRAY)
                .id("details")
                .focusable()
                .focusedBorderColor(CYAN);
    }

    private static String runtimeLabel(RuntimeState state) {
        return switch (state) {
            case EXITED, DEAD -> "stopped";
            default -> state.displayName();
        };
    }

    private static String formatContainerNames(ServiceRuntime runtime) {
        if (runtime.instances().isEmpty()) {
            return "none";
        }
        return runtime.instances().stream()
                .map(instance -> instance.name())
                .collect(Collectors.joining(", "));
    }

    private static String formatList(List<String> values) {
        return values.isEmpty() ? "none" : String.join(", ", values);
    }

    private static String formatEnvironment(List<String> variables) {
        if (variables.isEmpty()) {
            return "none";
        }
        return variables.size() + " variables (" + String.join(", ", variables) + ")";
    }

    private static String formatVolumes(List<String> volumes) {
        if (volumes.isEmpty()) {
            return "none";
        }
        var noun = volumes.size() == 1 ? "volume" : "volumes";
        return volumes.size() + " " + noun + " (" + String.join(", ", volumes) + ")";
    }

    private static String formatPorts(List<PublishedPort> ports) {
        if (ports.isEmpty()) {
            return "none";
        }
        return ports.stream()
                .map(DetailsPanel::formatPort)
                .collect(Collectors.joining(", "));
    }

    private static String formatPort(PublishedPort port) {
        var host = port.host().isBlank() ? "*" : port.host();
        return host + ":" + port.publishedPort() + " -> " + port.targetPort();
    }

    private static String formatExitCode(ServiceRuntime runtime) {
        return runtime.exitCode().isPresent()
                ? Integer.toString(runtime.exitCode().getAsInt())
                : "-";
    }
}
