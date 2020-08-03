package io.split.engine.sse;

import io.split.engine.sse.listeners.FeedbackLoopListener;

public interface SSEHandler {
    boolean start(String token, String channels);
    void stop();
    void startWorkers();
    void stopWorkers();
    void registerFeedbackListener(FeedbackLoopListener listener);
}
