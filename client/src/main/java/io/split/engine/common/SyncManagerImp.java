package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.ApiKeyCounter;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.StreamEventsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImp implements SyncManager {
    private static final Logger _log = LoggerFactory.getLogger(SyncManager.class);

    private final AtomicBoolean _streamingEnabledConfig;
    private final Synchronizer _synchronizer;
    private final PushManager _pushManager;
    private final AtomicBoolean _shutdown;
    private final LinkedBlockingQueue<PushManager.Status> _incomingPushStatus;
    private final ExecutorService _executorService;
    private final ExecutorService _startExecutorService;
    private final SDKReadinessGates _gates;
    private Future<?> _pushStatusMonitorTask;
    private Backoff _backoff;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final SplitClientConfig _config;

    @VisibleForTesting
    /* package private */ SyncManagerImp(boolean streamingEnabledConfig,
                                         Synchronizer synchronizer,
                                         PushManager pushManager,
                                         LinkedBlockingQueue<PushManager.Status> pushMessages,
                                         int authRetryBackOffBase,
                                         SDKReadinessGates gates, TelemetryRuntimeProducer telemetryRuntimeProducer,
                                         TelemetrySynchronizer telemetrySynchronizer,
                                         SplitClientConfig config) {
        _streamingEnabledConfig = new AtomicBoolean(streamingEnabledConfig);
        _synchronizer = checkNotNull(synchronizer);
        _pushManager = checkNotNull(pushManager);
        _shutdown = new AtomicBoolean(false);
        _incomingPushStatus = pushMessages;
        _executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("SPLIT-PushStatusMonitor-%d")
                .setDaemon(true)
                .build());
        _startExecutorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("SPLIT-PollingMode-%d")
                .setDaemon(true)
                .build());
        _backoff = new Backoff(authRetryBackOffBase);
        _gates = checkNotNull(gates);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _telemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        _config = checkNotNull(config);
    }

    public static SyncManagerImp build(boolean streamingEnabledConfig,
                                       SplitSynchronizationTask splitSynchronizationTask,
                                       SplitFetcher splitFetcher,
                                       SegmentSynchronizationTaskImp segmentSynchronizationTaskImp,
                                       SplitCacheProducer splitCacheProducer,
                                       String authUrl,
                                       CloseableHttpClient httpClient,
                                       String streamingServiceUrl,
                                       int authRetryBackOffBase,
                                       CloseableHttpClient sseHttpClient,
                                       SegmentCacheProducer segmentCacheProducer,
                                       int streamingRetryDelay,
                                       int maxOnDemandFetchRetries,
                                       int failedAttemptsBeforeLogging,
                                       boolean cdnDebugLogging,
                                       SDKReadinessGates gates,
                                       TelemetryRuntimeProducer telemetryRuntimeProducer,
                                       TelemetrySynchronizer telemetrySynchronizer,
                                       SplitClientConfig config) {
        LinkedBlockingQueue<PushManager.Status> pushMessages = new LinkedBlockingQueue<>();
        Synchronizer synchronizer = new SynchronizerImp(splitSynchronizationTask,
                                        splitFetcher,
                                        segmentSynchronizationTaskImp,
                                        splitCacheProducer,
                                        segmentCacheProducer,
                                        streamingRetryDelay,
                                        maxOnDemandFetchRetries,
                                        failedAttemptsBeforeLogging,
                                        cdnDebugLogging,
                                        gates);

        PushManager pushManager = PushManagerImp.build(synchronizer,
                                                        streamingServiceUrl,
                                                        authUrl,
                                                        httpClient,
                                                        pushMessages,
                                                        sseHttpClient,
                                                        telemetryRuntimeProducer);

        return new SyncManagerImp(streamingEnabledConfig,
                                  synchronizer,
                                  pushManager,
                                  pushMessages,
                                  authRetryBackOffBase, 
                                  gates, 
                                  telemetryRuntimeProducer,
                                  telemetrySynchronizer, 
                                  config);
    }

    @Override
    public void start() {
        _startExecutorService.submit(() -> {
            while(!_synchronizer.syncAll()) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    _log.warn("Sdk Initializer thread interrupted");
                    Thread.currentThread().interrupt();
                }
            }
            _gates.sdkInternalReady();
            _telemetrySynchronizer.synchronizeConfig(_config, System.currentTimeMillis(), ApiKeyCounter.getApiKeyCounterInstance().getFactoryInstances(), new ArrayList<>());
            if (_streamingEnabledConfig.get()) {
                startStreamingMode();
            } else {
                startPollingMode();
            }
        });
    }

    @Override
    public void shutdown() {
        _shutdown.set(true);
        _synchronizer.stopPeriodicFetching();
        _pushManager.stop();
    }

    private void startStreamingMode() {
        _log.debug("Starting in streaming mode ...");
        if (null == _pushStatusMonitorTask) {
            _pushStatusMonitorTask = _executorService.submit(this::incomingPushStatusHandler);
        }
        _pushManager.start();
        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SYNC_MODE_UPDATE.getType(), StreamEventsEnum.SyncModeUpdateValues.STREAMING_EVENT.getValue(), System.currentTimeMillis()));
    }

    private void startPollingMode() {
        _log.debug("Starting in polling mode ...");
        _synchronizer.startPeriodicFetching();
        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SYNC_MODE_UPDATE.getType(), StreamEventsEnum.SyncModeUpdateValues.POLLING_EVENT.getValue(), System.currentTimeMillis()));
    }

    @VisibleForTesting
    /* package private */ void incomingPushStatusHandler() {
        while (!Thread.interrupted()) {
            try {
                PushManager.Status status = _incomingPushStatus.take();
                _log.debug(String.format("Streaming status received: %s", status.toString()));
                switch (status) {
                    case STREAMING_READY:
                        _synchronizer.stopPeriodicFetching();
                        _synchronizer.syncAll();
                        _pushManager.startWorkers();
                        _pushManager.scheduleConnectionReset();
                        _backoff.reset();
                        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.STREAMING_STATUS.getType(), StreamEventsEnum.StreamingStatusValues.STREAMING_ENABLED.getValue(), System.currentTimeMillis()));
                        _log.info("Streaming up and running.");
                        break;
                    case STREAMING_DOWN:
                        _log.info("Streaming service temporarily unavailable, working in polling mode.");
                        _pushManager.stopWorkers();
                        _synchronizer.startPeriodicFetching();
                        break;
                    case STREAMING_BACKOFF:
                        long howLong = _backoff.interval() * 1000;
                        _log.info(String.format("Retryable error in streaming subsystem. Switching to polling and retrying in %d seconds", howLong/1000));
                        _synchronizer.startPeriodicFetching();
                        _pushManager.stopWorkers();
                        _pushManager.stop();
                        Thread.sleep(howLong);
                        _incomingPushStatus.clear();
                        _pushManager.start();
                        break;
                    case STREAMING_OFF:
                        _log.info("Unrecoverable error in streaming subsystem. SDK will work in polling-mode and will not retry an SSE connection.");
                        _pushManager.stop();
                        _synchronizer.startPeriodicFetching();
                        if (null != _pushStatusMonitorTask) {
                            _pushStatusMonitorTask.cancel(false);
                        }
                        return; // Stop this task for the rest of the SDK lifetime
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
