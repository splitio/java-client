package io.split.engine.experiments;

import java.util.concurrent.atomic.AtomicBoolean;

public interface SyncTask {

    void start();
    void stop();
    void close();
    boolean isRunning();
}
