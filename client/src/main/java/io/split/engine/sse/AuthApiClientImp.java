package io.split.engine.sse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.split.client.interceptors.AddSplitHeadersFilter;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.dtos.Jwt;
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
    private static final Gson _gson = new Gson();

    private final CloseableHttpClient _client;
    private final String _target;


    public AuthApiClientImp(String apiToken,
                            String url) {
        HttpClientBuilder httpClientbuilder = HttpClients.custom()
                .addInterceptorLast(AddSplitHeadersFilter.instance(apiToken, false));

        _client = httpClientbuilder.build();
        _target = url;
    }

    @Override
    public AuthenticationResponse Authenticate() {
        try {
            URI uri = new URIBuilder(_target).build();
            HttpGet request = new HttpGet(uri);

            CloseableHttpResponse response = _client.execute(request);
            Integer statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                _log.debug(String.format("Success connection to: %s"), _target);

                String jsonContent = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return getSuccessResponse(jsonContent);
            } else if (statusCode >= 400 && statusCode < 500) {
                _log.debug(String.format("Problem to connect to : %s. Response status: %s", _target, statusCode));

                return new AuthenticationResponse(false, false);
            }

            return new AuthenticationResponse(false, true);
        } catch (Exception ex) {
            _log.error(ex.getMessage());

            return new AuthenticationResponse(false, false);
        }
    }

    private AuthenticationResponse getSuccessResponse(String jsonContent) {
        AuthenticationResponse authenticationResponse = _gson.fromJson(jsonContent, AuthenticationResponse.class);

        if (authenticationResponse.isPushEnabled()) {
            String tokenDecoded = decodeJwt(authenticationResponse.getToken());
            Jwt token = _gson.fromJson(tokenDecoded, Jwt.class);

            authenticationResponse.setChannels(getChannels(token));
            authenticationResponse.setExpiration(getExpiration(token));
        }

        authenticationResponse.setRetry(false);

        return authenticationResponse;
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
}
