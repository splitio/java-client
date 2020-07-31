package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.engine.sse.AuthApiClient;
import io.split.engine.sse.AuthApiClientImp;
import io.split.engine.sse.SSEHandler;
import io.split.engine.sse.dtos.AuthenticationResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class PushManagerImp implements PushManager, Runnable {
    private static final Logger _log = LoggerFactory.getLogger(PushManager.class);

    private final AuthApiClient _authApiClient;
    private final SSEHandler _sseHandler;
    private final int _authRetryBackOffBase;
    private Future<?> _nextTokenRefreshTask;

    private ScheduledExecutorService _scheduledExecutorService;

    @VisibleForTesting
    /* package private */ PushManagerImp(AuthApiClient authApiClient,
                                         SSEHandler sseHandler,
                                         int authRetryBackOffBase) {
        _authApiClient = checkNotNull(authApiClient);
        _sseHandler = checkNotNull(sseHandler);
        _authRetryBackOffBase = checkNotNull(authRetryBackOffBase);

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SSERefreshToken-%d")
                .build();
        _scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);

    }

    public static PushManagerImp build(String authUrl, CloseableHttpClient httpClient, SSEHandler sseHandler, int authRetryBackOffBase) {
        return new PushManagerImp(new AuthApiClientImp(authUrl, httpClient), sseHandler, authRetryBackOffBase);
    }

    @Override
    public void start() {
        AuthenticationResponse response = _authApiClient.Authenticate();
        _log.debug(String.format("Auth service response pushEnabled: %s", response.isPushEnabled()));

        if (response.isPushEnabled()) {
            _sseHandler.start(response.getToken(), response.getChannels());
            scheduleConnectionReset(response.getExpiration());
        } else {
            stop();
        }

        if (response.isRetry()) {
            // TODO: update this after backoffService implementation.
            scheduleConnectionReset(_authRetryBackOffBase);
        }
    }

    @Override
    public void stop() {
        _sseHandler.stop();
        if (_nextTokenRefreshTask != null) {
            _nextTokenRefreshTask.cancel(false);
        }
    }

    @Override
    public void run() {
        _log.debug("Starting refresh token ...");
        _sseHandler.stop();
        start();
    }

    private void scheduleConnectionReset(long time) {
        _log.debug(String.format("scheduleNextTokenRefresh in %s SECONDS", time));
        _nextTokenRefreshTask = _scheduledExecutorService.schedule(() -> {
            stop();
            start();
        }, time, TimeUnit.SECONDS);
    }
}
