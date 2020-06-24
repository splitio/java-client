package io.split.engine.sse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.split.client.interceptors.AddSplitHeadersFilter;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.dtos.Jwt;
import io.split.engine.sse.dtos.RawAuthResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AuthApiClientImp implements AuthApiClient {
    private final static Integer PUSH_SECONDS_BEFORE_EXPIRATION = 600;
    private final static String OCCUPANCY_PREFIX = "[?occupancy=metrics.publishers]";
    private final static String CONTROL_PRI = "control_pri";
    private final static String CONTROL_SEC = "control_sec";

    private static final Logger _log = LoggerFactory.getLogger(AuthApiClient.class);

    private final Gson _gson;
    private final CloseableHttpClient _httpClient;
    private final String _target;

    public AuthApiClientImp(String apiToken,
                            String url,
                            Gson gson,
                            CloseableHttpClient httpClient) {
        //HttpClientBuilder httpClientBuilder = HttpClients.custom().addInterceptorLast(AddSplitHeadersFilter.instance(apiToken, false));
        _httpClient = httpClient;
        _target = url;
        _gson = gson;
    }

    @Override
    public AuthenticationResponse Authenticate() {
        try {
            URI uri = new URIBuilder(_target).build();
            HttpGet request = new HttpGet(uri);

            CloseableHttpResponse response = _httpClient.execute(request);
            Integer statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                _log.debug(String.format("Success connection to: %s", _target));

                String jsonContent = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return getSuccessResponse(jsonContent);
            } else if (statusCode >= 400 && statusCode < 500) {
                _log.debug(String.format("Problem to connect to : %s. Response status: %s", _target, statusCode));

                return buildErrorResponse(false);
            }

            return buildErrorResponse(true);
        } catch (Exception ex) {
            _log.error(ex.getMessage());

            return buildErrorResponse(false);
        }
    }

    private AuthenticationResponse getSuccessResponse(String jsonContent) {
        RawAuthResponse response = _gson.fromJson(jsonContent, RawAuthResponse.class);
        String channels = "";
        double expiration = 0;

        if (response.isPushEnabled()) {
            String tokenDecoded = decodeJwt(response.getToken());
            Jwt token = _gson.fromJson(tokenDecoded, Jwt.class);

            channels = getChannels(token);
            expiration = getExpiration(token);
        }

        return buildSuccessResponse(response.isPushEnabled(), response.getToken(), channels, expiration, false);
    }

    private String getChannels(Jwt token) {
        List<String> channelsList = new ArrayList<>();
        JsonObject jsonObject = _gson.fromJson(token.getCapability(), JsonObject.class);
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        entries.forEach(e -> channelsList.add(e.getKey()));

        return addPrefixControlChannels(String.join(",", channelsList));
    }

    private double getExpiration(Jwt token) {
        return token.getExpiration() - token.getIssueAt() - PUSH_SECONDS_BEFORE_EXPIRATION;
    }

    private String addPrefixControlChannels(String channels) {
        return channels
                .replace(CONTROL_PRI, String.format("%s%s", OCCUPANCY_PREFIX, CONTROL_PRI))
                .replace(CONTROL_SEC, String.format("%s%s", OCCUPANCY_PREFIX, CONTROL_SEC));
    }

    private String decodeJwt(String token) {
        String[] splitToken  = token.split("\\.");
        String encodedString = splitToken[1];
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);

        return new String(decodedBytes);
    }

    private AuthenticationResponse buildSuccessResponse(boolean pushEnabled, String token, String channels, double expiration, boolean retry) {
        return new AuthenticationResponse(pushEnabled, token, channels, expiration, retry);
    }

    private AuthenticationResponse buildErrorResponse(boolean retry) {
        return new AuthenticationResponse(false, null, null, 0, retry);
    }
}
