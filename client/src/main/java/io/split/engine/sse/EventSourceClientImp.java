package io.split.engine.sse;

import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.exceptions.EventParsingException;
import io.split.engine.sse.listeners.FeedbackLoopListener;
import io.split.engine.sse.listeners.NotificationsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventSourceClientImp implements EventSourceClient {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);
    private final Client _client;
    private final NotificationParser _notificationParser;
    private final List<FeedbackLoopListener> _feedbackListeners;
    private final List<NotificationsListener> _notificationsListeners;

    private SseEventSource _sseEventSource;

    public EventSourceClientImp(NotificationParser notificationParser) {
        _notificationParser = checkNotNull(notificationParser);
        _feedbackListeners = new ArrayList<>();
        _notificationsListeners = new ArrayList<>();

        _client = ClientBuilder
                .newBuilder()
                .readTimeout(70, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void start(String url) {
        try {
            if (_sseEventSource != null && _sseEventSource.isOpen()) { stop(); }

            _sseEventSource = SseEventSource
                    .target(_client.target(url))
                    .reconnectingEvery(1, TimeUnit.SECONDS)
                    .build();
            _sseEventSource.register(this::onMessage, this::onError);
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
        if (_sseEventSource == null) {
            notifyDisconnect();
            return;
        }

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

    public synchronized void notifyMessageNotification (IncomingNotification incomingNotification) {
        _notificationsListeners.forEach(listener -> listener.onMessageNotificationReceived(incomingNotification));
    }

    public synchronized void notifyErrorNotification (ErrorNotification errorNotification) {
        _feedbackListeners.forEach(listener -> listener.onErrorNotification(errorNotification));
    }

    public synchronized void notifyConnected () {
        _feedbackListeners.forEach(listener -> listener.onConnected());
    }

    public synchronized void notifyDisconnect () {
        _feedbackListeners.forEach(listener -> listener.onDisconnect());
    }

    private void onMessage(InboundSseEvent event) {
        try {
            String type = event.getName();
            String payload = event.readData();

            if (payload.length() > 0) {
                _log.debug(String.format("Payload received: %s", payload));
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

    private void onError(Throwable error) {
        _log.error(String.format("EventSourceClient onError: ", error.getMessage()));
        notifyDisconnect();
    }
}
