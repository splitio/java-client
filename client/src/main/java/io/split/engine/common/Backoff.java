package io.split.engine.common;

import java.util.concurrent.atomic.AtomicInteger;

public class Backoff {
    private final AtomicInteger _backoffBase;
    private AtomicInteger _attempt;

    public Backoff(int backoffBase) {
        _backoffBase = new AtomicInteger(backoffBase);
        _attempt = new AtomicInteger(0);
    }

    public long interval() {
        double interval = 0;

        int attempt = _attempt.get();
        if (attempt > 0) {
            interval = _backoffBase.get() * Math.pow(2, attempt);
        }

        _attempt.set(attempt++);

        return (long) interval;
    }

    public void reset() {
        _attempt.set(0);
    }
}
