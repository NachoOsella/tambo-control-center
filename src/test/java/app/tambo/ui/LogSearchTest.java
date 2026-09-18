package app.tambo.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogSearchTest {
    private final List<String> lines = List.of(
            "gateway started",
            "waiting for database",
            "database ready",
            "request completed"
    );

    @Test
    void findsMatchesCaseInsensitivelyFromAnOffset() {
        assertEquals(2, LogSearch.find(lines, "DATABASE", 2));
    }

    @Test
    void returnsNoMatchForBlankOrMissingQueries() {
        assertEquals(-1, LogSearch.find(lines, "", 0));
        assertEquals(-1, LogSearch.find(lines, "redis", 0));
    }
}
