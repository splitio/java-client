package io.split.engine.sse.listeners;

public interface NotificationKeeperListener {
    void onStreamingAvailable();
    void onStreamingDisabled();
    void onStreamingShutdown();
}
