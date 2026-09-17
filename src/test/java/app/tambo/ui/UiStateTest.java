package app.tambo.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiStateTest {
    private static final List<String> SERVICES = List.of("gateway", "bank", "postgres");

    @Test
    void selectsTheFirstServiceInitially() {
        var state = new UiState(SERVICES, 0);

        assertEquals("gateway", state.selectedService());
    }

    @Test
    void movesSelectionBetweenServices() {
        var state = new UiState(SERVICES, 0)
                .selectNext()
                .selectNext()
                .selectPrevious();

        assertEquals("bank", state.selectedService());
    }

    @Test
    void keepsSelectionWithinTheServiceList() {
        var beforeFirst = new UiState(SERVICES, 0).selectPrevious();
        var afterLast = new UiState(SERVICES, 2).selectNext();

        assertEquals("gateway", beforeFirst.selectedService());
        assertEquals("postgres", afterLast.selectedService());
    }
}
