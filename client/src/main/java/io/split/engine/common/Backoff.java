package io.split.engine.common;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class Backoff {
    private static final long BACKOFF_MAX_ALLOWED = 1800;

    private final long _backoffBase;
    private AtomicInteger _attempt;
    private final long _maxAllowed;

    public Backoff(long backoffBase) {
        this(backoffBase, BACKOFF_MAX_ALLOWED);
    }

    public Backoff(long backoffBase, long maxAllowed) {
        _backoffBase = checkNotNull(backoffBase);
        _attempt = new AtomicInteger(0);
        _maxAllowed = maxAllowed;
    }

    public long interval() {
        long interval = _backoffBase * (long) Math.pow(2, _attempt.getAndIncrement());

        return interval >= _maxAllowed ? BACKOFF_MAX_ALLOWED : interval;
    }

    public synchronized void reset() {
        _attempt.set(0);
    }
}
