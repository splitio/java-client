package io.split.engine.sse;

import io.split.engine.common.PushManager;
import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.OccupancyNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationManagerKeeperImp implements NotificationManagerKeeper {
    private static final Logger _log = LoggerFactory.getLogger(NotificationManagerKeeper.class);

    private final AtomicBoolean _streamingAvailable;
    private final LinkedBlockingQueue<PushManager.Status> _statusMessages;

    public NotificationManagerKeeperImp(LinkedBlockingQueue<PushManager.Status> statusMessages) {
        _streamingAvailable = new AtomicBoolean(true);
        _statusMessages = statusMessages;
    }

    @Override
    public void handleIncomingControlEvent(ControlNotification controlNotification) {
        switch (controlNotification.getControlType()) {
            case STREAMING_PAUSED: _statusMessages.offer(PushManager.Status.STREAMING_PAUSED); break;
            case STREAMING_RESUMED: _statusMessages.offer(PushManager.Status.STREAMING_ENABLED); break;
            case STREAMING_DISABLED: _statusMessages.offer(PushManager.Status.STREAMING_DISABLED); break;
            default: _log.error(String.format("Incorrect control type. %s", controlNotification.getControlType()));
        }
    }

    @Override
    public void handleIncomingOccupancyEvent(OccupancyNotification occupancyNotification) {
        int publishers = occupancyNotification.getMetrics().getPublishers();

        if (publishers <= 0 && _streamingAvailable.compareAndSet(true, false)) {
            _statusMessages.offer(PushManager.Status.STREAMING_DISABLED);
        } else if (publishers >= 1 && _streamingAvailable.compareAndSet(false, true)) {
            _statusMessages.offer(PushManager.Status.STREAMING_ENABLED);
        }
    }
}