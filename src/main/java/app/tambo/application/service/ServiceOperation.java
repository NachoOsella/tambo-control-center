package app.tambo.application.service;

public enum ServiceOperation {
    UP("up", "starting", "started"),
    UP_BUILD("up --build", "building", "built"),
    STOP("stop", "stopping", "stopped"),
    RESTART("restart", "restarting", "restarted"),
    RECREATE("recreate", "recreating", "recreated"),
    DOWN("down", "bringing down", "down");

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
