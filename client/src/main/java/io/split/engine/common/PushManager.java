package io.split.engine.common;

public interface PushManager {

    public enum Status {
        STREAMING_READY,
        STREAMING_ENABLED,
        STREAMING_DISABLED,
        STREAMING_PAUSED,
        RETRYABLE_ERROR,
        NONRETRYABLE_ERROR
    }

    void start();
    void stop();
    void startWorkers();
    void stopWorkers();
}
