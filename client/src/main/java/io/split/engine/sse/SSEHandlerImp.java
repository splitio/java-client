package io.split.engine.sse;

import io.split.engine.sse.workers.SplitsWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSEHandlerImp implements SSEHandler {
    private static final Logger _log = LoggerFactory.getLogger(SSEHandler.class);

    private final EventSourceClient _eventSourceClient;
    private final String _streamingServiceUrl;
    private final SplitsWorker _splitsWorker;
    private Thread _eventSourceClientThread;
    private Thread _splitsWorkerThread;

    public SSEHandlerImp(EventSourceClient eventSourceClient,
                         String streamingServiceUrl,
                         SplitsWorker splitsWorker) {
        _eventSourceClient = eventSourceClient;
        _streamingServiceUrl = streamingServiceUrl;
        _splitsWorker = splitsWorker;
    }

    @Override
    public void start(String token, String channels) {
        try {
            _log.debug("SSE Handel starting ...");

            String url = String.format("%s?channels=%s&v=1.1&accessToken=%s", _streamingServiceUrl, channels, token);

            _eventSourceClient.resetUrl(url);
            _eventSourceClientThread = new Thread(_eventSourceClient);
            _eventSourceClientThread.start();
        }catch (Exception ex) {
            _log.error("Exception in SSE Handler start: %s", ex.getMessage());
        }
    }

    @Override
    public void stop() {
        _eventSourceClient.stop();
        _eventSourceClientThread.interrupt();
    }

    @Override
    public void startWorkers() {
        _splitsWorkerThread = new Thread(_splitsWorker);
        _splitsWorkerThread.start();
    }

    @Override
    public void stropWorkers() {
        _splitsWorker.stop();
        _splitsWorkerThread.interrupt();
    }
}
