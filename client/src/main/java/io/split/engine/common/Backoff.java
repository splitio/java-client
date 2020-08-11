package io.split.engine.common;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class Backoff {
    private static final long BACKOFF_MAX_SECONDS_ALLOWED = 1800;

    private final long _backoffBase;
    private AtomicInteger _attempt;

    public Backoff(long backoffBase) {
        _backoffBase = checkNotNull(backoffBase);
        _attempt = new AtomicInteger(0);
    }

    public long interval() {
        long interval = _backoffBase * (long) Math.pow(2, _attempt.getAndIncrement());

        return interval >= BACKOFF_MAX_SECONDS_ALLOWED ? BACKOFF_MAX_SECONDS_ALLOWED : interval;
    }

    public void reset() {
        _attempt.set(0);
    }
}
