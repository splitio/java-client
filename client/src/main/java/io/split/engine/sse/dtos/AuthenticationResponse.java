package io.split.engine.sse.dtos;

public class AuthenticationResponse {
    private boolean pushEnabled;
    private String token;
    private String channels;
    private double expiration;
    private boolean retry;

    public AuthenticationResponse(boolean pushEnabled, boolean retry) {
        setPushEnabled(pushEnabled);
        setRetry(retry);
    }

    public double getExpiration() {
        return expiration;
    }

    public void setExpiration(double expiration) {
        this.expiration = expiration;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }
}
