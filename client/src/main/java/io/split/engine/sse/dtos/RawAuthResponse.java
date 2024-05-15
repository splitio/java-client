package io.split.engine.sse.dtos;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.split.client.utils.Json;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RawAuthResponse {
    private final static Integer PUSH_SECONDS_BEFORE_EXPIRATION = 600;

    private final boolean pushEnabled;
    private final String token;
    private final Jwt jwt;

    public RawAuthResponse(boolean pushEnabled, String token) {
        this.pushEnabled = pushEnabled;
        this.token = token;

        if (token != null && !token.isEmpty()) {
            String tokenDecoded = decodeJwt();
            this.jwt = Json.fromJson(tokenDecoded, Jwt.class);
        } else {
            this.jwt = null;
        }
    }

    public boolean isPushEnabled() { return pushEnabled; }

    public String getToken() { return token; }

    public String getChannels() {
        List<String> channelsList = new ArrayList<>();
        JsonObject jsonObject = Json.fromJson(jwt.getCapability(), JsonObject.class);
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        entries.forEach(e -> channelsList.add(e.getKey()));

        return addPrefixControlChannels(String.join(",", channelsList));
    }

    public long getExpiration() {
        return jwt.getExpiration() - jwt.getIssueAt() - PUSH_SECONDS_BEFORE_EXPIRATION;
    }

    private String decodeJwt() {
        String[] splitToken  = this.token.split("\\.");
        String encodedString = splitToken[1];
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);

        return new String(decodedBytes);
    }

    private String addPrefixControlChannels(String channels) {
        return channels
                .replace("control_pri", "[?occupancy=metrics.publishers]control_pri")
                .replace("control_sec", "[?occupancy=metrics.publishers]control_sec");
    }
}