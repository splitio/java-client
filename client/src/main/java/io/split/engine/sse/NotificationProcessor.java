package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;

public interface NotificationProcessor {
    void process(IncomingNotification notification);
}
