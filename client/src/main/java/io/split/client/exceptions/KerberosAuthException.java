package io.split.client.exceptions;

public class KerberosAuthException extends Exception {
    public KerberosAuthException(String message) {
        super(message);
    }
    public KerberosAuthException(String message, Throwable exception) {
        super(message, exception);
    }
}
