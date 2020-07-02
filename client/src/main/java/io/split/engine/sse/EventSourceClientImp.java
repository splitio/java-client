package io.split.engine.sse;

import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.exceptions.EventParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.ArrayList;
import java.util.List;

public class EventSourceClientImp implements EventSourceClient {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);
    private final Client _client;
    private final NotificationParser _notificationParser;
    private final List<FeedbackLoopListener> _feedbackListeners;
    private final List<NotificationsListener> _notificationsListeners;

    private SseEventSource _sseEventSource;

    public EventSourceClientImp(NotificationParser notificationParser) {
        _notificationParser = notificationParser;
        _client = ClientBuilder.newBuilder().build();
        _feedbackListeners = new ArrayList<>();
        _notificationsListeners = new ArrayList<>();
    }

    @Override
    public void start(String url) {
        try {
            if (_sseEventSource != null && _sseEventSource.isOpen()) { stop(); }

            _sseEventSource = SseEventSource
                    .target(_client.target(url))
                    .build();
            _sseEventSource.register(this::onMessage);
            _sseEventSource.open();

            _log.info(String.format("Connected and reading from: %s", url));

            notifyConnected();
        } catch (Exception e) {
            _log.error(String.format("Error connecting or reading from %s : %s", url, e.getMessage()));
            notifyDisconnect();
        }
    }

    @Override
    public void stop() {
        if (!_sseEventSource.isOpen()) {
            _log.error("Event Source Client is closed.");
            return;
        }

        _sseEventSource.close();
        notifyDisconnect();
    }

    @Override
    public synchronized void registerNotificationListener(NotificationsListener listener) {
        _notificationsListeners.add(listener);
    }

    @Override
    public synchronized void registerFeedbackListener(FeedbackLoopListener listener) {
        _feedbackListeners.add(listener);
    }

    @Override
    public synchronized void notifyMessageNotification (IncomingNotification incomingNotification) {
        _notificationsListeners.forEach(listener -> listener.onMessageNotificationReceived(incomingNotification));
    }

    @Override
    public synchronized void notifyErrorNotification (ErrorNotification errorNotification) {
        _feedbackListeners.forEach(listener -> listener.onErrorNotificationAdded(errorNotification));
    }

    @Override
    public synchronized void notifyConnected () {
        _feedbackListeners.forEach(listener -> listener.onConnected());
    }

    @Override
    public synchronized void notifyDisconnect () {
        _feedbackListeners.forEach(listener -> listener.onDisconnect());
    }

    private void onMessage(InboundSseEvent event) {
        try {
            String type = event.getName();
            String payload = event.readData();

            if (payload.length() > 0) {
                switch (type) {
                    case "message":
                        IncomingNotification incomingNotification = _notificationParser.parseMessage(payload);
                        notifyMessageNotification(incomingNotification);
                        break;
                    case "error":
                        ErrorNotification errorNotification = _notificationParser.parseError(payload);
                        notifyErrorNotification(errorNotification);
                        break;
                    default:
                        throw new EventParsingException("Wrong notification type.", payload);
                }
            }
        } catch (EventParsingException ex){
            _log.debug(String.format("Error parsing the event: %s. Payload: %s", ex.getMessage(), ex.getPayload()));
        } catch (Exception e) {
            _log.error(String.format("Error onMessage: %s", e.getMessage()));
        }
    }
}
