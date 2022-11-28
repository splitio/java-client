package io.split.engine.experiments;

public interface SplitSyncTask {

    void startPeriodicFetching();
    void stop();
    void close();
}
