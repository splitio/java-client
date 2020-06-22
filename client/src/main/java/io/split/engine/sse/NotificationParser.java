package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.exceptions.EventParsingException;

public interface NotificationParser {
    IncomingNotification parse(String type, String payload) throws EventParsingException;
}
