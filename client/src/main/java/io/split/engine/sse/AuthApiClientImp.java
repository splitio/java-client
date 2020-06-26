package io.split.engine.sse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.dtos.RawAuthResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class AuthApiClientImp implements AuthApiClient {
    private static final Logger _log = LoggerFactory.getLogger(AuthApiClient.class);

    private final Gson _gson;
    private final CloseableHttpClient _httpClient;
    private final String _target;

    public AuthApiClientImp(String url,
                            Gson gson,
                            CloseableHttpClient httpClient) {
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

            if (statusCode == HttpStatus.SC_OK) {
                _log.debug(String.format("Success connection to: %s", _target));

                String jsonContent = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return getSuccessResponse(jsonContent);
            } else if (statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR) {
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
        JsonObject jsonObject = _gson.fromJson(jsonContent, JsonObject.class);
        String token = jsonObject.get("token") != null ? jsonObject.get("token").getAsString() : "";
        RawAuthResponse response = new RawAuthResponse(jsonObject.get("pushEnabled").getAsBoolean(), token, _gson);
        String channels = "";
        double expiration = 0;

        if (response.isPushEnabled()) {
            channels = response.getChannels(_gson);
            expiration = response.getExpiration(_gson);
        }

        return new AuthenticationResponse(response.isPushEnabled(), response.getToken(), channels, expiration, false);
    }

    private AuthenticationResponse buildErrorResponse(boolean retry) {
        return new AuthenticationResponse(false, null, null, 0, retry);
    }
}
