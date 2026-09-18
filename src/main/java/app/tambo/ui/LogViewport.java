package app.tambo.ui;

record LogViewport(boolean following, int frozenEnd) {
    LogViewport {
        if (frozenEnd < 0) {
            throw new IllegalArgumentException("frozen end cannot be negative");
        }
    }

    static LogViewport atEnd() {
        return new LogViewport(true, 0);
    }

    LogViewport scrollUp(int lineCount) {
        if (lineCount == 0) {
            return this;
        }
        int currentEnd = following ? lineCount : Math.min(frozenEnd, lineCount);
        return new LogViewport(false, Math.max(0, currentEnd - 1));
    }

    LogViewport scrollDown(int lineCount) {
        if (following) {
            return this;
        }
        int nextEnd = Math.min(lineCount, frozenEnd + 1);
        return nextEnd == lineCount
                ? atEnd()
                : new LogViewport(false, nextEnd);
    }

    LogViewport toggleFollow(int lineCount) {
        return following
                ? new LogViewport(false, lineCount)
                : atEnd();
    }

    LogViewport jumpToEnd() {
        return atEnd();
    }

    VisibleRange visibleRange(int lineCount, int maximumLines) {
        int end = following ? lineCount : Math.min(frozenEnd, lineCount);
        int start = Math.max(0, end - maximumLines);
        return new VisibleRange(start, end);
    }

    record VisibleRange(int start, int end) {
    }
}
