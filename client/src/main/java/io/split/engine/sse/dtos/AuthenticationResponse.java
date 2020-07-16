package io.split.engine.sse.dtos;

public class AuthenticationResponse {
    private final boolean pushEnabled;
    private final String token;
    private final String channels;
    private final long expiration;
    private final boolean retry;

    public AuthenticationResponse(boolean pushEnabled, String token, String channels, long expiration, boolean retry) {
        this.pushEnabled = pushEnabled;
        this.token = token;
        this.channels = channels;
        this.expiration = expiration;
        this.retry = retry;
    }

    public AuthenticationResponse(boolean pushEnabled, boolean retry) {
        this.pushEnabled = pushEnabled;
        this.retry = retry;
        this.token = null;
        this.channels = null;
        this.expiration = 0;
    }

    public long getExpiration() {
        return expiration;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public String getToken() {
        return token;
    }

    public String getChannels() {
        return channels;
    }

    public boolean isRetry() {
        return retry;
    }
}
