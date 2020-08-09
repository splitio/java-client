package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.engine.sse.AuthApiClient;
import io.split.engine.sse.AuthApiClientImp;
import io.split.engine.sse.EventSourceClient;
import io.split.engine.sse.EventSourceClientImp;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.SplitsWorkerImp;
import io.split.engine.sse.workers.Worker;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class PushManagerImp implements PushManager {
    private static final Logger _log = LoggerFactory.getLogger(PushManager.class);

    private final AuthApiClient _authApiClient;
    private final EventSourceClient _eventSourceClient;
    private final Backoff _backoff;
    private final SplitsWorker _splitsWorker;
    private final Worker<SegmentQueueDto> _segmentWorker;
    private final LinkedBlockingQueue<PushManager.Status> _statusMessages;

    private Future<?> _nextTokenRefreshTask;
    private final ScheduledExecutorService _scheduledExecutorService;

    @VisibleForTesting
        /* package private */ PushManagerImp(AuthApiClient authApiClient,
                                             EventSourceClient eventSourceClient,
                                             SplitsWorker splitsWorker,
                                             Worker<SegmentQueueDto> segmentWorker,
                                             Backoff backoff,
                                             LinkedBlockingQueue<PushManager.Status> statusMessages) {

        _authApiClient = checkNotNull(authApiClient);
        _eventSourceClient = checkNotNull(eventSourceClient);
        _backoff = checkNotNull(backoff);
        _splitsWorker = splitsWorker;
        _segmentWorker = segmentWorker;
        _statusMessages = statusMessages;
        _scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SSERefreshToken-%d")
                .build());
    }

    public static PushManagerImp build(Synchronizer synchronizer,
                                       String streamingUrl,
                                       String authUrl,
                                       CloseableHttpClient httpClient,
                                       int authRetryBackOffBase,
                                       LinkedBlockingQueue<PushManager.Status> statusMessages) {

        SplitsWorker splitsWorker = new SplitsWorkerImp(synchronizer);
        Worker<SegmentQueueDto> segmentWorker = new SegmentsWorkerImp(synchronizer);

        return new PushManagerImp(new AuthApiClientImp(authUrl, httpClient),
                EventSourceClientImp.build(streamingUrl, splitsWorker, segmentWorker, statusMessages),
                splitsWorker,
                segmentWorker,
                new Backoff(authRetryBackOffBase),
                statusMessages);
    }

    @Override
    public synchronized void start() {
        AuthenticationResponse response = _authApiClient.Authenticate();
        _log.debug(String.format("Auth service response pushEnabled: %s", response.isPushEnabled()));
        if (response.isPushEnabled() && startSse(response.getToken(), response.getChannels())) {
            scheduleConnectionReset(response.getExpiration());
            _backoff.reset();
            return;
        }

        stop();
        if (response.isRetry()) {
            scheduleConnectionReset(_backoff.interval());
        } else {
            _statusMessages.offer(Status.STREAMING_DISABLED);
        }
    }

    @Override
    public synchronized void stop() {
        _eventSourceClient.stop();
        stopWorkers();
        if (_nextTokenRefreshTask != null) {
            _nextTokenRefreshTask.cancel(false);
        }
    }

    private void scheduleConnectionReset(long time) {
        _log.debug(String.format("scheduleNextTokenRefresh in %s SECONDS", time));
        _nextTokenRefreshTask = _scheduledExecutorService.schedule(() -> {
            _log.debug("Starting scheduleNextTokenRefresh ...");
            stop();
            start();
        }, time, TimeUnit.SECONDS);
    }

    private boolean startSse(String token, String channels) {
        try {
            _log.debug("SSE Handler starting ...");
            return _eventSourceClient.start(channels, token);
        } catch (Exception e) {
            _log.error("Exception in SSE Handler start: " + e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized void startWorkers() {
        _splitsWorker.start();
        _segmentWorker.start();
    }

    @Override
    public synchronized void stopWorkers() {
        _splitsWorker.stop();
        _segmentWorker.stop();
    }
}