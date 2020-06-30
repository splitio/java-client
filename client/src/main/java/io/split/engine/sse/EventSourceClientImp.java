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

public class EventSourceClientImp implements EventSourceClient, Runnable {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);
    private String _url;
    private SseEventSource _sseEventSource;
    private Client _client;
    private NotificationParser _notificationParser;
    private List<FeedbackLoopListener> listeners = new ArrayList<>();

    public EventSourceClientImp(String url,
                                NotificationParser notificationParser) {
        resetUrl(url);
        _notificationParser = notificationParser;
        _client = ClientBuilder.newBuilder().build();
    }

    @Override
    public void run() {
        try {
            if (_sseEventSource != null && _sseEventSource.isOpen()) stop();

            _sseEventSource = SseEventSource
                    .target(_client.target(_url))
                    .build();
            _sseEventSource.register(this::onMessage);
            _sseEventSource.open();

            _log.info(String.format("Connected and reading from: %s", _url));
            notifyConnected();
        } catch (Exception e) {
            _log.error(String.format("Error connecting or reading from %s : %s", _url, e.getMessage()));
            notifyDisconnect();
        }
    }

    @Override
    public void resetUrl(String url) {
        _url = url;
    }

    @Override
    public void stop() {
        _sseEventSource.close();
    }

    @Override
    public void registerListener(FeedbackLoopListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void notifyMessageNotification (IncomingNotification incomingNotification) {
        this.listeners.forEach(listener -> listener.onMessageNotificationAdded(incomingNotification));
    }

    @Override
    public void notifyErrorNotification (ErrorNotification errorNotification) {
        this.listeners.forEach(listener -> listener.onErrorNotificationAdded(errorNotification));
    }

    @Override
    public void notifyConnected () {
        this.listeners.forEach(listener -> listener.onConnected());
    }

    @Override
    public void notifyDisconnect () {
        this.listeners.forEach(listener -> listener.onDisconnect());
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