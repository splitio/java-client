package io.split.engine.common;

public interface Synchronizer {
    void syncAll();
    void startPeriodicFetching();
    void stopPeriodicFetching();
}
