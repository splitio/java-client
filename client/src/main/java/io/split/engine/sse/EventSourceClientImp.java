package io.split.engine.sse;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.common.PushManager;
import io.split.engine.sse.dtos.ErrorNotification;
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
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventSourceClientImp implements EventSourceClient {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);

    private final String _baseStreamingUrl;
    private final Client _client;
    private final NotificationParser _notificationParser;
    private final NotificationProcessor _notificationProcessor;
    private final SplitSseEventSource _splitSseEventSource;
    private final LinkedBlockingQueue<PushManager.Status> _statusMessages;

    @VisibleForTesting
    /* package private */ EventSourceClientImp(String baseStreamingUrl,
                                               NotificationParser notificationParser,
                                               NotificationProcessor notificationProcessor,
                                               Client client,
                                               LinkedBlockingQueue<PushManager.Status> statusMessages) {
        _baseStreamingUrl = checkNotNull(baseStreamingUrl);
        _notificationParser = checkNotNull(notificationParser);
        _notificationProcessor = checkNotNull(notificationProcessor);
        _client = checkNotNull(client);
        _splitSseEventSource = new SplitSseEventSource(inboundEvent -> { onMessage(inboundEvent); return null; }, this::handleSseStatus);
        _statusMessages = statusMessages;
    }

    public static EventSourceClientImp build(String baseStreamingUrl,
                                             SplitsWorker splitsWorker,
                                             Worker<SegmentQueueDto> segmentWorker,
                                             LinkedBlockingQueue<PushManager.Status> statusMessages) {

        return new EventSourceClientImp(baseStreamingUrl,
                new NotificationParserImp(),
                NotificationProcessorImp.build(splitsWorker, segmentWorker, new NotificationManagerKeeperImp(statusMessages)),
                ClientBuilder.newBuilder().readTimeout(70, TimeUnit.SECONDS).build(),
                statusMessages);
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
                        ErrorNotification errorNotification = _notificationParser.parseError(payload);
                        parseError(errorNotification).ifPresent(_statusMessages::offer);
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

    private Optional<PushManager.Status> parseError(ErrorNotification notification) {
        if (notification.getCode() >= 40140 && notification.getCode() <= 40149) {
            return Optional.of(PushManager.Status.RETRYABLE_ERROR);
        }
        if (notification.getCode() >= 40000 && notification.getCode() <= 49999) {
            return Optional.of(PushManager.Status.NONRETRYABLE_ERROR);
        }
        return Optional.empty();
    }

    private Void handleSseStatus(SseStatus status) {
        switch(status) {
            case CONNECTED: _statusMessages.offer(PushManager.Status.STREAMING_READY); break;
            case RETRYABLE_ERROR: _statusMessages.offer(PushManager.Status.RETRYABLE_ERROR); break;
            case NONRETRYABLE_ERROR: _statusMessages.offer(PushManager.Status.NONRETRYABLE_ERROR); break;
            case DISCONNECTED: /* nothing to do here. */ break;
        }
        return null;
    }
}
