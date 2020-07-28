package io.split.engine.sse;

public interface SSEHandler {
    void start(String token, String channels);
    void stop();
    void startWorkers();
    void stopWorkers();
}
