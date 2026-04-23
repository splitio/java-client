package io.split.client.events;

import io.split.client.dtos.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class InMemoryEventsStorage implements EventsStorage{

    private static final Logger _log = LoggerFactory.getLogger(InMemoryEventsStorage.class);
    private final BlockingQueue<WrappedEvent> _eventQueue;
    private final int _maxQueueSize;
    private final EventQueueStats _stats;

    public InMemoryEventsStorage(int maxQueueSize, EventQueueStats stats) {
        _eventQueue = new LinkedBlockingQueue<>(maxQueueSize);
        _maxQueueSize = maxQueueSize;
        _stats = Objects.requireNonNull(stats, "stats must not be null");
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
                _stats.onQueued(1);
            }
            else {
                _log.warn("Event queue is full, dropping event.");
                _stats.onDropped(1);
                return false;
            }

        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            _stats.onDropped(1);
            _log.warn("Interruption when adding event withed while adding message %s.", event);
            return false;
        }
        return true;
    }

    int queueSize() {
        return _maxQueueSize - _eventQueue.remainingCapacity();
    }
}
