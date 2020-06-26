package io.split.engine.sse.dtos;

public class ErrorNotification {
    private final String statusCode;
    private final String message;

    public ErrorNotification(String statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public String getMessage() { return message; }

    public String getStatusCode() { return statusCode; }
}
