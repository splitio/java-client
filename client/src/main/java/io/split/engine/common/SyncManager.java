package io.split.engine.common;

import java.io.IOException;

public interface SyncManager {
    void start();
    void shutdown(long splitCount, long segmentCount, long segmentKeyCount) throws IOException;
}
