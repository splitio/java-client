package io.split.engine.sse;

class StatusMessage {
    public enum Code {
        CONNECTED,
        RETRYABLE_ERROR,
        NONRETRYABLE_ERROR,
        DISCONNECTED
    }

    public final Code code;

    StatusMessage(Code _code) {
        code = _code;
    }
}

