package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.engine.experiments.SplitParser;
import io.split.engine.sse.AuthApiClient;
import io.split.engine.sse.AuthApiClientImp;
import io.split.engine.sse.EventSourceClient;
import io.split.engine.sse.EventSourceClientImp;
import io.split.engine.sse.PushStatusTracker;
import io.split.engine.sse.PushStatusTrackerImp;
import io.split.engine.sse.client.SSEClient;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.FeatureFlagsWorker;
import io.split.engine.sse.workers.FeatureFlagWorkerImp;
import io.split.engine.sse.workers.Worker;

import io.split.storages.SplitCacheProducer;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.StreamEventsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.client.utils.SplitExecutorFactory.buildSingleThreadScheduledExecutor;

public class PushManagerImp implements PushManager {
    private static final Logger _log = LoggerFactory.getLogger(PushManager.class);

    private final AuthApiClient _authApiClient;
    private final EventSourceClient _eventSourceClient;
    private final FeatureFlagsWorker _featureFlagsWorker;
    private final Worker<SegmentQueueDto> _segmentWorker;
    private final PushStatusTracker _pushStatusTracker;

    private Future<?> _nextTokenRefreshTask;
    private final ScheduledExecutorService _scheduledExecutorService;
    private AtomicLong _expirationTime;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    @VisibleForTesting
    /* package private */ PushManagerImp(AuthApiClient authApiClient,
                                         EventSourceClient eventSourceClient,
                                         FeatureFlagsWorker featureFlagsWorker,
                                         Worker<SegmentQueueDto> segmentWorker,
                                         PushStatusTracker pushStatusTracker,
                                         TelemetryRuntimeProducer telemetryRuntimeProducer,
                                         ThreadFactory threadFactory) {

        _authApiClient = checkNotNull(authApiClient);
        _eventSourceClient = checkNotNull(eventSourceClient);
        _featureFlagsWorker = featureFlagsWorker;
        _segmentWorker = segmentWorker;
        _pushStatusTracker = pushStatusTracker;
        _expirationTime = new AtomicLong();
        _scheduledExecutorService = buildSingleThreadScheduledExecutor(threadFactory, "Split-SSERefreshToken-%d");
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    public static PushManagerImp build(Synchronizer synchronizer,
                                       String streamingUrl,
                                       String authUrl,
                                       SplitAPI splitAPI,
                                       LinkedBlockingQueue<PushManager.Status> statusMessages,
                                       TelemetryRuntimeProducer telemetryRuntimeProducer,
                                       ThreadFactory threadFactory,
                                       SplitParser splitParser,
                                       SplitCacheProducer splitCacheProducer,
                                       FlagSetsFilter flagSetsFilter) {
        FeatureFlagsWorker featureFlagsWorker = new FeatureFlagWorkerImp(synchronizer, splitParser, splitCacheProducer,
                telemetryRuntimeProducer, flagSetsFilter);
        Worker<SegmentQueueDto> segmentWorker = new SegmentsWorkerImp(synchronizer);
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(statusMessages, telemetryRuntimeProducer);
        return new PushManagerImp(new AuthApiClientImp(authUrl, splitAPI.getHttpClient(), telemetryRuntimeProducer),
                EventSourceClientImp.build(streamingUrl, featureFlagsWorker, segmentWorker, pushStatusTracker, splitAPI.getSseHttpClient(),
                        telemetryRuntimeProducer, threadFactory, splitAPI.getRequestDecorator()),
                featureFlagsWorker,
                segmentWorker,
                pushStatusTracker,
                telemetryRuntimeProducer,
                threadFactory);
    }

    @Override
    public synchronized void start() {
        AuthenticationResponse response = _authApiClient.Authenticate();
        _log.debug(String.format("Auth service response pushEnabled: %s", response.isPushEnabled()));
        if (response.isPushEnabled() && startSse(response.getToken(), response.getChannels())) {
            _expirationTime.set(response.getExpiration());
            _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.TOKEN_REFRESH.getType(),
                    response.getExpiration(), System.currentTimeMillis()));
            return;
        }

        cleanUpResources();
        if (response.isRetry()) {
            _pushStatusTracker.handleSseStatus(SSEClient.StatusMessage.RETRYABLE_ERROR);
        } else {
            _pushStatusTracker.forcePushDisable();
        }
    }

    @Override
    public synchronized void stop() {
        _log.debug("Stopping PushManagerImp");
        cleanUpResources();
    }

    @Override
    public void scheduleConnectionReset() {
        _log.debug(String.format("scheduleNextTokenRefresh in %s SECONDS", _expirationTime));
        _nextTokenRefreshTask = _scheduledExecutorService.schedule(() -> {
            _log.debug("Starting scheduleNextTokenRefresh ...");
            stop();
            start();
        }, _expirationTime.get(), TimeUnit.SECONDS);
    }

    private boolean startSse(String token, String channels) {
        try {
            _log.debug("SSE Handler starting ...");
            return _eventSourceClient.start(channels, token);
        } catch (Exception e) {
            _log.debug("Exception in SSE Handler start: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void startWorkers() {
        _featureFlagsWorker.start();
        _segmentWorker.start();
    }

    @Override
    public void stopWorkers() {
        _featureFlagsWorker.stop();
        _segmentWorker.stop();
    }

    private void cleanUpResources() {
        _eventSourceClient.stop();
        stopWorkers();
        if (_nextTokenRefreshTask != null) {
            _log.debug("Cancel nextTokenRefreshTask");
            _nextTokenRefreshTask.cancel(false);
        }
    }
}