package app.tambo.domain.service;

import java.util.Locale;

public enum HealthState {
    NOT_CONFIGURED("not-configured"),
    STARTING("starting"),
    HEALTHY("healthy"),
    UNHEALTHY("unhealthy"),
    UNKNOWN("unknown");

    private final String displayName;

    HealthState(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static HealthState fromCompose(String value) {
        if (value == null || value.isBlank()) {
            return NOT_CONFIGURED;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
