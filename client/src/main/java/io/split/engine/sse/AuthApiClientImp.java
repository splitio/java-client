package io.split.engine.sse;

import com.google.gson.JsonObject;
import io.split.client.utils.Json;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.dtos.RawAuthResponse;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
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
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public AuthApiClientImp(String url, CloseableHttpClient httpClient, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _httpClient = checkNotNull(httpClient);
        _target = checkNotNull(url);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public AuthenticationResponse Authenticate() {
        try {
            long initTime = System.currentTimeMillis();
            URI uri = new URIBuilder(_target).build();
            HttpGet request = new HttpGet(uri);

            CloseableHttpResponse response = _httpClient.execute(request);
            Integer statusCode = response.getCode();

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("[%s] %s. Status code: %s", request.getMethod(), uri.toURL(), statusCode));
            }

            if (statusCode == HttpStatus.SC_OK) {
                _log.debug(String.format("Success connection to: %s", _target));

                String jsonContent = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                _telemetryRuntimeProducer.recordTokenRefreshes();
                _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.TOKEN, System.currentTimeMillis());
                _telemetryRuntimeProducer.recordSyncLatency(HTTPLatenciesEnum.TOKEN, System.currentTimeMillis()-initTime);
                return getSuccessResponse(jsonContent);
            }

            _log.error(String.format("Problem to connect to : %s. Response status: %s", _target, statusCode));
            if (statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    _telemetryRuntimeProducer.recordAuthRejections();
                }
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
