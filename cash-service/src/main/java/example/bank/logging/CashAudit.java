package example.bank.logging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import static example.bank.logging.LogAction.with;

@Service
@RequiredArgsConstructor
public class CashAudit {

    private static final Logger log = LoggerFactory.getLogger(CashAudit.class);

    public void info(String action, String type, String status, String message) {
        withCtx(action, type, status, () -> log.info(message));
    }

    public void warn(String action, String type, String status, String message) {
        withCtx(action, type, status, () -> log.warn(message));
    }

    public void error(String action, String type, String status, String message, Throwable ex) {
        withCtx(action, type, status, () -> log.error(message, ex));
    }

    private void withCtx(String action, String type, String status, Runnable r) {
        try (var a = with(action);
             var t = MDC.putCloseable("operation.type", safe(type));
             var s = MDC.putCloseable("operation.status", safe(status))) {
            r.run();
        } catch (Exception ignored) {
            r.run();
        }
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
