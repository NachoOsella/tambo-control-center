package app.tambo.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutStateTest {
    @Test
    void changesPanelRatiosInSmallSteps() {
        var layout = LayoutState.defaults();

        assertEquals(new LayoutState(35, 50), layout.widerServices().tallerOverview());
        assertEquals(new LayoutState(25, 40), layout.narrowerServices().shorterOverview());
    }

    @Test
    void stopsAtConfiguredBounds() {
        var layout = new LayoutState(45, 65);

        assertEquals(layout, layout.widerServices().tallerOverview());
        assertEquals(new LayoutState(20, 30),
                new LayoutState(20, 30).narrowerServices().shorterOverview());
    }
}
