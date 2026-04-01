package io.split.rules.exceptions;

public class VersionedExceptionWrapper extends Exception {
    private final Exception _wrappedException;
    private final Long _version;

    public VersionedExceptionWrapper(Exception wrappedException, Long version) {
        _wrappedException = wrappedException;
        _version = version;
    }

    public Exception wrappedException() {
        return _wrappedException;
    }

    public Long version() {
        return _version;
    }
}
