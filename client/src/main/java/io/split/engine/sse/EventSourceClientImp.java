package io.split.engine.sse;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.sse.client.RawEvent;
import io.split.engine.sse.client.SSEClient;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.exceptions.EventParsingException;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.Worker;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventSourceClientImp implements EventSourceClient {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";

    private final String _baseStreamingUrl;
    private final NotificationParser _notificationParser;
    private final NotificationProcessor _notificationProcessor;
    private final SSEClient _sseClient;
    private final PushStatusTracker _pushStatusTracker;
    private final AtomicBoolean _firstEvent;

    @VisibleForTesting
    /* package private */ EventSourceClientImp(String baseStreamingUrl,
                                               NotificationParser notificationParser,
                                               NotificationProcessor notificationProcessor,
                                               PushStatusTracker pushStatusTracker,
                                               CloseableHttpClient sseHttpClient,
                                               TelemetryRuntimeProducer telemetryRuntimeProducer,
                                               ThreadFactory threadFactory) {
        _baseStreamingUrl = checkNotNull(baseStreamingUrl);
        _notificationParser = checkNotNull(notificationParser);
        _notificationProcessor = checkNotNull(notificationProcessor);
        _pushStatusTracker = pushStatusTracker;

        _sseClient = new SSEClient(
                inboundEvent -> { onMessage(inboundEvent); return null; },
                status -> { _pushStatusTracker.handleSseStatus(status); return null; },
                sseHttpClient,
                telemetryRuntimeProducer,
                threadFactory);
        _firstEvent = new AtomicBoolean();
    }

    public static EventSourceClientImp build(String baseStreamingUrl,
                                             SplitsWorker splitsWorker,
                                             Worker<SegmentQueueDto> segmentWorker,
                                             PushStatusTracker pushStatusTracker,
                                             CloseableHttpClient sseHttpClient,
                                             TelemetryRuntimeProducer telemetryRuntimeProducer,
                                             ThreadFactory threadFactory) {
        return new EventSourceClientImp(baseStreamingUrl,
                new NotificationParserImp(),
                NotificationProcessorImp.build(splitsWorker, segmentWorker, pushStatusTracker),
                pushStatusTracker,
                sseHttpClient,
                telemetryRuntimeProducer,
                threadFactory);
    }

    @Override
    public boolean start(String channelList, String token) {
        if (_sseClient.isOpen()) {
            _sseClient.close();
        }

        try {
            _firstEvent.set(false);
            return _sseClient.open(buildUri(channelList, token));
        } catch (URISyntaxException e) {
            _log.error("Error building Streaming URI: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void stop() {
        _log.info("Stopping EventSourceClientImp");
        if (!_sseClient.isOpen()) {
            _log.info("Event Source Client is closed.");
            return;
        }
        _sseClient.close();
    }

    private URI buildUri(String channelList, String token) throws URISyntaxException {
        return new URIBuilder(_baseStreamingUrl)
                .addParameter("channels", channelList)
                .addParameter("v", "1.1")
                .addParameter("accessToken", token)
                .build();
    }

    private void onMessage(RawEvent event) {
        try {
            String type = event.event();
            String payload = event.data();
            if(_firstEvent.compareAndSet(false, true) && !ERROR.equals(type)){
                _pushStatusTracker.handleSseStatus(SSEClient.StatusMessage.FIRST_EVENT);
            }
            if (payload != null && !payload.isEmpty()) {
                _log.debug(String.format("Payload received: %s", payload));
                switch (type) {
                    case MESSAGE:
                        _notificationProcessor.process(_notificationParser.parseMessage(payload));
                        break;
                    case ERROR:
                        _pushStatusTracker.handleIncomingAblyError(_notificationParser.parseError(payload));
                        break;
                    default:
                        throw new EventParsingException("Wrong notification type.", payload);
                }
            }
        } catch (EventParsingException ex) {
            _log.debug(String.format("Error parsing the event: %s. Payload: %s", ex.getMessage(), ex.getPayload()));
        } catch (Exception e) {
            _log.debug(String.format("Error parsing the event id: %s. OnMessage: %s", event.id(), e.getMessage()), e);
        }
    }
}