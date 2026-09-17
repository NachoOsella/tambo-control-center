package app.tambo.domain.service;

import java.util.Locale;

public enum RuntimeState {
    NOT_CREATED("not-created"),
    CREATED("created"),
    RUNNING("running"),
    RESTARTING("restarting"),
    PAUSED("paused"),
    EXITED("exited"),
    DEAD("dead"),
    REMOVING("removing"),
    UNKNOWN("unknown");

    private final String displayName;

    RuntimeState(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static RuntimeState fromCompose(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
