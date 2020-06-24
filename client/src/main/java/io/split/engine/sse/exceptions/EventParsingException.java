package io.split.engine.sse.exceptions;

public class EventParsingException extends Exception {
    private final String _payload;

    public EventParsingException(String message, Throwable cause, String payload) {
        super(message, cause);
        _payload = payload;
    }

    public EventParsingException(String message, String payload) {
        super(message);
        _payload = payload;
    }

    public String getPayload() {
        return _payload;
    }
}
