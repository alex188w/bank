package example.bank.logging;

import org.slf4j.MDC;

public final class LogAction {
    private LogAction() {}

    public static AutoCloseable with(String action) {
        if (action == null || action.isBlank()) return () -> {};
        return MDC.putCloseable("event.action", action);
    }
}
