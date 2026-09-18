package app.tambo.ui.component;

import app.tambo.domain.service.ResourceUsage;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;

import java.util.List;
import java.util.Locale;

import static dev.tamboui.style.Color.DARK_GRAY;
import static dev.tamboui.style.Color.LIGHT_BLUE;
import static dev.tamboui.style.Color.LIGHT_GREEN;
import static dev.tamboui.style.Color.LIGHT_YELLOW;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.lineGauge;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

public final class StatsPanel {
    private StatsPanel() {
    }

    public static Panel render(List<ResourceUsage> usages, String statusLabel) {
        Element content;
        if (usages.isEmpty()) {
            content = text("No runtime metrics available").dim();
        } else {
            content = column(usages.stream()
                    .map(StatsPanel::resourceUsageCard)
                    .toArray(Element[]::new));
        }

        return panel("󰍛 Resource Usage", content)
                .borderColor(DARK_GRAY)
                .bottomTitle(statusLabel)
                .id("stats")
                .fill();
    }

    private static Element resourceUsageCard(ResourceUsage usage) {
        var cpu = lineGauge(ratio(usage.cpuPercent()))
                .label("CPU " + formatPercent(usage.cpuPercent()))
                .filledColor(LIGHT_GREEN)
                .fill();
        var memory = lineGauge(ratio(usage.memoryPercent()))
                .label("Memory " + formatPercent(usage.memoryPercent()))
                .filledColor(LIGHT_YELLOW)
                .fill();
        return column(
                text("󰘚 " + usage.containerName()).fg(LIGHT_BLUE).bold(),
                cpu,
                memory,
                text("󰈀 Network  ↓ " + usage.networkInput()
                        + "  ↑ " + usage.networkOutput()).dim(),
                text("󰈀 Processes " + usage.processCount()).dim()
        ).spacing(1);
    }

    private static double ratio(double percent) {
        return Math.min(1.0, Math.max(0.0, percent / 100.0));
    }

    private static String formatPercent(double percent) {
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }
}
