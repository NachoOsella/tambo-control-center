package app.tambo.ui;

import app.tambo.domain.service.ComposeService;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiStateTest {
    private static final List<ComposeService> SERVICES = List.of(
            service("gateway"),
            service("bank"),
            service("postgres")
    );

    @Test
    void selectsTheFirstServiceInitially() {
        var state = new UiState(SERVICES, 0);

        assertEquals("gateway", state.selectedService().name());
    }

    @Test
    void movesSelectionBetweenServices() {
        var state = new UiState(SERVICES, 0)
                .selectNext()
                .selectNext()
                .selectPrevious();

        assertEquals("bank", state.selectedService().name());
    }

    @Test
    void keepsSelectionWithinTheServiceList() {
        var beforeFirst = new UiState(SERVICES, 0).selectPrevious();
        var afterLast = new UiState(SERVICES, 2).selectNext();

        assertEquals("gateway", beforeFirst.selectedService().name());
        assertEquals("postgres", afterLast.selectedService().name());
    }

    private static ComposeService service(String name) {
        return new ComposeService(name, Optional.empty());
    }
}
