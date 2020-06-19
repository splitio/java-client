package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;
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
    public void notifyIncomingNotification (IncomingNotification incomingNotification) {
        this.listeners.forEach(listener -> listener.onIncomingNotificationAdded(incomingNotification));
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
                IncomingNotification incomingNotification = _notificationParser.parse(type, payload);

                if (incomingNotification != null) {
                    this.notifyIncomingNotification(incomingNotification);
                }
            }
        } catch (Exception e) {
            _log.debug(String.format("Error parsing the event: %s", e.getMessage()));
        }
    }
}
