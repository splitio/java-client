package io.split.engine.sse.dtos;

import com.google.gson.annotations.SerializedName;

public class Jwt {
    @SerializedName("x-ably-capability")
    private String capability;
    @SerializedName("x-ably-clientId")
    private String clientId;
    @SerializedName("exp")
    private long expiration;
    @SerializedName("iat")
    private long issueAt;

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getIssueAt() {
        return issueAt;
    }

    public void setIssueAt(long issueAt) {
        this.issueAt = issueAt;
    }
}
