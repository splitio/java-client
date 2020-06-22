package io.split.engine.sse.exceptions;

public class EventParsingException extends Exception {
    private final Exception _delegate;
    private final String _payload;

    public EventParsingException(Exception delegate, String payload) {
        _delegate = delegate;
        _payload = payload;
    }

    public Exception wrappedException() {
        return _delegate;
    }

    public String getPayload() {
        return _payload;
    }
}
