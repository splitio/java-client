package io.split.engine.sse;

import com.google.common.collect.Maps;
import io.split.engine.common.PushManager;
import io.split.engine.sse.client.SSEClient;
import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.ControlType;
import io.split.engine.sse.dtos.ErrorNotification;
import io.split.engine.sse.dtos.OccupancyNotification;
import io.split.telemetry.domain.StreamingEvent;
import io.split.telemetry.domain.enums.StreamEventsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

public class PushStatusTrackerImp implements PushStatusTracker {
    private static final Logger _log = LoggerFactory.getLogger(PushStatusTracker.class);
    private static final String CONTROL_PRI_CHANNEL = "control_pri";
    private static final String CONTROL_SEC_CHANNEL = "control_sec";

    private final AtomicBoolean _publishersOnline = new AtomicBoolean(true);
    private final AtomicReference<SSEClient.StatusMessage> _sseStatus = new AtomicReference<>(SSEClient.StatusMessage.INITIALIZATION_IN_PROGRESS);
    private final AtomicReference<ControlType> _backendStatus = new AtomicReference<>(ControlType.STREAMING_RESUMED);
    private final LinkedBlockingQueue<PushManager.Status> _statusMessages;
    private final ConcurrentMap<String, Integer> regions = Maps.newConcurrentMap();

    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public PushStatusTrackerImp(LinkedBlockingQueue<PushManager.Status> statusMessages, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _statusMessages = statusMessages;
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    private synchronized void reset() {
        _publishersOnline.set(true);
        _sseStatus.set(SSEClient.StatusMessage.INITIALIZATION_IN_PROGRESS);
        _backendStatus.set(ControlType.STREAMING_RESUMED);
    }

    @Override
    public void handleSseStatus(SSEClient.StatusMessage newStatus) {
        _log.debug(String.format("Current status: %s. New status: %s", _sseStatus.get().toString(), newStatus.toString()));

        switch(newStatus) {
            case FIRST_EVENT:
                if (SSEClient.StatusMessage.CONNECTED.equals(_sseStatus.get())) {
                    _statusMessages.offer(PushManager.Status.STREAMING_READY);
                    _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.CONNECTION_ESTABLISHED.getType(),0l, System.currentTimeMillis()));
                }
            case CONNECTED:
                _sseStatus.compareAndSet(SSEClient.StatusMessage.INITIALIZATION_IN_PROGRESS, SSEClient.StatusMessage.CONNECTED);
                break;
            case RETRYABLE_ERROR:
                if (_sseStatus.compareAndSet(SSEClient.StatusMessage.CONNECTED, SSEClient.StatusMessage.RETRYABLE_ERROR)) {
                    _statusMessages.offer(PushManager.Status.STREAMING_BACKOFF);
                }
                break;
            case NONRETRYABLE_ERROR:
                if (_sseStatus.compareAndSet(SSEClient.StatusMessage.CONNECTED, SSEClient.StatusMessage.NONRETRYABLE_ERROR)
                    || _sseStatus.compareAndSet(SSEClient.StatusMessage.RETRYABLE_ERROR, SSEClient.StatusMessage.NONRETRYABLE_ERROR)) {
                    _statusMessages.offer(PushManager.Status.STREAMING_OFF);
                }
                break;
            case FORCED_STOP:
                if (_sseStatus.compareAndSet(SSEClient.StatusMessage.CONNECTED, SSEClient.StatusMessage.FORCED_STOP)
                    || _sseStatus.compareAndSet(SSEClient.StatusMessage.RETRYABLE_ERROR, SSEClient.StatusMessage.FORCED_STOP)
                    || _sseStatus.compareAndSet(SSEClient.StatusMessage.INITIALIZATION_IN_PROGRESS, SSEClient.StatusMessage.FORCED_STOP)) {
                    _statusMessages.offer(PushManager.Status.STREAMING_DOWN);
                }
                break;
            case INITIALIZATION_IN_PROGRESS: // Restore initial status
                reset();
                break;
        }
    }

    @Override
    public void handleIncomingControlEvent(ControlNotification controlNotification) {
        _log.debug(String.format("handleIncomingOccupancyEvent: %s", controlNotification.getControlType()));

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
                _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.STREAMING_STATUS.getType(), StreamEventsEnum.StreamingStatusValues.STREAMING_PAUSED.getValue(), System.currentTimeMillis()));
                if (_backendStatus.compareAndSet(ControlType.STREAMING_RESUMED, ControlType.STREAMING_PAUSED) && _publishersOnline.get()) {
                    // If there are no publishers online, the STREAMING_DOWN message should have already been sent
                    _statusMessages.offer(PushManager.Status.STREAMING_DOWN);
                }
                break;
            case STREAMING_DISABLED:
                _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.STREAMING_STATUS.getType(), StreamEventsEnum.StreamingStatusValues.STREAMING_DISABLED.getValue(), System.currentTimeMillis()));
                _backendStatus.set(ControlType.STREAMING_DISABLED);
                _statusMessages.offer(PushManager.Status.STREAMING_OFF);
                break;
        }
    }

    @Override
    public void handleIncomingOccupancyEvent(OccupancyNotification occupancyNotification) {
        _log.debug(String.format("handleIncomingOccupancyEvent: publishers=%d", occupancyNotification.getMetrics().getPublishers()));

        int publishers = occupancyNotification.getMetrics().getPublishers();
        recordTelemetryOcuppancy(occupancyNotification, publishers);
        regions.put(occupancyNotification.getChannel(), publishers);
        boolean isPublishers = isPublishers();
        if (!isPublishers && _publishersOnline.compareAndSet(true, false) && _backendStatus.get().equals(ControlType.STREAMING_RESUMED)) {
            _statusMessages.offer(PushManager.Status.STREAMING_DOWN);
        } else if (isPublishers && _publishersOnline.compareAndSet(false, true) && _backendStatus.get().equals(ControlType.STREAMING_RESUMED)) {
            _statusMessages.offer(PushManager.Status.STREAMING_READY);
        }
    }

    @Override
    public void handleIncomingAblyError(ErrorNotification notification) {
        _log.debug(String.format("handleIncomingAblyError: %s", notification.getMessage()));
        _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.ABLY_ERROR.getType(), notification.getCode(), System.currentTimeMillis()));
        if (_backendStatus.get().equals(ControlType.STREAMING_DISABLED)) {
            return; // Ignore
        }
        if (notification.getCode() >= 40140 && notification.getCode() <= 40149) {
            _statusMessages.offer(PushManager.Status.STREAMING_BACKOFF);
            return;
        }
        if (notification.getCode() >= 40000 && notification.getCode() <= 49999) {
            _statusMessages.offer(PushManager.Status.STREAMING_OFF);
        }
    }

    @Override
    public synchronized void forcePushDisable() {
        _log.debug("forcePushDisable");

        _publishersOnline.set(false);
        _sseStatus.set(SSEClient.StatusMessage.INITIALIZATION_IN_PROGRESS);
        _backendStatus.set(ControlType.STREAMING_DISABLED);
        _statusMessages.offer(PushManager.Status.STREAMING_OFF);
    }
        
    private boolean isPublishers() {
        for(Integer publisher : regions.values()) {
            if (publisher > 0) {
                return true;
            }
        }
        return false;
    }

    private void recordTelemetryOcuppancy(OccupancyNotification occupancyNotification, int publishers) {
        if (CONTROL_PRI_CHANNEL.equals(occupancyNotification.getChannel())) {
            _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.OCCUPANCY_PRI.getType(), publishers, System.currentTimeMillis()));
        }
        else if (CONTROL_SEC_CHANNEL.equals(occupancyNotification.getChannel())){
            _telemetryRuntimeProducer.recordStreamingEvents(new StreamingEvent(StreamEventsEnum.OCCUPANCY_SEC.getType(), publishers, System.currentTimeMillis()));
        }

    }
}