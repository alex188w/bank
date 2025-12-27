package example.bank.logging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountAudit {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    public void info(String action, String message, String type, String status) {
        try (var a = MDC.putCloseable("event.action", action);
                var t = MDC.putCloseable("operation.type", type);
                var s = MDC.putCloseable("operation.status", status)) {
            log.info(message);
        }
    }

    public void warn(String action, String msg) {
        withAction(action, () -> log.warn(msg));
    }

    public void error(String action, String msg, Throwable ex) {
        withAction(action, () -> log.error(msg, ex));
    }

    private void withAction(String action, Runnable r) {
        try (var c = LogAction.with(action)) {
            r.run();
        } catch (Exception ignored) {
            r.run();
        }
    }
}