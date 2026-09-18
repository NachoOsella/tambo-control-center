package app.tambo.ui;

import app.tambo.domain.service.ComposeService;

import java.util.List;
import java.util.Locale;

final class ServiceFilter {
    private ServiceFilter() {
    }

    static List<ComposeService> matching(List<ComposeService> services, String query) {
        var normalizedQuery = query.toLowerCase(Locale.ROOT);
        return services.stream()
                .filter(service -> normalizedQuery.isBlank()
                        || service.name().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
    }
}
