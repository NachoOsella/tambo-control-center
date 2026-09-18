package app.tambo.ui;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class TerminalClipboard {
    private TerminalClipboard() {
    }

    static void copy(String value) {
        var encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        System.out.print("\u001b]52;c;" + encoded + "\u0007");
        System.out.flush();
    }
}
