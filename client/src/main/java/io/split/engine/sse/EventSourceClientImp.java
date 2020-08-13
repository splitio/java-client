package io.split.engine.sse;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.exceptions.EventParsingException;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.Worker;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventSourceClientImp implements EventSourceClient {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);

    private final String _baseStreamingUrl;
    private final Client _client;
    private final NotificationParser _notificationParser;
    private final NotificationProcessor _notificationProcessor;
    private final SplitSseEventSource _splitSseEventSource;
    private final PushStatusTracker _pushStatusTracker;

    @VisibleForTesting
    /* package private */ EventSourceClientImp(String baseStreamingUrl,
                                               NotificationParser notificationParser,
                                               NotificationProcessor notificationProcessor,
                                               Client client,
                                               PushStatusTracker pushStatusTracker) {
        _baseStreamingUrl = checkNotNull(baseStreamingUrl);
        _notificationParser = checkNotNull(notificationParser);
        _notificationProcessor = checkNotNull(notificationProcessor);
        _client = checkNotNull(client);
        _splitSseEventSource = new SplitSseEventSource(
                inboundEvent -> { onMessage(inboundEvent); return null; },
                status -> { onSSeStatusChange(status); return null; });
        _pushStatusTracker = pushStatusTracker;
    }

    public static EventSourceClientImp build(String baseStreamingUrl,
                                             SplitsWorker splitsWorker,
                                             Worker<SegmentQueueDto> segmentWorker,
                                             PushStatusTracker pushStatusTracker) {

        return new EventSourceClientImp(baseStreamingUrl,
                new NotificationParserImp(),
                NotificationProcessorImp.build(splitsWorker, segmentWorker, pushStatusTracker),
                ClientBuilder.newBuilder().readTimeout(70, TimeUnit.SECONDS).build(),
                pushStatusTracker);
    }

    @Override
    public boolean start(String channelList, String token) {
        if (_splitSseEventSource.isOpen()) {
            _splitSseEventSource.close();
        }

        try {
            return _splitSseEventSource.open(buildTarget(channelList, token));
        } catch (URISyntaxException e) {
            _log.error("Error building Streaming URI: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void stop() {
        if (!_splitSseEventSource.isOpen()) {
            _log.warn("Event Source Client is closed.");
            return;
        }
        _splitSseEventSource.close();
    }

    private WebTarget buildTarget(String channelList, String token) throws URISyntaxException {
        return _client.target(new URIBuilder(_baseStreamingUrl)
                .addParameter("channels", channelList)
                .addParameter("v", "1.1")
                .addParameter("accessToken", token)
                .build());
    }

    private void onMessage(InboundSseEvent event) {
        try {
            String type = event.getName();
            String payload = event.readData();
            if (payload.length() > 0) {
                _log.debug(String.format("Payload received: %s", payload));
                switch (type) {
                    case "message":
                        _notificationProcessor.process(_notificationParser.parseMessage(payload));
                        break;
                    case "error":
                        _pushStatusTracker.handleIncomingAblyError(_notificationParser.parseError(payload));
                        break;
                    default:
                        throw new EventParsingException("Wrong notification type.", payload);
                }
            }
        } catch (EventParsingException ex) {
            _log.debug(String.format("Error parsing the event: %s. Payload: %s", ex.getMessage(), ex.getPayload()));
        } catch (Exception e) {
            _log.warn(String.format("Error onMessage: %s", e.getMessage()));
        }
    }

    private void onSSeStatusChange(SseStatus status) {
        _pushStatusTracker.handleSseStatus(status);
    }
}