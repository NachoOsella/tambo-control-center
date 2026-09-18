package app.tambo.application.service;

public enum ServiceOperation {
    UP("up", "starting", "started"),
    STOP("stop", "stopping", "stopped"),
    RESTART("restart", "restarting", "restarted");

    private final String commandName;
    private final String activeLabel;
    private final String completedLabel;

    ServiceOperation(String commandName, String activeLabel, String completedLabel) {
        this.commandName = commandName;
        this.activeLabel = activeLabel;
        this.completedLabel = completedLabel;
    }

    public String commandName() {
        return commandName;
    }

    public String activeLabel() {
        return activeLabel;
    }

    public String completedLabel() {
        return completedLabel;
    }
}
