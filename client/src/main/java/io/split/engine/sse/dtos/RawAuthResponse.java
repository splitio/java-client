package io.split.engine.sse.dtos;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public class RawAuthResponse {
    private final static Integer PUSH_SECONDS_BEFORE_EXPIRATION = 600;
    private final static Gson gson = new Gson();

    private final boolean pushEnabled;
    private final String token;
    private Jwt jwt;

    public RawAuthResponse(boolean pushEnabled, String token) {
        this.pushEnabled = pushEnabled;
        this.token = token;
    }

    public boolean isPushEnabled() { return pushEnabled; }

    public String getToken() { return token; }

    public Jwt getJwt() {
        if (this.jwt == null) {
            String tokenDecoded = decodeJwt();
            this.jwt = gson.fromJson(tokenDecoded, Jwt.class);
        }

        return this.jwt;
    }

    public String getChannels() {
        List<String> channelsList = new ArrayList<>();
        JsonObject jsonObject = gson.fromJson(getJwt().getCapability(), JsonObject.class);
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        entries.forEach(e -> channelsList.add(e.getKey()));

        return addPrefixControlChannels(String.join(",", channelsList));
    }

    public double getExpiration() {
        return getJwt().getExpiration() - getJwt().getIssueAt() - PUSH_SECONDS_BEFORE_EXPIRATION;
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
