package example.bank.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferAudit {

    private static final Logger log = LoggerFactory.getLogger(TransferAudit.class);

    public void info(String action, String message) {
        withAction(action, () -> log.info(message));
    }

    public void warn(String action, String message) {
        withAction(action, () -> log.warn(message));
    }

    public void error(String action, String message, Throwable ex) {
        withAction(action, () -> log.error(message, ex));
    }

    private void withAction(String action, Runnable r) {
        if (action == null || action.isBlank()) {
            r.run();
            return;
        }
        try (var c = MDC.putCloseable("event.action", action)) {
            r.run();
        } catch (Exception ignored) {
            r.run();
        }
    }
}