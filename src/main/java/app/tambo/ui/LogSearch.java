package app.tambo.ui;

import java.util.List;
import java.util.Locale;

final class LogSearch {
    private LogSearch() {
    }

    static int find(List<String> lines, String query, int startIndex) {
        if (query.isBlank() || lines.isEmpty()) {
            return -1;
        }
        var normalizedQuery = query.toLowerCase(Locale.ROOT);
        int start = Math.max(0, Math.min(startIndex, lines.size() - 1));
        for (int index = start; index < lines.size(); index++) {
            if (lines.get(index).toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return index;
            }
        }
        return -1;
    }
}
