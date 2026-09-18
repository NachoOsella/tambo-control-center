package app.tambo.domain.logs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogBufferTest {
    @Test
    void retainsOnlyTheNewestLines() {
        var buffer = new LogBuffer(3);

        buffer.add("one");
        buffer.add("two");
        buffer.add("three");
        buffer.add("four");

        assertEquals(List.of("two", "three", "four"), buffer.snapshot());
    }

    @Test
    void clearsAllLines() {
        var buffer = new LogBuffer(3);
        buffer.add("line");

        buffer.clear();

        assertEquals(List.of(), buffer.snapshot());
    }
}
