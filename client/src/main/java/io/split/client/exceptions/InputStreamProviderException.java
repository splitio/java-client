package io.split.client.exceptions;

public class InputStreamProviderException extends Exception {
    private final String _fileName;

    public InputStreamProviderException(String fileName, String message) {
        super(message);
        _fileName = fileName;
    }

    public String getFileName() {
        return _fileName;
    }
}