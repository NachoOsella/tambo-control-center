package app.tambo.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutStateTest {
    @Test
    void changesPanelRatiosInSmallSteps() {
        var layout = LayoutState.defaults();

        assertEquals(new LayoutState(35, 60), layout.widerServices().tallerOverview());
        assertEquals(new LayoutState(25, 50), layout.narrowerServices().shorterOverview());
    }

    @Test
    void stopsAtConfiguredBounds() {
        var layout = new LayoutState(45, 70);

        assertEquals(layout, layout.widerServices().tallerOverview());
        assertEquals(new LayoutState(20, 35),
                new LayoutState(20, 35).narrowerServices().shorterOverview());
    }
}
