package io.split.engine.common;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class Backoff {
    private final long _backoffBase;
    private AtomicInteger _attempt;

    public Backoff(long backoffBase) {
        _backoffBase = checkNotNull(backoffBase);
        _attempt = new AtomicInteger(0);
    }

    public long interval() {
        return _backoffBase * (long) Math.pow(2, _attempt.getAndIncrement());
    }

    public void reset() {
        _attempt.set(0);
    }
}
