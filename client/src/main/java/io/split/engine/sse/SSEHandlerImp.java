package io.split.engine.sse;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.experiments.RefreshableSplitFetcherProvider;
import io.split.engine.segments.RefreshableSegmentFetcher;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.listeners.FeedbackLoopListener;
import io.split.engine.sse.listeners.NotificationsListener;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.SplitsWorkerImp;
import io.split.engine.sse.workers.Worker;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class SSEHandlerImp implements SSEHandler, NotificationsListener {
    private static final Logger _log = LoggerFactory.getLogger(SSEHandler.class);

    private final EventSourceClient _eventSourceClient;
    private final SplitsWorker _splitsWorker;
    private final Worker<SegmentQueueDto> _segmentWorker;
    private final NotificationProcessor _notificationProcessor;
    private final String _streamingServiceUrl;

    @VisibleForTesting
    /* package private */ SSEHandlerImp(EventSourceClient eventSourceClient,
                                        String streamingServiceUrl,
                                        SplitsWorker splitsWorker,
                                        NotificationProcessor notificationProcessor,
                                        Worker<SegmentQueueDto> segmentWorker) {
        _eventSourceClient = checkNotNull(eventSourceClient);
        _streamingServiceUrl = checkNotNull(streamingServiceUrl);
        _splitsWorker = checkNotNull(splitsWorker);
        _notificationProcessor = checkNotNull(notificationProcessor);
        _segmentWorker = checkNotNull(segmentWorker);

        _eventSourceClient.registerNotificationListener(this);
    }

    public static SSEHandlerImp build(String streamingServiceUrl, RefreshableSplitFetcherProvider splitFetcherProvider, RefreshableSegmentFetcher segmentFetcher) {
        SplitsWorker splitsWorker = SplitsWorkerImp.build(splitFetcherProvider.getFetcher());
        Worker<SegmentQueueDto> segmentWorker = SegmentsWorkerImp.build(segmentFetcher);
        NotificationProcessor notificationProcessor = NotificationProcessorImp.build(splitsWorker, segmentWorker);

        return new SSEHandlerImp(EventSourceClientImp.build(), streamingServiceUrl, splitsWorker, notificationProcessor, segmentWorker);
    }

    @Override
    public void start(String token, String channels) {
        try {
            _log.debug("SSE Handel starting ...");

            URIBuilder uri = new URIBuilder(_streamingServiceUrl);
            uri.addParameter("channels", channels);
            uri.addParameter("v", "1.1");
            uri.addParameter("accessToken", token);

            _eventSourceClient.setUrl(uri.toString());
            _eventSourceClient.start();
        } catch (Exception ex) {
            _log.error("Exception in SSE Handler start: %s", ex.getMessage());
        }
    }

    @Override
    public void stop() {
        _eventSourceClient.stop();
        stopWorkers();
    }

    @Override
    public void startWorkers() {
        _splitsWorker.start();
        _segmentWorker.start();
    }

    @Override
    public void stopWorkers() {
        _splitsWorker.stop();
        _segmentWorker.stop();
    }

    @Override
    public synchronized void registerFeedbackListener(FeedbackLoopListener listener) {
        _eventSourceClient.registerFeedbackListener(listener);
    }

    @Override
    public void onMessageNotificationReceived(IncomingNotification incomingNotification) {
        _log.debug(String.format("Incoming notification received: %s", incomingNotification));
        _notificationProcessor.process(incomingNotification);
    }
}
