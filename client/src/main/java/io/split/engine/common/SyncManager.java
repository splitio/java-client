package io.split.engine.common;

import java.io.IOException;

public interface SyncManager {
    void start();
    void shutdown() throws IOException;
}
