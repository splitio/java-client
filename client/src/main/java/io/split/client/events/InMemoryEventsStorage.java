package io.split.client.events;


import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.Event;
import io.split.telemetry.domain.enums.EventsDataRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

public class InMemoryEventsStorage implements EventsStorage{

    private static final Logger _log = LoggerFactory.getLogger(InMemoryEventsStorage.class);
    private final BlockingQueue<WrappedEvent> _eventQueue;
    private final int _maxQueueSize;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    public InMemoryEventsStorage(int maxQueueSize, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _eventQueue = new LinkedBlockingQueue<>(maxQueueSize);
        _maxQueueSize = maxQueueSize;
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public WrappedEvent pop() {
        try {
            return _eventQueue.take();
        } catch (InterruptedException e) {
            _log.warn("Got interrupted while waiting for an event in the queue.");
        }
        return null;
    }

    @Override
    public List<WrappedEvent> popAll() {
        ArrayList<WrappedEvent> popped = new ArrayList<>();
        _eventQueue.drainTo(popped);
        return popped;
    }

    @Override
    public boolean isFull() {
        return _eventQueue.remainingCapacity() == 0;
    }

    @Override
    public boolean track(Event event, int eventSize) {
        try {
            if (event == null) {
                return false;
            }
            if(_eventQueue.offer(new WrappedEvent(event, eventSize))) {
                _telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_QUEUED, 1);
            }
            else {
                _log.warn("Event queue is full, dropping event.");
                _telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 1);
                return false;
            }

        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            _telemetryRuntimeProducer.recordEventStats(EventsDataRecordsEnum.EVENTS_DROPPED, 1);
            _log.warn("Interruption when adding event withed while adding message %s.", event);
            return false;
        }
        return true;
    }

    @VisibleForTesting
    int queueSize() {
        return _maxQueueSize - _eventQueue.remainingCapacity();
    }
}
