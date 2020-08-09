package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.segments.RefreshableSegmentFetcher;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Future<?> _pushStatusMonitorTask;

    @VisibleForTesting
    /* package private */ SyncManagerImp(boolean streamingEnabledConfig,
                                         Synchronizer synchronizer,
                                         PushManager pushManager,
                                         LinkedBlockingQueue<PushManager.Status> pushMessages) {
        _streamingEnabledConfig = new AtomicBoolean(streamingEnabledConfig);
        _synchronizer = checkNotNull(synchronizer);
        _pushManager = checkNotNull(pushManager);
        _shutdown = new AtomicBoolean(false);
        _incomingPushStatus = pushMessages;
        _executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("SPLIT-PushStatusMonitor-%d")
                .setDaemon(true)
                .build());
    }

    public static SyncManagerImp build(boolean streamingEnabledConfig,
                                        RefreshableSplitFetcherProvider refreshableSplitFetcherProvider,
                                        RefreshableSegmentFetcher segmentFetcher,
                                        String authUrl,
                                        CloseableHttpClient httpClient,
                                        String streamingServiceUrl,
                                        int authRetryBackOffBase) {

        LinkedBlockingQueue<PushManager.Status> pushMessages = new LinkedBlockingQueue<>();
        Synchronizer synchronizer = new SynchronizerImp(refreshableSplitFetcherProvider, segmentFetcher);
        PushManager pushManager = PushManagerImp.build(synchronizer, streamingServiceUrl, authUrl, httpClient, authRetryBackOffBase, pushMessages);
        return new SyncManagerImp(streamingEnabledConfig, synchronizer, pushManager, pushMessages);
    }

    @Override
    public void start() {
        if (_streamingEnabledConfig.get()) {
            startStreamingMode();
        } else {
            startPollingMode();
        }
    }

    @Override
    public void shutdown() {
        _shutdown.set(true);
        _synchronizer.stopPeriodicFetching();
        _pushManager.stop();
    }

    private void startStreamingMode() {
        _log.debug("Starting in streaming mode ...");
        _synchronizer.syncAll();
        if (null == _pushStatusMonitorTask) {
            _pushStatusMonitorTask = _executorService.submit(this::incomingPushStatusHandler);
        }
        _pushManager.start();

    }

    private void startPollingMode() {
        _log.debug("Starting in polling mode ...");
        _synchronizer.startPeriodicFetching();
    }

    void incomingPushStatusHandler() {
        while (!Thread.interrupted()) {
            try {
                PushManager.Status status = _incomingPushStatus.take();
                switch (status) {
                    case STREAMING_ENABLED:
                        _synchronizer.stopPeriodicFetching();
                        // FALLTHROUGH
                    case STREAMING_READY:
                        _synchronizer.syncAll();
                        _pushManager.startWorkers();
                        break;
                    case STREAMING_PAUSED:
                        _pushManager.stopWorkers();
                        _synchronizer.startPeriodicFetching();
                        break;
                    case RETRYABLE_ERROR:
                        _synchronizer.startPeriodicFetching();
                        _pushManager.stop();
                        _pushManager.start();
                        break;
                    case STREAMING_DISABLED:
                    case NONRETRYABLE_ERROR:
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
