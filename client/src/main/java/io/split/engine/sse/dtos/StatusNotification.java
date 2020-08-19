package io.split.engine.sse.dtos;

import io.split.engine.sse.PushStatusTracker;

public interface StatusNotification {
    void handlerStatus(PushStatusTracker pushStatusTracker);
}
