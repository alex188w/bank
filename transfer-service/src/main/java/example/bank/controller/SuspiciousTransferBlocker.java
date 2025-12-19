package example.bank.controller;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class SuspiciousTransferBlocker {

    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();

    // 5% блокировок — алгоритм “на выбор” из ТЗ
    public boolean shouldBlock() {
        return rnd.nextInt(100) < 5;
    }
}
