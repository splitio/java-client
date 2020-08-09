package io.split.engine.sse;

public interface EventSourceClient {
    boolean start(String channelList, String token);
    void stop();
}
