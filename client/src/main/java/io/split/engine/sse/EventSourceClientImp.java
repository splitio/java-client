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
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventSourceClientImp implements EventSourceClient {
    private static final Logger _log = LoggerFactory.getLogger(EventSourceClient.class);

    private final Client _client;
    private final NotificationParser _notificationParser;
    private final List<FeedbackLoopListener> _feedbackListeners;
    private final List<NotificationsListener> _notificationsListeners;
    private final SplitSseEventSource _splitSseEventSource;
    private final LinkedBlockingQueue<StatusMessage> _incomingSSEStatus;
    private final ScheduledExecutorService _sseMonitorExecutor;
    private final AtomicBoolean _firstTime;

    @VisibleForTesting
    /* package private */ EventSourceClientImp(NotificationParser notificationParser) {
        _notificationParser = checkNotNull(notificationParser);

        _incomingSSEStatus = new LinkedBlockingQueue<>();
        _sseMonitorExecutor = Executors.newSingleThreadScheduledExecutor();
        _feedbackListeners = new ArrayList<>();
        _notificationsListeners = new ArrayList<>();
        _firstTime = new AtomicBoolean(true);
        _client = ClientBuilder
                .newBuilder()
                .readTimeout(70, TimeUnit.SECONDS)
                .build();
        _splitSseEventSource = new SplitSseEventSource(inboundEvent -> { onMessage(inboundEvent); return null; });

        // Start the SSE Monitor thread at construction time.
        _sseMonitorExecutor.execute(this::handleSSEStatusMessages);
    }

    //add parameter base backoff
    public static EventSourceClientImp build() {
        return new EventSourceClientImp(new NotificationParserImp());
    }

    @Override
    public boolean start(String url) {
        if (_splitSseEventSource != null && _splitSseEventSource.isOpen()) {
            _splitSseEventSource.close();
        }

        _splitSseEventSource.open(_client.target(url), _incomingSSEStatus);
        _splitSseEventSource.awaitFirstContact();
        _firstTime.set(false);

        return _splitSseEventSource.isOpen();
    }

    @Override
    public void stop() {
        if (_firstTime.get()) {
            _feedbackListeners.forEach(listener -> listener.onDisconnect(false));
            return;
        }

        if (!_splitSseEventSource.isOpen()) {
            _log.warn("Event Source Client is closed.");
            return;
        }

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


    private synchronized void notifyMessageNotification (IncomingNotification incomingNotification) {
        _notificationsListeners.forEach(listener -> listener.onMessageNotificationReceived(incomingNotification));
    }

    private synchronized void notifyErrorNotification (ErrorNotification errorNotification) {
        _feedbackListeners.forEach(listener -> listener.onErrorNotification(errorNotification));
    }

    private void handleSSEStatusMessages() {
        while(true) {
            try {
                StatusMessage message = _incomingSSEStatus.take();
                switch (message.code) {
                    case CONNECTED:
                        _log.info("Successfully connected to sse");
                        _feedbackListeners.forEach(FeedbackLoopListener::onConnected);
                        break;
                    case RETRYABLE_ERROR:
                        _feedbackListeners.forEach(listener -> listener.onDisconnect(true));
                        break;
                    case NONRETRYABLE_ERROR:
                    case DISCONNECTED:
                        _feedbackListeners.forEach(listener -> listener.onDisconnect(false));
                }
            } catch (InterruptedException e) {
                _log.warn(String.format("handleSSEStatusMessages Thread interrupted: exit gracefully.", e.getMessage()));
                // Thread interrupted: exit gracefully.
                Thread.currentThread().interrupt();
                break;
            }
        }

    }
}
