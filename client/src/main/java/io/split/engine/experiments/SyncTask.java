package io.split.engine.experiments;

public interface SyncTask {

    void start();
    void stop();
    void close();
    boolean isRunning();
}