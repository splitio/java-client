package io.split.client.exceptions;

/**
 * Created by adilaijaz on 1/5/17.
 */
public class ChangeNumberExceptionWrapper extends Exception {
    private final Exception _delegate;
    private final long _changeNumber;


    public ChangeNumberExceptionWrapper(Exception delegate, long changeNumber) {
        _delegate = delegate;
        _changeNumber = changeNumber;
    }

    public Exception wrappedException() {
        return _delegate;
    }

    public long changeNumber() {
        return _changeNumber;
    }
}
