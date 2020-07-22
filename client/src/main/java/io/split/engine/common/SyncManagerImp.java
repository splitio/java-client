package io.split.engine.common;

import com.google.gson.Gson;
import io.split.client.SplitClientConfig;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.sse.AuthApiClient;
import io.split.engine.sse.AuthApiClientImp;
import io.split.engine.sse.EventSourceClient;
import io.split.engine.sse.EventSourceClientImp;
import io.split.engine.sse.NotificationManagerKeeper;
import io.split.engine.sse.NotificationManagerKeeperImp;
import io.split.engine.sse.NotificationParser;
import io.split.engine.sse.NotificationParserImp;
import io.split.engine.sse.NotificationProcessor;
import io.split.engine.sse.NotificationProcessorImp;
import io.split.engine.sse.SSEHandler;
import io.split.engine.sse.SSEHandlerImp;
import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.SplitsWorkerImp;
import io.split.engine.sse.workers.Worker;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImp implements SyncManager {
    private static final Logger _log = LoggerFactory.getLogger(SyncManager.class);

    private final AtomicBoolean _streamingEnabledConfig;
    private final Synchronizer _synchronizer;
    private final PushManager _pushManager;
    private final SSEHandler _sseHandler;

    public SyncManagerImp(boolean streamingEnabledConfig,
                          Synchronizer synchronizer,
                          PushManager pushManager,
                          SSEHandler sseHandler) {
        _streamingEnabledConfig = new AtomicBoolean(streamingEnabledConfig);
        _synchronizer = checkNotNull(synchronizer);
        _pushManager = checkNotNull(pushManager);
        _sseHandler = checkNotNull(sseHandler);
    }

    public static SyncManagerImp create(RefreshableSplitFetcherProvider splitFetcherProvider, RefreshableSegmentFetcher segmentFetcher, SplitClientConfig config, CloseableHttpClient httpclient) {
        Gson gson = new Gson();
        NotificationParser notificationParser = new NotificationParserImp(gson);
        SplitsWorker splitsWorker = new SplitsWorkerImp(splitFetcherProvider.getFetcher());
        Worker<SegmentQueueDto> segmentWorker = new SegmentsWorkerImp(segmentFetcher);
        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        NotificationProcessor notificationProcessor = new NotificationProcessorImp(splitsWorker, segmentWorker, notificationManagerKeeper);
        EventSourceClient eventSourceClient = new EventSourceClientImp(notificationParser);
        SSEHandler sseHandler = new SSEHandlerImp(eventSourceClient, config.streamingServiceURL(), splitsWorker, notificationProcessor, segmentWorker);
        AuthApiClient authApiClient = new AuthApiClientImp(config.authServiceURL(), gson, httpclient);
        PushManager pushManager = new PushManagerImp(authApiClient, sseHandler, config.authRetryBackoffBase());
        Synchronizer synchronizer = new SynchronizerImp(splitFetcherProvider, segmentFetcher);
        SyncManagerImp syncManager = new SyncManagerImp(config.streamingEnabled(), synchronizer, pushManager, sseHandler);
        eventSourceClient.registerFeedbackListener(syncManager);

        return syncManager;
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
        _synchronizer.stopPeriodicFetching();
        _pushManager.stop();
    }

    @Override
    public void onStreamingAvailable() {
        _synchronizer.stopPeriodicFetching();
        _synchronizer.syncAll();
        _sseHandler.startWorkers();
    }

    @Override
    public void onStreamingDisabled() {
        _sseHandler.stop();
        _synchronizer.startPeriodicFetching();
    }

    @Override
    public void onStreamingShutdown() {
        _pushManager.stop();
        _sseHandler.stopWorkers();
    }

    private void startStreamingMode() {
        _log.debug("Starting in streaming mode ...");
        _synchronizer.syncAll();
        _pushManager.start();
    }

    private void startPollingMode() {
        _log.debug("Starting in polling mode ...");
        _synchronizer.startPeriodicFetching();
    }

    @Override
    public void onErrorNotification(ErrorNotification errorNotification) {
        _pushManager.stop();
        startStreamingMode();
    }

    @Override
    public void onConnected() {
        _synchronizer.stopPeriodicFetching();
        _synchronizer.syncAll();
        _sseHandler.startWorkers();
    }

    @Override
    public void onDisconnect() {
        _synchronizer.startPeriodicFetching();
        _sseHandler.stopWorkers();
    }
}
