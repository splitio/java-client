package io.split.engine.sse;

import com.google.gson.JsonObject;
import io.split.client.utils.Json;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.dtos.RawAuthResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;

public class AuthApiClientImp implements AuthApiClient {
    private static final Logger _log = LoggerFactory.getLogger(AuthApiClient.class);

    private final CloseableHttpClient _httpClient;
    private final String _target;

    public AuthApiClientImp(String url,
                                           CloseableHttpClient httpClient) {
        _httpClient = checkNotNull(httpClient);
        _target = checkNotNull(url);
    }

    @Override
    public AuthenticationResponse Authenticate() {
        try {
            URI uri = new URIBuilder(_target).build();
            HttpGet request = new HttpGet(uri);

            CloseableHttpResponse response = _httpClient.execute(request);
            Integer statusCode = response.getCode();

            if (statusCode == HttpStatus.SC_OK) {
                _log.debug(String.format("Success connection to: %s", _target));

                String jsonContent = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return getSuccessResponse(jsonContent);
            }

            _log.warn(String.format("Problem to connect to : %s. Response status: %s", _target, statusCode));
            if (statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                return new AuthenticationResponse(false,false);
            }

            return new AuthenticationResponse(false,true);
        } catch (Exception ex) {
            _log.error(ex.getMessage());
            return new AuthenticationResponse(false,true);
        }
    }

    private AuthenticationResponse getSuccessResponse(String jsonContent) {
        JsonObject jsonObject = Json.fromJson(jsonContent, JsonObject.class);
        String token = jsonObject.get("token") != null ? jsonObject.get("token").getAsString() : "";
        RawAuthResponse response = new RawAuthResponse(jsonObject.get("pushEnabled").getAsBoolean(), token);
        String channels = "";
        long expiration = 0;

        if (response.isPushEnabled()) {
            channels = response.getChannels();
            expiration = response.getExpiration();
        }

        return new AuthenticationResponse(response.isPushEnabled(), response.getToken(), channels, expiration, false);
    }
}
