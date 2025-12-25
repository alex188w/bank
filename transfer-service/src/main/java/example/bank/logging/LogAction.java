package example.bank.logging;

import org.slf4j.MDC;

public final class LogAction {
    private LogAction() {}

    public static AutoCloseable with(String action) {
        return MDC.putCloseable("event.action", action);
    }
}
