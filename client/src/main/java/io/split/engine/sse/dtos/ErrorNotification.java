package io.split.engine.sse.dtos;

public class ErrorNotification {
    private final String statusCode;
    private final String message;
    private final int code;

    public ErrorNotification(String statusCode, String message, int code) {
        this.statusCode = statusCode;
        this.message = message;
        this.code = code;
    }

    public String getMessage() { return message; }

    public String getStatusCode() { return statusCode; }

    public int getCode() { return code; }
}
