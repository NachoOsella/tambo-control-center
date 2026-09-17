package app.tambo.ui;

import app.tambo.application.service.LifecycleResult;
import app.tambo.application.service.RefreshRuntimeSnapshot;
import app.tambo.application.service.UpService;
import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.PublishedPort;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.project.ProjectContext;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static dev.tamboui.style.Color.CYAN;
import static dev.tamboui.style.Color.DARK_GRAY;
import static dev.tamboui.style.Color.LIGHT_GREEN;
import static dev.tamboui.style.Color.LIGHT_RED;
import static dev.tamboui.style.Color.LIGHT_YELLOW;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

public final class TamboApp extends ToolkitApp {
    private static final Duration RUNTIME_REFRESH_INTERVAL = Duration.ofSeconds(5);

    private final ProjectContext project;
    private final RefreshRuntimeSnapshot refreshRuntime;
    private final UpService upService;
    private final Set<String> startingServices = new HashSet<>();
    private Map<String, ServiceRuntime> runtimeByService;
    private UiState state;
    private RefreshStatus refreshStatus = RefreshStatus.IDLE;
    private String refreshMessage = "";
    private OperationStatus operationStatus = OperationStatus.IDLE;
    private String operationMessage = "";
    private ToolkitRunner.ScheduledAction runtimePolling;

    public TamboApp(
            ProjectContext project,
            List<ComposeService> services,
            Map<String, ServiceRuntime> runtimeByService,
            RefreshRuntimeSnapshot refreshRuntime,
            UpService upService
    ) {
        this.project = Objects.requireNonNull(project, "project");
        this.runtimeByService = Map.copyOf(runtimeByService);
        this.refreshRuntime = Objects.requireNonNull(refreshRuntime, "refreshRuntime");
        this.upService = Objects.requireNonNull(upService, "upService");
        this.state = new UiState(services, 0);
    }

    @Override
    protected void onStart() {
        setWindowTitle("Tambo | " + projectName());
        runner().eventRouter().addGlobalHandler(this::handleGlobalEvent);
        runtimePolling = runner().scheduleRepeating(
                () -> runner().runOnRenderThread(() -> requestRuntimeRefresh()),
                RUNTIME_REFRESH_INTERVAL
        );
    }

    @Override
    protected void onStop() {
        runtimePolling.cancel();
        upService.close();
        refreshRuntime.close();
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
                        text("Operation").dim().length(12),
                        text(selectedOperation()).fill()
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

    private String selectedOperation() {
        return startingServices.contains(state.selectedService().name()) ? "starting" : "idle";
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
                text("u").fg(CYAN).bold(),
                text("up").dim(),
                text("g").fg(CYAN).bold(),
                text("refresh").dim(),
                text("q").fg(CYAN).bold(),
                text("quit").dim(),
                text("").fill(),
                statusIndicator()
        ).spacing(1).length(1);
    }

    private Element statusIndicator() {
        if (operationStatus != OperationStatus.IDLE) {
            return switch (operationStatus) {
                case RUNNING -> text("󰐊 " + operationMessage).fg(LIGHT_YELLOW);
                case SUCCEEDED -> text("󰄬 " + operationMessage).fg(LIGHT_GREEN);
                case FAILED -> text("󰅙 " + operationMessage).fg(LIGHT_RED);
                case IDLE -> throw new IllegalStateException("idle operation has no indicator");
            };
        }

        return switch (refreshStatus) {
            case IDLE -> text("󰡨 auto 5s").dim();
            case REFRESHING -> text("󰑐 refreshing · auto 5s").fg(LIGHT_YELLOW);
            case SUCCEEDED -> text("󰄬 auto 5s").fg(LIGHT_GREEN);
            case FAILED -> text("󰅙 " + refreshMessage).fg(LIGHT_RED);
        };
    }

    private EventResult handleGlobalEvent(Event event) {
        if (!(event instanceof KeyEvent keyEvent)) {
            return EventResult.UNHANDLED;
        }
        if (keyEvent.isChar('u')) {
            return upSelectedService();
        }
        if (keyEvent.isChar('g')) {
            return requestRuntimeRefresh();
        }
        return EventResult.UNHANDLED;
    }

    private EventResult upSelectedService() {
        var serviceName = state.selectedService().name();
        if (!startingServices.add(serviceName)) {
            return EventResult.HANDLED;
        }

        operationStatus = OperationStatus.RUNNING;
        operationMessage = "starting " + serviceName;
        upService.execute(serviceName).whenComplete((result, error) -> {
            if (!runner().isRunning()) {
                return;
            }
            runner().runOnRenderThread(() -> finishUp(serviceName, result, error));
        });
        return EventResult.HANDLED;
    }

    private void finishUp(String serviceName, LifecycleResult result, Throwable error) {
        startingServices.remove(serviceName);

        if (error != null) {
            operationStatus = OperationStatus.FAILED;
            operationMessage = "up " + serviceName + " failed: " + errorMessage(error);
            return;
        }
        if (result instanceof LifecycleResult.Failed failure) {
            operationStatus = OperationStatus.FAILED;
            operationMessage = "up " + serviceName + " failed: " + compact(failure.message());
            return;
        }
        if (result instanceof LifecycleResult.Rejected) {
            operationStatus = OperationStatus.FAILED;
            operationMessage = serviceName + " already has an active operation";
            return;
        }

        operationStatus = OperationStatus.SUCCEEDED;
        operationMessage = "started " + serviceName;
        requestRuntimeRefresh();
    }

    private EventResult requestRuntimeRefresh() {
        if (refreshStatus == RefreshStatus.REFRESHING) {
            return EventResult.HANDLED;
        }

        refreshStatus = RefreshStatus.REFRESHING;
        refreshMessage = "";
        refreshRuntime.execute().whenComplete((runtime, error) -> {
            if (!runner().isRunning()) {
                return;
            }

            // TamboUI renders on one thread, so async results return there before changing screen state.
            runner().runOnRenderThread(() -> finishRefresh(runtime, error));
        });
        return EventResult.HANDLED;
    }

    private void finishRefresh(Map<String, ServiceRuntime> runtime, Throwable error) {
        if (error == null) {
            runtimeByService = Map.copyOf(runtime);
            refreshStatus = RefreshStatus.SUCCEEDED;
            if (operationStatus == OperationStatus.SUCCEEDED) {
                operationStatus = OperationStatus.IDLE;
                operationMessage = "";
            }
            return;
        }

        refreshStatus = RefreshStatus.FAILED;
        refreshMessage = errorMessage(error);
    }

    private String errorMessage(Throwable error) {
        var cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause()
                : error;
        return cause.getMessage() == null || cause.getMessage().isBlank()
                ? cause.getClass().getSimpleName()
                : compact(cause.getMessage());
    }

    private String compact(String message) {
        return message.replaceAll("\\s+", " ").strip();
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

    private enum RefreshStatus {
        IDLE,
        REFRESHING,
        SUCCEEDED,
        FAILED
    }

    private enum OperationStatus {
        IDLE,
        RUNNING,
        SUCCEEDED,
        FAILED
    }
}
