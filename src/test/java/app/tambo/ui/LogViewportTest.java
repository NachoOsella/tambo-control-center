package app.tambo.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogViewportTest {
    @Test
    void followingShowsTheNewestLines() {
        var viewport = LogViewport.atEnd();

        assertEquals(new LogViewport.VisibleRange(7, 12), viewport.visibleRange(12, 5));
    }

    @Test
    void scrollingUpFreezesTheVisibleEnd() {
        var viewport = LogViewport.atEnd().scrollUp(12);

        assertFalse(viewport.following());
        assertEquals(new LogViewport.VisibleRange(6, 11), viewport.visibleRange(13, 5));
    }

    @Test
    void scrollingBackToTheEndResumesFollowing() {
        var viewport = LogViewport.atEnd().scrollUp(12).scrollDown(12);

        assertTrue(viewport.following());
        assertEquals(new LogViewport.VisibleRange(7, 12), viewport.visibleRange(12, 5));
    }
}
