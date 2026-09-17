package app.tambo.ui;

import java.util.List;

record UiState(List<String> services, int selectedIndex) {
    UiState {
        services = List.copyOf(services);
        if (services.isEmpty()) {
            throw new IllegalArgumentException("services cannot be empty");
        }
        if (selectedIndex < 0 || selectedIndex >= services.size()) {
            throw new IllegalArgumentException("selected index is outside the service list");
        }
    }

    String selectedService() {
        return services.get(selectedIndex);
    }

    UiState selectNext() {
        int nextIndex = Math.min(selectedIndex + 1, services.size() - 1);
        return new UiState(services, nextIndex);
    }

    UiState selectPrevious() {
        int previousIndex = Math.max(selectedIndex - 1, 0);
        return new UiState(services, previousIndex);
    }
}
