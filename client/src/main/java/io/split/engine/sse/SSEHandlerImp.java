package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.listeners.NotificationsListener;
import io.split.engine.sse.workers.SplitsWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSEHandlerImp implements SSEHandler, NotificationsListener {
    private static final Logger _log = LoggerFactory.getLogger(SSEHandler.class);

    private final EventSourceClient _eventSourceClient;
    private final SplitsWorker _splitsWorker;
    private final NotificationProcessor _notificationProcessor;
    private final String _streamingServiceUrl;

    public SSEHandlerImp(EventSourceClient eventSourceClient,
                         String streamingServiceUrl,
                         SplitsWorker splitsWorker,
                         NotificationProcessor notificationProcessor) {
        _eventSourceClient = eventSourceClient;
        _streamingServiceUrl = streamingServiceUrl;
        _splitsWorker = splitsWorker;
        _notificationProcessor = notificationProcessor;

        _eventSourceClient.registerNotificationListener(this);
    }

    @Override
    public void start(String token, String channels) {
        try {
            _log.debug("SSE Handel starting ...");

            String url = String.format("%s?channels=%s&v=1.1&accessToken=%s", _streamingServiceUrl, channels, token);

            _eventSourceClient.start(url);
        }catch (Exception ex) {
            _log.error("Exception in SSE Handler start: %s", ex.getMessage());
        }
    }

    @Override
    public void stop() {
        _eventSourceClient.stop();
    }

    @Override
    public void startWorkers() {
        _splitsWorker.start();
    }

    @Override
    public void stropWorkers() {
        _splitsWorker.stop();
    }

    @Override
    public void onMessageNotificationReceived(IncomingNotification incomingNotification) {
        _notificationProcessor.process(incomingNotification);
    }
}
