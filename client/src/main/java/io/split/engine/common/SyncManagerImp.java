package io.split.engine.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTaskMauro;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
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
                                       SplitSynchronizationTask splitSynchronizationTask,
                                       SplitFetcherImp splitFetcher,
                                       SegmentSynchronizationTaskMauro segmentSynchronizationTaskMauro,
                                       SplitCache splitCache,
                                       String authUrl,
                                       CloseableHttpClient httpClient,
                                       String streamingServiceUrl,
                                       int authRetryBackOffBase,
                                       CloseableHttpClient sseHttpClient,
                                       SegmentCache segmentCache) {
        LinkedBlockingQueue<PushManager.Status> pushMessages = new LinkedBlockingQueue<>();
        Synchronizer synchronizer = new SynchronizerImp(splitSynchronizationTask, splitFetcher, segmentSynchronizationTaskMauro, splitCache, segmentCache);
        PushManager pushManager = PushManagerImp.build(synchronizer, streamingServiceUrl, authUrl, httpClient, authRetryBackOffBase, pushMessages, sseHttpClient);
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
                        break;
                    case STREAMING_DOWN:
                        _pushManager.stopWorkers();
                        _synchronizer.startPeriodicFetching();
                        break;
                    case STREAMING_BACKOFF:
                        _synchronizer.startPeriodicFetching();
                        _pushManager.stopWorkers();
                        _pushManager.start();
                        break;
                    case STREAMING_OFF:
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
