package io.split.client.impressions;

import io.split.client.dtos.KeyImpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class InMemoryImpressionsStorage implements ImpressionsStorage {

    private static final Logger _log = LoggerFactory.getLogger(InMemoryImpressionsStorage.class);

    private final BlockingQueue<KeyImpression> _queue;

    public InMemoryImpressionsStorage(int queueSize) {
        _queue = new LinkedBlockingQueue<>(queueSize);
    }

    @Override
    public List<KeyImpression> pop(int count) {
        ArrayList<KeyImpression> popped = new ArrayList<>();
        _queue.drainTo(popped, count);
        return popped;
    }

    @Override
    public List<KeyImpression> pop() {
        ArrayList<KeyImpression> popped = new ArrayList<>();
        _queue.drainTo(popped);
        return popped;
    }

    @Override
    public boolean isFull() {
        return _queue.remainingCapacity() == 0;
    }

    @Override
    public boolean put(KeyImpression imp) {
        try {
            return _queue.offer(imp);
        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            _log.warn("Unable to send impression to ImpressionsManager", e);
            return false;
        }
    }

    @Override
    public boolean put(List<KeyImpression> imps) {
        return false;
    }
}