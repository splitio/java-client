package io.split.engine.sse;

import com.google.common.annotations.VisibleForTesting;
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
    private final SplitSseEventSource _splitSseEventSource;

    @VisibleForTesting
    /* package private */ EventSourceClientImp(NotificationParser notificationParser) {
        _notificationParser = checkNotNull(notificationParser);
        _feedbackListeners = new ArrayList<>();
        _notificationsListeners = new ArrayList<>();
        _client = ClientBuilder
                .newBuilder()
                .readTimeout(70, TimeUnit.SECONDS)
                .build();
        _splitSseEventSource = new SplitSseEventSource(
                inboundEvent -> { onMessage(inboundEvent); return null; },
                reconnect -> { onDisconnect(reconnect); return null; });
    }

    public static EventSourceClientImp build() {
        return new EventSourceClientImp(new NotificationParserImp());
    }

    @Override
    public void start(String url) {
        if (_splitSseEventSource != null && _splitSseEventSource.isOpen()) {
            _splitSseEventSource.close();
        }

        /*_splitSseEventSource = new SplitSseEventSource(_client.target(url),
                inboundEvent -> { onMessage(inboundEvent); return null; },
                reconnect -> { onDisconnect(reconnect); return null; });*/

        _splitSseEventSource.setTarget(_client.target(url));
        _splitSseEventSource.open();

        if(_splitSseEventSource.isOpen()) {
            _log.info(String.format("Connected and reading from: %s", url));
            notifyConnected();
        }
    }

    @Override
    public void stop() {
        if (_splitSseEventSource == null) {
            notifyDisconnect();
            return;
        }

        if (!_splitSseEventSource.isOpen()) {
            _log.warn("Event Source Client is closed.");
            return;
        }

        notifyDisconnect();
        _splitSseEventSource.close();
    }

    @Override
    public synchronized void registerNotificationListener(NotificationsListener listener) {
        _notificationsListeners.add(listener);
    }

    @Override
    public synchronized void registerFeedbackListener(FeedbackLoopListener listener) {
        _feedbackListeners.add(listener);
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
        } catch (EventParsingException ex) {
            _log.debug(String.format("Error parsing the event: %s. Payload: %s", ex.getMessage(), ex.getPayload()));
        } catch (Exception e) {
            _log.warn(String.format("Error onMessage: %s", e.getMessage()));
        }
    }

    private void onDisconnect(Boolean reconnect) {
        _log.warn(String.format("onDisconnect, reconnect: %s", reconnect));
        notifyDisconnect(reconnect);
    }

    private synchronized void notifyMessageNotification (IncomingNotification incomingNotification) {
        _notificationsListeners.forEach(listener -> listener.onMessageNotificationReceived(incomingNotification));
    }

    private synchronized void notifyErrorNotification (ErrorNotification errorNotification) {
        _feedbackListeners.forEach(listener -> listener.onErrorNotification(errorNotification));
    }

    private synchronized void notifyConnected () {
        _feedbackListeners.forEach(listener -> listener.onConnected());
    }

    private synchronized void notifyDisconnect (Boolean reconnect) {
        _feedbackListeners.forEach(listener -> listener.onDisconnect(reconnect));
    }

    private synchronized void notifyDisconnect () {
        _feedbackListeners.forEach(listener -> listener.onDisconnect(false));
    }
}
