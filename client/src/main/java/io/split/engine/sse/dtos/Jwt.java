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

    public long getExpiration() {
        return expiration;
    }

    public long getIssueAt() {
        return issueAt;
    }
}
