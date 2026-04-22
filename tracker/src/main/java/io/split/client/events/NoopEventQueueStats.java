package io.split.client.events;

public final class NoopEventQueueStats implements EventQueueStats {
    public static final NoopEventQueueStats INSTANCE = new NoopEventQueueStats();
    private NoopEventQueueStats() {}

    @Override public void onQueued(long count) {
        // no-op
    }

    @Override public void onDropped(long count) {
        // no-op
    }
}
