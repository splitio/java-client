package io.split.client.impressions;

import java.util.concurrent.ThreadFactory;

public final class ImpressionsManagerConfig {

    private final ImpressionsManager.Mode _mode;
    private final int _impressionsRefreshRateSeconds;
    private final ThreadFactory _threadFactory;
    private final boolean _debugEnabled;

    private ImpressionsManagerConfig(Builder builder) {
        _mode = builder._mode;
        _impressionsRefreshRateSeconds = builder._impressionsRefreshRateSeconds;
        _threadFactory = builder._threadFactory;
        _debugEnabled = builder._debugEnabled;
    }

    public ImpressionsManager.Mode mode() { return _mode; }
    public int impressionsRefreshRateSeconds() { return _impressionsRefreshRateSeconds; }
    public ThreadFactory threadFactory() { return _threadFactory; }
    public boolean debugEnabled() { return _debugEnabled; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ImpressionsManager.Mode _mode = ImpressionsManager.Mode.OPTIMIZED;
        private int _impressionsRefreshRateSeconds = 60;
        private ThreadFactory _threadFactory = null;
        private boolean _debugEnabled = false;

        public Builder mode(ImpressionsManager.Mode mode) { _mode = mode; return this; }
        public Builder impressionsRefreshRateSeconds(int rate) { _impressionsRefreshRateSeconds = rate; return this; }
        public Builder threadFactory(ThreadFactory tf) { _threadFactory = tf; return this; }
        public Builder debugEnabled(boolean debug) { _debugEnabled = debug; return this; }

        public ImpressionsManagerConfig build() { return new ImpressionsManagerConfig(this); }
    }
}
