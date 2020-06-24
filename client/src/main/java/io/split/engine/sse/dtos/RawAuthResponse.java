package io.split.engine.sse.dtos;

public class RawAuthResponse {
    private final boolean pushEnabled;
    private final String token;

    public RawAuthResponse(boolean pushEnabled, String token) {
        this.pushEnabled = pushEnabled;
        this.token = token;
    }

    public boolean isPushEnabled() { return pushEnabled; }

    public String getToken() { return token; }
}
