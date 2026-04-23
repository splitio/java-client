package io.split.client.events;

public interface EventQueueStats {
    void onQueued(long count);
    void onDropped(long count);
}
