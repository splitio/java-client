package io.split.engine.sse;

import io.split.engine.common.PushManager;
import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.ControlType;
import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.OccupancyNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PushStatusTrackerImp implements PushStatusTracker {
    private static final Logger _log = LoggerFactory.getLogger(PushStatusTracker.class);

    private final AtomicBoolean _publishersOnline = new AtomicBoolean(true);
    private final AtomicReference<SseStatus> _sseStatus = new AtomicReference<>(SseStatus.DISCONNECTED);
    private final AtomicReference<ControlType> _backendStatus = new AtomicReference<>(ControlType.STREAMING_RESUMED);
    private final LinkedBlockingQueue<PushManager.Status> _statusMessages;

    public PushStatusTrackerImp(LinkedBlockingQueue<PushManager.Status> statusMessages) {
        _statusMessages = statusMessages;
    }

    public synchronized void reset() {
        _publishersOnline.set(true);
        _sseStatus.set(SseStatus.DISCONNECTED);
        _backendStatus.set(ControlType.STREAMING_RESUMED);
    }

    @Override
    public void handleSseStatus(SseStatus newStatus) {
        _log.debug(String.format("handleSseStatus new status: %s", newStatus.toString()));
        _log.debug(String.format("handleSseStatus current status: %s", _sseStatus.get().toString()));
        switch(newStatus) {
            case CONNECTED:
                if (_sseStatus.compareAndSet(SseStatus.DISCONNECTED, SseStatus.CONNECTED)) {
                    _statusMessages.offer(PushManager.Status.STREAMING_READY);
                }
                break;
            case RETRYABLE_ERROR:
                if (_sseStatus.compareAndSet(SseStatus.CONNECTED, SseStatus.RETRYABLE_ERROR)) {
                    _statusMessages.offer(PushManager.Status.STREAMING_BACKOFF);
                }
            case NONRETRYABLE_ERROR:
                if (_sseStatus.compareAndSet(SseStatus.CONNECTED, SseStatus.NONRETRYABLE_ERROR)
                    || _sseStatus.compareAndSet(SseStatus.RETRYABLE_ERROR, SseStatus.NONRETRYABLE_ERROR)) {
                    _statusMessages.offer(PushManager.Status.STREAMING_OFF);
                }
                break;
            case DISCONNECTED: // Restore initial status
                reset();
                break;
        }
    }

    @Override
    public void handleIncomingControlEvent(ControlNotification controlNotification) {
        if (_backendStatus.get().equals(ControlType.STREAMING_DISABLED)) {
            return;
        }

        switch (controlNotification.getControlType()) {
            case STREAMING_RESUMED:
                if (_backendStatus.compareAndSet(ControlType.STREAMING_PAUSED, ControlType.STREAMING_RESUMED) && _publishersOnline.get()) {
                    _statusMessages.offer(PushManager.Status.STREAMING_READY);
                }
                break;
            case STREAMING_PAUSED:
                if (_backendStatus.compareAndSet(ControlType.STREAMING_RESUMED, ControlType.STREAMING_PAUSED) && _publishersOnline.get()) {
                    // If there are no publishers online, the STREAMING_DOWN message should have already been sent
                    _statusMessages.offer(PushManager.Status.STREAMING_DOWN);
                }
                break;
            case STREAMING_DISABLED:
                _backendStatus.set(ControlType.STREAMING_DISABLED);
                _statusMessages.offer(PushManager.Status.STREAMING_OFF);
                break;
        }
    }

    @Override
    public void handleIncomingOccupancyEvent(OccupancyNotification occupancyNotification) {
        int publishers = occupancyNotification.getMetrics().getPublishers();
        if (publishers <= 0 && _publishersOnline.compareAndSet(true, false) && _backendStatus.get().equals(ControlType.STREAMING_RESUMED)) {
            _statusMessages.offer(PushManager.Status.STREAMING_DOWN);
        } else if (publishers >= 1 && _publishersOnline.compareAndSet(false, true) && _backendStatus.get().equals(ControlType.STREAMING_RESUMED)) {
            _statusMessages.offer(PushManager.Status.STREAMING_READY);
        }
    }

    @Override
    public void handleIncomingAblyError(ErrorNotification notification) {
        _log.debug(String.format("handleIncomingAblyError :", notification.getMessage()));
        if (_backendStatus.get().equals(ControlType.STREAMING_DISABLED)) {
            return; // Ignore
        }
        if (notification.getCode() >= 40140 && notification.getCode() <= 40149) {
            _statusMessages.offer(PushManager.Status.STREAMING_BACKOFF);
        }
        if (notification.getCode() >= 40000 && notification.getCode() <= 49999) {
            _statusMessages.offer(PushManager.Status.STREAMING_OFF);
        }
    }

    @Override
    public synchronized void forcePushDisable() {
        _log.debug("forcePushDisable");
        _publishersOnline.set(false);
        _sseStatus.set(SseStatus.DISCONNECTED);
        _backendStatus.set(ControlType.STREAMING_DISABLED);
        _statusMessages.offer(PushManager.Status.STREAMING_OFF);
    }
}