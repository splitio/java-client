package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.ApiKeyCounter;
import io.split.client.SplitClientConfig;
import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManager;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.StreamEventsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
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
    private final AtomicBoolean _shuttedDown;
    private final LinkedBlockingQueue<PushManager.Status> _incomingPushStatus;
    private final ExecutorService _pushMonitorExecutorService;
    private final ExecutorService _initializationtExecutorService;
    private final SDKReadinessGates _gates;
    private Future<?> _pushStatusMonitorTask;
    private Backoff _backoff;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final TelemetrySynchronizer _telemetrySynchronizer;
    private final SplitClientConfig _config;
    private final long _startingSyncCallBackoffBaseMs;
    private final ImpressionsManager _impressionManager;
    private final EventsTask _eventsTask;
    private final TelemetrySyncTask _telemetrySyncTask;
    private static final long STARTING_SYNC_ALL_BACKOFF_MAX_WAIT_MS = new Long(10000); // 10 seconds max wait

    @VisibleForTesting
    /* package private */ SyncManagerImp(boolean streamingEnabledConfig,
                                         Synchronizer synchronizer,
                                         PushManager pushManager,
                                         LinkedBlockingQueue<PushManager.Status> pushMessages,
                                         int authRetryBackOffBase,
                                         SDKReadinessGates gates, TelemetryRuntimeProducer telemetryRuntimeProducer,
                                         TelemetrySynchronizer telemetrySynchronizer,
                                         SplitClientConfig config, ImpressionsManager impressionsManager,
                                         EventsTask eventsTask, TelemetrySyncTask telemetrySyncTask) {
        _streamingEnabledConfig = new AtomicBoolean(streamingEnabledConfig);
        _synchronizer = checkNotNull(synchronizer);
        _pushManager = checkNotNull(pushManager);
        _shuttedDown = new AtomicBoolean(false);
        _incomingPushStatus = pushMessages;
        _pushMonitorExecutorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("SPLIT-PushStatusMonitor-%d")
                .setDaemon(true)
                .build());
        _initializationtExecutorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("SPLIT-Initialization-%d")
                .setDaemon(true)
                .build());
        _backoff = new Backoff(authRetryBackOffBase);
        _gates = checkNotNull(gates);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _telemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        _config = checkNotNull(config);
        _startingSyncCallBackoffBaseMs = config.startingSyncCallBackoffBaseMs();
        _impressionManager = impressionsManager;
        _eventsTask = eventsTask;
        _telemetrySyncTask = telemetrySyncTask;
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
                                       SplitClientConfig config,
                                       ImpressionsManager impressionsManager,
                                       EventsTask eventsTask,
                                       TelemetrySyncTask telemetrySyncTask) {
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
                                  config,
                                  impressionsManager,
                                  eventsTask,
                                  telemetrySyncTask);
    }

    @Override
    public void start() {
        _initializationtExecutorService.submit(() -> {
            _backoff = new Backoff(_startingSyncCallBackoffBaseMs, STARTING_SYNC_ALL_BACKOFF_MAX_WAIT_MS);
            while(!_synchronizer.syncAll()) {
                try{
                    long howLong = _backoff.interval();
                    Thread.currentThread().sleep(howLong);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (_shuttedDown.get()) {
                return;
            }
            _gates.sdkInternalReady();
            _telemetrySynchronizer.synchronizeConfig(_config, System.currentTimeMillis(), ApiKeyCounter.getApiKeyCounterInstance().getFactoryInstances(), new ArrayList<>());
            if (_streamingEnabledConfig.get()) {
                startStreamingMode();
            } else {
                startPollingMode();
            }
            _impressionManager.start();
            try {
                _eventsTask.start();
            } catch (Exception e) {
                _log.error("Error trying to init EventTask synchronizer task.", e);
            }
            try {
                _telemetrySyncTask.startScheduledTask();
            } catch (Exception e) {
                _log.warn("Error trying to init telemetry stats synchronizer task.");
            }
        });
    }

    @Override
    public void shutdown() {
        if(_shuttedDown.get()) {
            return;
        }
        _shuttedDown.set(true);
        _initializationtExecutorService.shutdownNow();
        _synchronizer.stopPeriodicFetching();
        _pushManager.stop();
        _pushMonitorExecutorService.shutdownNow();
    }

    private void startStreamingMode() {
        _log.debug("Starting in streaming mode ...");
        if (null == _pushStatusMonitorTask) {
            _pushStatusMonitorTask = _pushMonitorExecutorService.submit(this::incomingPushStatusHandler);
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
