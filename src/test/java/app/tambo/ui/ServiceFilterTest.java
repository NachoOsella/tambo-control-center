package app.tambo.ui;

import app.tambo.domain.service.ComposeService;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceFilterTest {
    private final List<ComposeService> services = List.of(
            new ComposeService("api", Optional.empty()),
            new ComposeService("gateway", Optional.empty()),
            new ComposeService("postgres", Optional.empty())
    );

    @Test
    void matchesServiceNamesCaseInsensitively() {
        assertEquals(List.of("gateway"), ServiceFilter.matching(services, "WAY")
                .stream()
                .map(ComposeService::name)
                .toList());
    }

    @Test
    void blankQueryReturnsAllServices() {
        assertEquals(services, ServiceFilter.matching(services, ""));
    }
}
