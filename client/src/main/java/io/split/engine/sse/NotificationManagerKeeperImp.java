package io.split.engine.sse;

import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.OccupancyNotification;
import io.split.engine.sse.listeners.NotificationKeeperListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationManagerKeeperImp implements NotificationManagerKeeper {
    private static final Logger _log = LoggerFactory.getLogger(NotificationManagerKeeper.class);

    private final AtomicBoolean _streamingAvailable;
    private final List<NotificationKeeperListener> _notificationKeeperListeners;

    public NotificationManagerKeeperImp() {
        _streamingAvailable = new AtomicBoolean(true);
        _notificationKeeperListeners = new ArrayList<>();
    }

    @Override
    public void handleIncomingControlEvent(ControlNotification controlNotification) {
        switch (controlNotification.getControlType()) {
            case STREAMING_PAUSED:
                notifyStreamingDisabled();
                break;
            case STREAMING_RESUMED:
                if (isStreamingAvailable()) { notifyStreamingAvailable(); }
                break;
            case STREAMING_DISABLED:
                notifyStreamingShutdown();
                break;
            default:
                _log.error(String.format("Incorrect control type. %s", controlNotification.getControlType()));
                break;
        }
    }

    @Override
    public void handleIncomingOccupancyEvent(OccupancyNotification occupancyNotification) {
        int publishers = occupancyNotification.getMetrics().getPublishers();

        if (publishers <= 0 && isStreamingAvailable()) {
            _streamingAvailable.set(false);
            notifyStreamingDisabled();
        } else if (publishers >= 1 && !isStreamingAvailable()) {
            _streamingAvailable.set(true);
            notifyStreamingAvailable();
        }
    }

    @Override
    public void registerNotificationKeeperListener(NotificationKeeperListener listener) {
        _notificationKeeperListeners.add(listener);
    }

    private boolean isStreamingAvailable() {
        return _streamingAvailable.get();
    }

    private void notifyStreamingAvailable() {
        _notificationKeeperListeners.forEach(l -> l.onStreamingAvailable());
    }

    private void notifyStreamingDisabled() {
        _notificationKeeperListeners.forEach(l -> l.onStreamingDisabled());
    }

    private void notifyStreamingShutdown() {
        _notificationKeeperListeners.forEach(l -> l.onStreamingShutdown());
    }
}
