package app.tambo.ui;

import app.tambo.application.events.ComposeEventObserver;
import app.tambo.application.logs.LogMode;
import app.tambo.application.logs.LogScope;
import app.tambo.application.logs.LogsController;
import app.tambo.application.service.LifecycleResult;
import app.tambo.application.service.OperationTarget;
import app.tambo.application.service.RefreshResourceStats;
import app.tambo.application.service.RefreshRuntimeSnapshot;
import app.tambo.application.service.RunServiceOperation;
import app.tambo.application.service.ServiceOperation;
import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.ContainerInstance;
import app.tambo.domain.service.PublishedPort;
import app.tambo.domain.service.ResourceUsage;
import app.tambo.domain.service.RuntimeState;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.project.ComposeFileChangeDetector;
import app.tambo.project.ProjectContext;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.elements.TextElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static dev.tamboui.style.Color.CYAN;
import static dev.tamboui.style.Color.DARK_GRAY;
import static dev.tamboui.style.Color.LIGHT_BLUE;
import static dev.tamboui.style.Color.LIGHT_GREEN;
import static dev.tamboui.style.Color.LIGHT_RED;
import static dev.tamboui.style.Color.LIGHT_YELLOW;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.lineGauge;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

public final class TamboApp extends ToolkitApp {
    private static final Duration RUNTIME_REFRESH_INTERVAL = Duration.ofSeconds(5);
    private static final int VISIBLE_LOG_LINES = 12;
    private static final int DETAIL_LABEL_WIDTH = 16;
    private static final Color SELECTED_BACKGROUND = Color.rgb(61, 55, 31);

    private final ProjectContext project;
    private final ComposeFileChangeDetector composeFileChanges;
    private final List<ComposeService> allServices;
    private final ComposeEventObserver composeEvents;
    private final RefreshRuntimeSnapshot refreshRuntime;
    private final RefreshResourceStats refreshStats;
    private final RunServiceOperation serviceOperations;
    private final LogsController logsController;
    private LogMode logMode = LogMode.SELECTED_SERVICE;
    private final Map<String, ServiceOperation> activeOperations = new HashMap<>();
    private ServiceOperation globalOperation;
    private Map<String, ServiceRuntime> runtimeByService;
    private Map<String, ResourceUsage> resourceUsageByContainer;
    private UiState state;
    private RefreshStatus refreshStatus = RefreshStatus.IDLE;
    private String refreshMessage = "";
    private String eventMessage = "";
    private StatsStatus statsStatus = StatsStatus.IDLE;
    private String statsMessage = "";
    private OperationStatus operationStatus = OperationStatus.IDLE;
    private String operationMessage = "";
    private LogViewport logViewport = LogViewport.atEnd();
    private final LayoutPreferences layoutPreferences = new LayoutPreferences();
    private LayoutState layout = LayoutState.defaults();
    private boolean helpVisible;
    private boolean errorOverlayVisible;
    private String lastError = "";
    private ServiceOperation pendingConfirmation;
    private boolean filterActive;
    private String filterQuery = "";
    private ToolkitRunner.ScheduledAction runtimePolling;
    private ToolkitRunner.ScheduledAction statsPolling;
    private boolean composeFileChanged;

    public TamboApp(
            ProjectContext project,
            ComposeFileChangeDetector composeFileChanges,
            List<ComposeService> services,
            Map<String, ServiceRuntime> runtimeByService,
            Map<String, ResourceUsage> resourceUsageByContainer,
            RefreshRuntimeSnapshot refreshRuntime,
            RefreshResourceStats refreshStats,
            RunServiceOperation serviceOperations,
            LogsController logsController,
            ComposeEventObserver composeEvents
    ) {
        this.project = Objects.requireNonNull(project, "project");
        this.composeFileChanges = Objects.requireNonNull(composeFileChanges, "composeFileChanges");
        this.allServices = List.copyOf(services);
        this.composeEvents = Objects.requireNonNull(composeEvents, "composeEvents");
        this.runtimeByService = Map.copyOf(runtimeByService);
        this.resourceUsageByContainer = Map.copyOf(resourceUsageByContainer);
        this.refreshRuntime = Objects.requireNonNull(refreshRuntime, "refreshRuntime");
        this.refreshStats = Objects.requireNonNull(refreshStats, "refreshStats");
        this.serviceOperations = Objects.requireNonNull(serviceOperations, "serviceOperations");
        this.logsController = Objects.requireNonNull(logsController, "logsController");
        this.state = new UiState(services, 0);
    }

    @Override
    protected void onStart() {
        layout = layoutPreferences.load();
        setWindowTitle("Tambo | " + projectName());
        runner().eventRouter().addGlobalHandler(this::handleGlobalEvent);
        logsController.follow(LogScope.selected(state.selectedService().name()), this::requestLogRender);
        composeEvents.start(
                () -> runner().runOnRenderThread(this::requestRuntimeRefresh),
                message -> runner().runOnRenderThread(() -> eventMessage = compact(message))
        );
        runtimePolling = runner().scheduleRepeating(
                () -> runner().runOnRenderThread(() -> {
                    checkComposeFile();
                    requestRuntimeRefresh();
                }),
                RUNTIME_REFRESH_INTERVAL
        );
        statsPolling = runner().scheduleRepeating(
                () -> runner().runOnRenderThread(() -> requestStatsRefresh()),
                RUNTIME_REFRESH_INTERVAL
        );
    }

    @Override
    protected void onStop() {
        layoutPreferences.save(layout);
        runtimePolling.cancel();
        statsPolling.cancel();
        composeEvents.close();
        logsController.close();
        serviceOperations.close();
        refreshRuntime.close();
        refreshStats.close();
    }

    @Override
    protected Element render() {
        if (helpVisible) {
            return helpPanel();
        }
        if (errorOverlayVisible) {
            return errorPanel();
        }
        if (pendingConfirmation != null) {
            return confirmationPanel();
        }

        var terminalSize = runner().tuiRunner().terminal().size();
        if (terminalSize.width() < 80 || terminalSize.height() < 24) {
            return column(
                    text("Terminal too small").fg(LIGHT_YELLOW).bold(),
                    text("Minimum recommended size: 80x24").dim()
            );
        }

        var overview = terminalSize.width() < 110
                ? column(
                        servicesPanel().percent(25),
                        detailsPanel().percent(50),
                        statsPanel().fill()
                ).spacing(1).percent(layout.overviewHeightPercent())
                : row(
                        servicesPanel().percent(layout.servicesWidthPercent()),
                        detailsPanel().percent(48),
                        statsPanel().fill()
                ).spacing(1).percent(layout.overviewHeightPercent());

        return column(
                header(),
                overview,
                logsPanel(),
                statusBar()
        ).spacing(0);
    }

    private Element helpPanel() {
        return standardPanel("󰋖 Keyboard help",
                column(
                        text("Navigation").fg(CYAN).bold(),
                        text("↑↓ / j/k       select service"),
                        text("Tab / Shift+Tab switch panel focus"),
                        text(""),
                        text("Lifecycle").fg(CYAN).bold(),
                        text("u / s / r       up, stop, restart selected"),
                        text("b / x           build or recreate selected"),
                        text("U / S / R       up, stop, restart all"),
                        text("B / X           build or recreate all"),
                        text("D               down project (confirm)"),
                        text(""),
                        text("Runtime and logs").fg(CYAN).bold(),
                        text("g               refresh runtime"),
                        text("l               selected/all logs"),
                        text("/               filter services"),
                        text("f / G / c       follow, end, clear logs"),
                        text("e               show the last full error"),
                        text(""),
                        text("Layout").fg(CYAN).bold(),
                        text("Ctrl+h/l        resize Services column"),
                        text("Ctrl+j/k        resize overview height"),
                        text(""),
                        text("?               close help"),
                        text("q               quit")
                ).spacing(0)
        ).borderColor(CYAN).padding(1).fill();
    }

    private Element errorPanel() {
        return standardPanel("󰅙 Error",
                column(
                        text("The last operation failed").fg(LIGHT_RED).bold(),
                        text(lastError),
                        text("Press e or Esc to close").dim()
                ).spacing(1)
        ).borderColor(LIGHT_RED).padding(1).fill();
    }

    private Element confirmationPanel() {
        return standardPanel("󰀪 Confirm project action",
                column(
                        text("Down will stop and remove the Compose project.").fg(LIGHT_YELLOW),
                        text("Press y to continue or n/Esc to cancel").dim()
                ).spacing(1)
        ).borderColor(LIGHT_YELLOW).padding(1).fill();
    }

    private Element header() {
        return row(
                text("󰡨 Tambo").fg(CYAN).bold(),
                text("│").dim(),
                text("Project:").dim(),
                text(projectName()).fg(LIGHT_GREEN).bold(),
                text("").fill(),
                connectionSummary()
        ).length(1);
    }

    private Element connectionSummary() {
        var connected = refreshStatus != RefreshStatus.FAILED;
        var connection = text(connected ? "󰌘 Docker Connected" : "󰅙 Docker Unavailable")
                .fg(connected ? LIGHT_GREEN : LIGHT_RED);
        var services = text("󰏗 " + allServices.size() + " services").fg(LIGHT_BLUE);
        var running = text("󰐊 " + countRuntime(RuntimeState.RUNNING) + " running")
                .fg(LIGHT_GREEN);
        var stopped = text("󰓛 " + stoppedRuntimeCount() + " stopped")
                .fg(LIGHT_RED);
        return row(
                connection,
                text("│").dim(),
                services,
                text("│").dim(),
                running,
                text("│").dim(),
                stopped
        ).spacing(1);
    }

    private long countRuntime(RuntimeState runtimeState) {
        return allServices.stream()
                .map(this::runtimeFor)
                .filter(runtime -> runtime.runtimeState() == runtimeState)
                .count();
    }

    private String serviceCountLabel() {
        return filterActive
                ? "Services · " + state.services().size() + "/" + allServices.size()
                : "Services · " + allServices.size();
    }

    private long stoppedRuntimeCount() {
        return countRuntime(RuntimeState.EXITED) + countRuntime(RuntimeState.DEAD);
    }

    private Panel servicesPanel() {
        return standardPanel("󰏗 " + serviceCountLabel(), serviceList())
                .id("services")
                .focusable()
                .focusedBorderColor(CYAN)
                .onKeyEvent(this::handleServiceKey);
    }

    private Panel logsPanel() {
        var view = logsController.view();
        Element content;
        if (view.lines().isEmpty()) {
            content = text(logEmptyMessage(view)).dim();
        } else {
            var range = logViewport.visibleRange(view.lines().size(), VISIBLE_LOG_LINES);
            var lines = view.lines().subList(range.start(), range.end()).stream()
                    .map(line -> (Element) text(line))
                    .toArray(Element[]::new);
            content = column(lines);
        }

        return standardPanel("󰆍 Logs · " + view.scope().label(), content)
                .bottomTitle(logStatusLabel(view.status())
                        + " · follow: " + (logViewport.following() ? "on" : "off"))
                .id("logs")
                .focusable()
                .focusedBorderColor(CYAN)
                .onKeyEvent(this::handleLogKey)
                .fill();
    }

    private String logEmptyMessage(LogsController.LogView view) {
        return switch (view.status()) {
            case CONNECTING -> "Connecting to log stream...";
            case FOLLOWING -> "Waiting for output...";
            case DISCONNECTED -> "Log stream disconnected";
            case FAILED -> view.message().isBlank() ? "Unable to follow logs" : view.message();
        };
    }

    private String logStatusLabel(LogsController.LogStatus status) {
        return switch (status) {
            case CONNECTING -> "connecting";
            case FOLLOWING -> "follow: on";
            case DISCONNECTED -> "disconnected";
            case FAILED -> "failed";
        };
    }

    private Panel statsPanel() {
        var usages = selectedRuntime().instances().stream()
                .map(instance -> resourceUsageByContainer.get(instance.name()))
                .filter(Objects::nonNull)
                .toList();
        Element content;
        if (usages.isEmpty()) {
            content = text("No runtime metrics available").dim();
        } else {
            content = column(usages.stream()
                    .map(this::resourceUsageCard)
                    .toArray(Element[]::new));
        }

        return standardPanel("󰍛 Resource Usage", content)
                .bottomTitle(statsStatusLabel())
                .id("stats")
                .fill();
    }

    private Element resourceUsageCard(ResourceUsage usage) {
        var cpu = lineGauge(ratio(usage.cpuPercent()))
                .label("CPU " + formatPercent(usage.cpuPercent()))
                .filledColor(LIGHT_GREEN)
                .fill();
        var memory = lineGauge(ratio(usage.memoryPercent()))
                .label("Memory " + formatPercent(usage.memoryPercent()))
                .filledColor(LIGHT_YELLOW)
                .fill();
        return column(
                text("󰘚 " + usage.containerName()).fg(LIGHT_BLUE).bold(),
                cpu,
                memory,
                text("󰈀 Network  ↓ " + usage.networkInput()
                        + "  ↑ " + usage.networkOutput()).dim(),
                text("󰈀 Processes " + usage.processCount()).dim()
        ).spacing(1);
    }

    private double ratio(double percent) {
        return Math.min(1.0, Math.max(0.0, percent / 100.0));
    }

    private String formatPercent(double percent) {
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    private String statsStatusLabel() {
        return switch (statsStatus) {
            case IDLE -> "waiting";
            case REFRESHING -> "refreshing";
            case SUCCEEDED -> "live";
            case FAILED -> statsMessage.isBlank() ? "failed" : "failed: " + compact(statsMessage);
        };
    }

    private Panel detailsPanel() {
        var service = state.selectedService();
        var runtime = selectedRuntime();
        var details = column(
                row(
                        text("Service").dim().length(DETAIL_LABEL_WIDTH),
                        text(service.name()).fg(CYAN).bold().fill()
                ),
                row(
                        text("Image").dim().length(DETAIL_LABEL_WIDTH),
                        text(service.image().orElse("not specified")).fill()
                ),
                row(
                        text("Status").dim().length(DETAIL_LABEL_WIDTH),
                        text(runtimeLabel(runtime.runtimeState())).fill()
                ),
                row(
                        text("Operation").dim().length(DETAIL_LABEL_WIDTH),
                        text(selectedOperation()).fill()
                ),
                row(
                        text("Health").dim().length(DETAIL_LABEL_WIDTH),
                        text(runtime.healthState().displayName()).fill()
                ),
                row(
                        text("Container").dim().length(DETAIL_LABEL_WIDTH),
                        text(formatContainerNames(runtime)).fill()
                ),
                row(
                        text("Containers").dim().length(DETAIL_LABEL_WIDTH),
                        text(runtime.containerCount()).fill()
                ),
                row(
                        text("Ports").dim().length(DETAIL_LABEL_WIDTH),
                        text(formatPorts(runtime.publishedPorts())).fill()
                ),
                row(
                        text("Network").dim().length(DETAIL_LABEL_WIDTH),
                        text(formatList(service.networks())).fill()
                ),
                row(
                        text("Depends on").dim().length(DETAIL_LABEL_WIDTH),
                        text(formatList(service.dependencies())).fill()
                ),
                row(
                        text("Profiles").dim().length(DETAIL_LABEL_WIDTH),
                        text(formatList(service.profiles())).fill()
                ),
                row(
                        text("Restart policy").dim().length(DETAIL_LABEL_WIDTH),
                        text(service.restartPolicy().orElse("not specified")).fill()
                ),
                row(
                        text("Environment").dim().length(DETAIL_LABEL_WIDTH),
                        text(formatEnvironment(service.environmentVariables())).fill()
                ),
                row(
                        text("Volumes").dim().length(DETAIL_LABEL_WIDTH),
                        text(formatVolumes(service.volumes())).fill()
                ),
                row(
                        text("Exit code").dim().length(DETAIL_LABEL_WIDTH),
                        text(formatExitCode(runtime)).fill()
                )
        ).spacing(0);
        return standardPanel("󰒓 Details · " + service.name(), details)
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

    private EventResult handleFilterKey(KeyEvent event) {
        if (event.isCancel()) {
            filterActive = false;
            filterQuery = "";
            rebuildFilteredServices();
            return EventResult.HANDLED;
        }
        if (event.isDeleteBackward()) {
            if (!filterQuery.isEmpty()) {
                filterQuery = filterQuery.substring(0, filterQuery.length() - 1);
                rebuildFilteredServices();
            }
            return EventResult.HANDLED;
        }
        if (event.character() >= 32 && !Character.isISOControl(event.character())) {
            var previousQuery = filterQuery;
            filterQuery += event.character();
            if (!rebuildFilteredServices()) {
                filterQuery = previousQuery;
            }
            return EventResult.HANDLED;
        }
        return EventResult.HANDLED;
    }

    private boolean rebuildFilteredServices() {
        var selectedName = state.selectedService().name();
        var matches = ServiceFilter.matching(allServices, filterQuery);
        if (matches.isEmpty()) {
            return false;
        }

        var selectedIndex = matches.stream()
                .map(ComposeService::name)
                .toList()
                .indexOf(selectedName);
        state = new UiState(matches, Math.max(0, selectedIndex));
        return true;
    }

    private Element serviceList() {
        var rows = new Element[state.services().size()];
        for (int index = 0; index < state.services().size(); index++) {
            boolean selected = index == state.selectedIndex();
            var service = state.services().get(index);
            var runtime = runtimeFor(service);
            var name = text((selected ? "▸ " : "  ") + service.name()).fill();
            var status = serviceStatus(runtime);
            if (selected) {
                name = name.fg(CYAN).bold().bg(SELECTED_BACKGROUND);
                status = status.bg(SELECTED_BACKGROUND);
            }
            rows[index] = row(name, status);
        }
        return column(rows);
    }

    private TextElement serviceStatus(ServiceRuntime runtime) {
        var state = runtime.runtimeState();
        var icon = switch (state) {
            case RUNNING -> "󰐊";
            case EXITED, DEAD -> "󰓛";
            case NOT_CREATED -> "󰝦";
            default -> "󰅙";
        };
        return text(icon + " " + runtimeLabel(state)).fg(runtimeColor(state));
    }

    private String runtimeLabel(RuntimeState state) {
        return switch (state) {
            case EXITED, DEAD -> "stopped";
            default -> state.displayName();
        };
    }

    private Color runtimeColor(RuntimeState state) {
        return switch (state) {
            case RUNNING -> LIGHT_GREEN;
            case EXITED, DEAD -> LIGHT_RED;
            case NOT_CREATED -> DARK_GRAY;
            default -> LIGHT_YELLOW;
        };
    }

    private String formatContainerNames(ServiceRuntime runtime) {
        if (runtime.instances().isEmpty()) {
            return "none";
        }
        return runtime.instances().stream()
                .map(instance -> instance.name())
                .collect(Collectors.joining(", "));
    }

    private String formatList(List<String> values) {
        return values.isEmpty() ? "none" : String.join(", ", values);
    }

    private String formatEnvironment(List<String> variables) {
        if (variables.isEmpty()) {
            return "none";
        }
        return variables.size() + " variables (" + String.join(", ", variables) + ")";
    }

    private String formatVolumes(List<String> volumes) {
        if (volumes.isEmpty()) {
            return "none";
        }
        var noun = volumes.size() == 1 ? "volume" : "volumes";
        return volumes.size() + " " + noun + " (" + String.join(", ", volumes) + ")";
    }

    private ServiceRuntime selectedRuntime() {
        return runtimeFor(state.selectedService());
    }

    private String selectedOperation() {
        if (globalOperation != null) {
            return globalOperation.activeLabel() + " all";
        }
        var operation = activeOperations.get(state.selectedService().name());
        return operation == null ? "idle" : operation.activeLabel();
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
                text("u/s/r").fg(CYAN).bold(),
                text("up/stop/restart").dim(),
                text("b/x").fg(CYAN).bold(),
                text("build/recreate").dim(),
                text("U/S/R").fg(CYAN).bold(),
                text("all").dim(),
                text("g").fg(CYAN).bold(),
                text("refresh").dim(),
                text("l").fg(CYAN).bold(),
                text("selected/all").dim(),
                text("/").fg(CYAN).bold(),
                text(filterActive ? filterQuery : "filter").dim(),
                text("f/G/c").fg(CYAN).bold(),
                text("follow/end/clear").dim(),
                text("D").fg(CYAN).bold(),
                text("down").dim(),
                text("e").fg(CYAN).bold(),
                text("details").dim(),                text("C-h/l C-j/k").fg(CYAN).bold(),
                text("resize").dim(),
                text("q").fg(CYAN).bold(),
                text("quit").dim(),
                text("").fill(),
                statusIndicator()
        ).spacing(1).length(1);
    }

    private void checkComposeFile() {
        if (composeFileChanges.hasChanged()) {
            composeFileChanged = true;
            eventMessage = "Compose file changed; restart Tambo to reload services";
        }
    }

    private Element statusIndicator() {
        if (composeFileChanged) {
            return text("󰀪 Compose file changed; restart Tambo").fg(LIGHT_YELLOW);
        }

        if (operationStatus != OperationStatus.IDLE) {
            return switch (operationStatus) {
                case RUNNING -> text("󰐊 " + operationMessage).fg(LIGHT_YELLOW);
                case SUCCEEDED -> text("󰄬 " + operationMessage).fg(LIGHT_GREEN);
                case FAILED -> text("󰅙 " + operationMessage).fg(LIGHT_RED);
                case IDLE -> throw new IllegalStateException("idle operation has no indicator");
            };
        }

        if (!eventMessage.isBlank()) {
            return text("󰅙 events: " + eventMessage).fg(LIGHT_YELLOW);
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
        if (keyEvent.isChar('q')) {
            quit();
            return EventResult.HANDLED;
        }
        if (keyEvent.isChar('?')) {
            helpVisible = !helpVisible;
            return EventResult.HANDLED;
        }
        if (keyEvent.isCancel() && (helpVisible || errorOverlayVisible || pendingConfirmation != null)) {
            helpVisible = false;
            errorOverlayVisible = false;
            pendingConfirmation = null;
            return EventResult.HANDLED;
        }
        if (pendingConfirmation != null) {
            if (keyEvent.isChar('y')) {
                var operation = pendingConfirmation;
                pendingConfirmation = null;
                return runGlobalServiceOperation(operation);
            }
            if (keyEvent.isChar('n')) {
                pendingConfirmation = null;
            }
            return EventResult.HANDLED;
        }
        if (keyEvent.isChar('e') && !lastError.isBlank()) {
            errorOverlayVisible = true;
            return EventResult.HANDLED;
        }
        if (helpVisible || errorOverlayVisible) {
            return EventResult.HANDLED;
        }
        if (filterActive) {
            return handleFilterKey(keyEvent);
        }
        if (keyEvent.isChar('/')) {
            filterActive = true;
            filterQuery = "";
            return EventResult.HANDLED;
        }
        if (keyEvent.hasCtrl()) {
            if (keyEvent.isChar('h')) {
                layout = layout.narrowerServices();
                layoutPreferences.save(layout);
                return EventResult.HANDLED;
            }
            if (keyEvent.isChar('l')) {
                layout = layout.widerServices();
                layoutPreferences.save(layout);
                return EventResult.HANDLED;
            }
            if (keyEvent.isChar('j')) {
                layout = layout.tallerOverview();
                layoutPreferences.save(layout);
                return EventResult.HANDLED;
            }
            if (keyEvent.isChar('k')) {
                layout = layout.shorterOverview();
                layoutPreferences.save(layout);
                return EventResult.HANDLED;
            }
        }
        if (keyEvent.isChar('u')) {
            return runSelectedServiceOperation(ServiceOperation.UP);
        }
        if (keyEvent.isChar('s')) {
            return runSelectedServiceOperation(ServiceOperation.STOP);
        }
        if (keyEvent.isChar('r')) {
            return runSelectedServiceOperation(ServiceOperation.RESTART);
        }
        if (keyEvent.isChar('b')) {
            return runSelectedServiceOperation(ServiceOperation.UP_BUILD);
        }
        if (keyEvent.isChar('x')) {
            return runSelectedServiceOperation(ServiceOperation.RECREATE);
        }
        if (keyEvent.isChar('U')) {
            return runGlobalServiceOperation(ServiceOperation.UP);
        }
        if (keyEvent.isChar('S')) {
            return runGlobalServiceOperation(ServiceOperation.STOP);
        }
        if (keyEvent.isChar('R')) {
            return runGlobalServiceOperation(ServiceOperation.RESTART);
        }
        if (keyEvent.isChar('B')) {
            return runGlobalServiceOperation(ServiceOperation.UP_BUILD);
        }
        if (keyEvent.isChar('X')) {
            return runGlobalServiceOperation(ServiceOperation.RECREATE);
        }
        if (keyEvent.isChar('D')) {
            pendingConfirmation = ServiceOperation.DOWN;
            return EventResult.HANDLED;
        }
        if (keyEvent.isChar('g')) {
            return requestRuntimeRefresh();
        }
        if (keyEvent.isChar('l')) {
            return toggleLogMode();
        }
        return EventResult.UNHANDLED;
    }

    private EventResult toggleLogMode() {
        logMode = logMode == LogMode.SELECTED_SERVICE
                ? LogMode.ALL_SERVICES
                : LogMode.SELECTED_SERVICE;
        logViewport = LogViewport.atEnd();
        var scope = logMode == LogMode.ALL_SERVICES
                ? LogScope.all()
                : LogScope.selected(state.selectedService().name());
        logsController.follow(scope, this::requestLogRender);
        return EventResult.HANDLED;
    }

    private EventResult runSelectedServiceOperation(ServiceOperation operation) {
        return runServiceOperation(OperationTarget.service(state.selectedService().name()), operation);
    }

    private EventResult runGlobalServiceOperation(ServiceOperation operation) {
        return runServiceOperation(OperationTarget.allServices(), operation);
    }

    private EventResult runServiceOperation(OperationTarget target, ServiceOperation operation) {
        if (target.serviceName().isPresent()) {
            var serviceName = target.serviceName().orElseThrow();
            if (globalOperation != null || activeOperations.putIfAbsent(serviceName, operation) != null) {
                return EventResult.HANDLED;
            }
        } else if (globalOperation != null || !activeOperations.isEmpty()) {
            return EventResult.HANDLED;
        } else {
            globalOperation = operation;
        }

        operationStatus = OperationStatus.RUNNING;
        operationMessage = operation.activeLabel() + " " + target.label();
        var result = target.serviceName().isPresent()
                ? serviceOperations.execute(target.serviceName().orElseThrow(), operation)
                : serviceOperations.executeAll(operation);
        result.whenComplete((lifecycleResult, error) -> {
            if (!runner().isRunning()) {
                return;
            }
            runner().runOnRenderThread(() -> finishServiceOperation(
                    target,
                    operation,
                    lifecycleResult,
                    error
            ));
        });
        return EventResult.HANDLED;
    }

    private void finishServiceOperation(
            OperationTarget target,
            ServiceOperation operation,
            LifecycleResult result,
            Throwable error
    ) {
        target.serviceName().ifPresent(activeOperations::remove);
        if (target.serviceName().isEmpty()) {
            globalOperation = null;
        }

        if (error != null) {
            lastError = errorDetails(error);
            operationStatus = OperationStatus.FAILED;
            operationMessage = operation.commandName() + " " + target.label()
                    + " failed: " + compact(lastError);
            return;
        }
        if (result instanceof LifecycleResult.Failed failure) {
            lastError = failure.message();
            operationStatus = OperationStatus.FAILED;
            operationMessage = operation.commandName() + " " + target.label()
                    + " failed: " + compact(lastError);
            return;
        }
        if (result instanceof LifecycleResult.Rejected) {
            operationStatus = OperationStatus.FAILED;
            operationMessage = target.label() + " already has an active operation";
            return;
        }

        operationStatus = OperationStatus.SUCCEEDED;
        operationMessage = operation.completedLabel() + " " + target.label();
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
        lastError = errorDetails(error);
        refreshMessage = compact(lastError);
    }

    private EventResult requestStatsRefresh() {
        if (statsStatus == StatsStatus.REFRESHING) {
            return EventResult.HANDLED;
        }

        statsStatus = StatsStatus.REFRESHING;
        statsMessage = "";
        refreshStats.execute(allContainers()).whenComplete((stats, error) -> {
            if (!runner().isRunning()) {
                return;
            }
            runner().runOnRenderThread(() -> finishStats(stats, error));
        });
        return EventResult.HANDLED;
    }

    private List<ContainerInstance> allContainers() {
        return runtimeByService.values().stream()
                .flatMap(runtime -> runtime.instances().stream())
                .toList();
    }

    private void finishStats(Map<String, ResourceUsage> stats, Throwable error) {
        if (error == null) {
            resourceUsageByContainer = Map.copyOf(stats);
            statsStatus = StatsStatus.SUCCEEDED;
            return;
        }

        statsStatus = StatsStatus.FAILED;
        lastError = errorDetails(error);
        statsMessage = compact(lastError);
    }

    private String errorMessage(Throwable error) {
        return compact(errorDetails(error));
    }

    private String errorDetails(Throwable error) {
        var cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause()
                : error;
        return cause.getMessage() == null || cause.getMessage().isBlank()
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }

    private String compact(String message) {
        return message.replaceAll("\\s+", " ").strip();
    }

    private EventResult handleServiceKey(KeyEvent event) {
        if (event.isChar('q')) {
            quit();
            return EventResult.HANDLED;
        }
        if (filterActive) {
            return handleFilterKey(event);
        }
        var previousSelection = state.selectedIndex();
        if (event.isDown() || event.isChar('j')) {
            state = state.selectNext();
        } else if (event.isUp() || event.isChar('k')) {
            state = state.selectPrevious();
        } else {
            return EventResult.UNHANDLED;
        }

        if (state.selectedIndex() != previousSelection) {
            logViewport = LogViewport.atEnd();
            if (logMode == LogMode.SELECTED_SERVICE) {
                logsController.follow(
                        LogScope.selected(state.selectedService().name()),
                        this::requestLogRender
                );
            }
        }
        return EventResult.HANDLED;
    }

    private EventResult handleLogKey(KeyEvent event) {
        if (event.isChar('q')) {
            quit();
            return EventResult.HANDLED;
        }
        int lineCount = logsController.view().lines().size();
        if (event.isUp() || event.isChar('k')) {
            logViewport = logViewport.scrollUp(lineCount);
        } else if (event.isDown() || event.isChar('j')) {
            logViewport = logViewport.scrollDown(lineCount);
        } else if (event.isChar('f')) {
            logViewport = logViewport.toggleFollow(lineCount);
        } else if (event.isChar('G')) {
            logViewport = logViewport.jumpToEnd();
        } else if (event.isChar('c')) {
            logsController.clear();
            logViewport = LogViewport.atEnd();
        } else {
            return EventResult.UNHANDLED;
        }
        return EventResult.HANDLED;
    }

    private void requestLogRender() {
        if (runner().isRunning()) {
            runner().runOnRenderThread(() -> { });
        }
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

    private enum StatsStatus {
        IDLE,
        REFRESHING,
        SUCCEEDED,
        FAILED
    }
}
