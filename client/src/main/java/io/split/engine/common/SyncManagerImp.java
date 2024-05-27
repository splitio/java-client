package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.ApiKeyCounter;
import io.split.client.SplitClientConfig;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitParser;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.StreamEventsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.telemetry.synchronizer.TelemetrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.client.utils.SplitExecutorFactory.buildExecutorService;

public class SyncManagerImp implements SyncManager {
    private static final Logger _log = LoggerFactory.getLogger(SyncManagerImp.class);

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
    private final SegmentSynchronizationTask _segmentSynchronizationTaskImp;
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private static final long STARTING_SYNC_ALL_BACKOFF_MAX_WAIT_MS = 10000; // 10 seconds max wait
    private  final SplitAPI _splitAPI;

    @VisibleForTesting
    /* package private */ SyncManagerImp(SplitTasks splitTasks,
                                         boolean streamingEnabledConfig,
                                         Synchronizer synchronizer,
                                         PushManager pushManager,
                                         LinkedBlockingQueue<PushManager.Status> pushMessages,
                                         SDKReadinessGates gates,
                                         TelemetryRuntimeProducer telemetryRuntimeProducer,
                                         TelemetrySynchronizer telemetrySynchronizer,
                                         SplitClientConfig config,
                                         SplitAPI splitAPI) {
        _streamingEnabledConfig = new AtomicBoolean(streamingEnabledConfig);
        _synchronizer = checkNotNull(synchronizer);
        _pushManager = checkNotNull(pushManager);
        _shuttedDown = new AtomicBoolean(false);
        _incomingPushStatus = pushMessages;
        _pushMonitorExecutorService = buildExecutorService(config.getThreadFactory(), "SPLIT-PushStatusMonitor-%d");
        _initializationtExecutorService = buildExecutorService(config.getThreadFactory(), "SPLIT-Initialization-%d");
        _backoff = new Backoff(config.authRetryBackoffBase());
        _gates = checkNotNull(gates);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _telemetrySynchronizer = checkNotNull(telemetrySynchronizer);
        _config = checkNotNull(config);
        _startingSyncCallBackoffBaseMs = config.startingSyncCallBackoffBaseMs();
        _segmentSynchronizationTaskImp = checkNotNull(splitTasks.getSegmentSynchronizationTask());
        _splitSynchronizationTask = checkNotNull(splitTasks.getSplitSynchronizationTask());
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
                                       SplitParser splitParser,
                                       FlagSetsFilter flagSetsFilter) {
        LinkedBlockingQueue<PushManager.Status> pushMessages = new LinkedBlockingQueue<>();
        Synchronizer synchronizer = new SynchronizerImp(splitTasks,
                                        splitFetcher,
                                        splitCacheProducer,
                                        segmentCacheProducer,
                                        config.streamingRetryDelay(),
                                        config.streamingFetchMaxRetries(),
                                        config.failedAttemptsBeforeLogging(),
                                        config.getSetsFilter());

        PushManager pushManager = PushManagerImp.build(synchronizer,
                                                        config.streamingServiceURL(),
                                                        config.authServiceURL(),
                                                        splitAPI,
                                                        pushMessages,
                                                        telemetryRuntimeProducer,
                                                        config.getThreadFactory(),
                                                        splitParser,
                                                        splitCacheProducer,
                                                        flagSetsFilter);

        return new SyncManagerImp(splitTasks,
                                  config.streamingEnabled(),
                                  synchronizer,
                                  pushManager,
                                  pushMessages,
                                  gates, 
                                  telemetryRuntimeProducer,
                                  telemetrySynchronizer, 
                                  config,
                                  splitAPI);
    }

    @Override
    public void start() {
        _initializationtExecutorService.submit(() -> {
            Backoff startBackoff = new Backoff(_startingSyncCallBackoffBaseMs, STARTING_SYNC_ALL_BACKOFF_MAX_WAIT_MS);
            while(!_synchronizer.syncAll()) {
                try{
                    long howLong = startBackoff.interval();
                    Thread.currentThread().sleep(howLong);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (_shuttedDown.get()) {
                return;
            }
            if (_log.isDebugEnabled()) {
                _log.debug("SyncAll Ready");
            }
            _gates.sdkInternalReady();
            if (_streamingEnabledConfig.get()) {
                startStreamingMode();
            } else {
                startPollingMode();
            }
            _synchronizer.startPeriodicDataRecording();
            _telemetrySynchronizer.synchronizeConfig(_config, System.currentTimeMillis(), ApiKeyCounter.getApiKeyCounterInstance().
                    getFactoryInstances(), new ArrayList<>());
        });
    }

    @Override
    public void shutdown() throws IOException {
        _log.info("Shutting down SyncManagerImp");
        if(_shuttedDown.get()) {
            return;
        }
        _shuttedDown.set(true);
        _initializationtExecutorService.shutdownNow();
        _synchronizer.stopPeriodicFetching();
        if (_streamingEnabledConfig.get()) {
            _pushManager.stop();
            _pushMonitorExecutorService.shutdownNow();
        }
        _segmentSynchronizationTaskImp.close();
        _log.info("Successful shutdown of segment fetchers");
        _splitSynchronizationTask.close();
        _log.info("Successful shutdown of splits");
        _synchronizer.stopPeriodicDataRecording();
        _splitAPI.close();
    }

    private void startStreamingMode() {
        _log.debug("Starting in streaming mode ...");
        if (null == _pushStatusMonitorTask) {
                _pushStatusMonitorTask = _pushMonitorExecutorService.submit(this::incomingPushStatusHandler);
        }
        _pushManager.start();
        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SYNC_MODE_UPDATE.getType(),
                StreamEventsEnum.SyncModeUpdateValues.STREAMING_EVENT.getValue(), System.currentTimeMillis()));
    }

    private void startPollingMode() {
        _log.debug("Starting in polling mode ...");
        _synchronizer.startPeriodicFetching();
        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.SYNC_MODE_UPDATE.getType(),
                StreamEventsEnum.SyncModeUpdateValues.POLLING_EVENT.getValue(), System.currentTimeMillis()));
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
                        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.STREAMING_STATUS.getType(),
                                StreamEventsEnum.StreamingStatusValues.STREAMING_ENABLED.getValue(), System.currentTimeMillis()));
                        _log.info("Streaming up and running.");
                        break;
                    case STREAMING_DOWN:
                        _log.info("Streaming service temporarily unavailable, working in polling mode.");
                        _pushManager.stopWorkers();
                        _synchronizer.startPeriodicFetching();
                        break;
                    case STREAMING_BACKOFF:
                        long howLong = _backoff.interval();
                        _log.info(String.format("Retryable error in streaming subsystem. Switching to polling and retrying in %d seconds", howLong));
                        _synchronizer.startPeriodicFetching();
                        _pushManager.stop();
                        Thread.sleep(howLong * 1000);
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