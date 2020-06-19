package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;

public interface NotificationParser {
    IncomingNotification parse(String type, String payload);
}
