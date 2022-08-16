package io.split.engine.common;

public interface SyncManager {
    void start();
    void shutdown(long splitCount, long segmentCount, long segmentKeyCount);
}
