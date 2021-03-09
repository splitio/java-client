package io.split.engine.common;

public interface PushManager {

    enum Status {
        STREAMING_READY,
        STREAMING_BACKOFF,
        STREAMING_DOWN,
        STREAMING_OFF
    }

    void start();
    void stop();
    void startWorkers();
    void stopWorkers();
    void scheduleConnectionReset();
}
