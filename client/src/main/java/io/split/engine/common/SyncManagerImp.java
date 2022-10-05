package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.client.ApiKeyCounter;
import io.split.client.SplitClientConfig;
import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.StreamEventsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private final SegmentSynchronizationTask _segmentSynchronizationTaskImp;
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final UniqueKeysTracker _uniqueKeysTracker;
    private static final long STARTING_SYNC_ALL_BACKOFF_MAX_WAIT_MS = new Long(10000); // 10 seconds max wait
    private  final SplitAPI _splitAPI;

    @VisibleForTesting
    /* package private */ SyncManagerImp(SplitTasks splitTasks,
                                         boolean streamingEnabledConfig,
                                         Synchronizer synchronizer,
                                         PushManager pushManager,
                                         LinkedBlockingQueue<PushManager.Status> pushMessages,
                                         SDKReadinessGates gates, TelemetryRuntimeProducer telemetryRuntimeProducer,
                                         TelemetrySynchronizer telemetrySynchronizer,
                                         SplitClientConfig config,
                                         UniqueKeysTracker uniqueKeysTracker,
                                         SplitAPI splitAPI) {
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
        _backoff = new Backoff(config.authRetryBackoffBase());
        _gates = checkNotNull(gates);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _telemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        _config = checkNotNull(config);
        _startingSyncCallBackoffBaseMs = config.startingSyncCallBackoffBaseMs();
        _impressionManager = checkNotNull(splitTasks.getImpressionManager());
        _eventsTask = checkNotNull(splitTasks.getEventsTask());
        _telemetrySyncTask = checkNotNull(splitTasks.getTelemetrySyncTask());
        _segmentSynchronizationTaskImp = checkNotNull(splitTasks.getSegmentSynchronizationTask());
        _splitSynchronizationTask = checkNotNull(splitTasks.getSplitSynchronizationTask());
        _uniqueKeysTracker = uniqueKeysTracker;
        _splitAPI = splitAPI;
    }

    public static SyncManagerImp build(SplitTasks splitTasks,
                                       SplitFetcher splitFetcher,
                                       SplitCacheProducer splitCacheProducer,
                                       SplitAPI splitAPI,
                                       SegmentCacheProducer segmentCacheProducer,
                                       SDKReadinessGates gates,
                                       TelemetryRuntimeProducer telemetryRuntimeProducer,
                                       TelemetrySynchronizer telemetrySynchronizer,
                                       SplitClientConfig config,
                                       UniqueKeysTracker uniqueKeysTracker) {
        LinkedBlockingQueue<PushManager.Status> pushMessages = new LinkedBlockingQueue<>();
        Synchronizer synchronizer = new SynchronizerImp(splitTasks.getSplitSynchronizationTask(),
                                        splitFetcher,
                                        splitTasks.getSegmentSynchronizationTask(),
                                        splitCacheProducer,
                                        segmentCacheProducer,
                                        config.streamingRetryDelay(),
                                        config.streamingFetchMaxRetries(),
                                        config.failedAttemptsBeforeLogging(),
                                        config.cdnDebugLogging(),
                                        gates);

        PushManager pushManager = PushManagerImp.build(synchronizer,
                                                        config.streamingServiceURL(),
                                                        config.authServiceURL(),
                                                        splitAPI,
                                                        pushMessages,
                                                        telemetryRuntimeProducer);

        return new SyncManagerImp(splitTasks,
                                  config.streamingEnabled(),
                                  synchronizer,
                                  pushManager,
                                  pushMessages,
                                  gates, 
                                  telemetryRuntimeProducer,
                                  telemetrySynchronizer, 
                                  config,
                                  uniqueKeysTracker,
                                  splitAPI);
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

            try {
                _impressionManager.start();
            } catch (Exception e) {
                _log.error("Error trying to init Impression Manager synchronizer task.", e);
            }
            if (_uniqueKeysTracker != null){
                try {
                    _uniqueKeysTracker.start();
                } catch (Exception e) {
                    _log.error("Error trying to init Unique Keys Tracker synchronizer task.", e);
                }
            }
            try {
                _eventsTask.start();
            } catch (Exception e) {
                _log.error("Error trying to init Events synchronizer task.", e);
            }

            if (_streamingEnabledConfig.get()) {
                startStreamingMode();
            } else {
                startPollingMode();
            }
            _telemetrySynchronizer.synchronizeConfig(_config, System.currentTimeMillis(), ApiKeyCounter.getApiKeyCounterInstance().getFactoryInstances(), new ArrayList<>());

            try {
                _telemetrySyncTask.startScheduledTask();
            } catch (Exception e) {
                _log.error("Error trying to Telemetry synchronizer task.", e);
            }
        });
    }

    @Override
    public void shutdown(long splitCount, long segmentCount, long segmentKeyCount) throws IOException {
        if(_shuttedDown.get()) {
            return;
        }
        _shuttedDown.set(true);
        _initializationtExecutorService.shutdownNow();
        _synchronizer.stopPeriodicFetching();
        _pushManager.stop();
        _pushMonitorExecutorService.shutdownNow();
        _impressionManager.close();
        _log.info("Successful shutdown of impressions manager");
        if (_uniqueKeysTracker != null){
            _uniqueKeysTracker.stop();
            _log.info("Successful stop of UniqueKeysTracker");
        }
        _eventsTask.close();
        _log.info("Successful shutdown of eventsTask");
        _segmentSynchronizationTaskImp.close();
        _log.info("Successful shutdown of segment fetchers");
        _splitSynchronizationTask.close();
        _log.info("Successful shutdown of splits");
        _telemetrySyncTask.stopScheduledTask(splitCount, segmentCount, segmentKeyCount);
        _log.info("Successful shutdown of telemetry sync task");
        _splitAPI.close();
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
