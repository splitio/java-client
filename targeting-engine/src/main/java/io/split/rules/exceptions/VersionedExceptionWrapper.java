package io.split.rules.exceptions;

public class VersionedExceptionWrapper extends Exception {
    private final Exception _wrappedException;

    public VersionedExceptionWrapper(Exception wrappedException) {
        _wrappedException = wrappedException;
    }

    public Exception wrappedException() {
        return _wrappedException;
    }
}
