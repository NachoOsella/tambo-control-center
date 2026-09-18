package app.tambo.domain.logs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class LogBuffer {
    private final int capacity;
    private final Deque<String> lines;

    public LogBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.lines = new ArrayDeque<>(capacity);
    }

    public synchronized void add(String line) {
        if (lines.size() == capacity) {
            lines.removeFirst();
        }
        lines.addLast(line);
    }

    public synchronized List<String> snapshot() {
        return List.copyOf(new ArrayList<>(lines));
    }

    public synchronized void clear() {
        lines.clear();
    }
}
