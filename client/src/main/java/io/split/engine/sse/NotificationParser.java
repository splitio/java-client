package io.split.engine.sse;

import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.exceptions.EventParsingException;

public interface NotificationParser {
    IncomingNotification parseMessage(String payload) throws EventParsingException;
    ErrorNotification parseError(String payload) throws EventParsingException;
}
