package io.split.engine.segments;

import io.split.client.dtos.SegmentChange;
import io.split.engine.SDKReadinessGates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A SegmentFetcher implementation that can periodically refresh itself.
 *
 * @author adil
 */
public class RefreshableSegment implements Runnable, Segment {
    private static final Logger _log = LoggerFactory.getLogger(RefreshableSegment.class);

    private final String _segmentName;
    private final SegmentChangeFetcher _segmentChangeFetcher;
    private final AtomicLong _changeNumber;
    private final SDKReadinessGates _gates;

    private Set<String> _concurrentKeySet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final Object _lock = new Object();

    @Override
    public String segmentName() {
        return _segmentName;
    }

    @Override
    public boolean contains(String key) {
        return _concurrentKeySet.contains(key);
    }

    /*package private*/ Set<String> fetch() {
        return Collections.unmodifiableSet(_concurrentKeySet);
    }

    @Override
    public void forceRefresh() {
        run();
    }

    public long changeNumber() {
        return _changeNumber.get();
    }

    public static RefreshableSegment create(String segmentName, SegmentChangeFetcher segmentChangeFetcher, SDKReadinessGates gates) {
        return new RefreshableSegment(segmentName, segmentChangeFetcher, -1L, gates);
    }


    public RefreshableSegment(String segmentName, SegmentChangeFetcher segmentChangeFetcher, long changeNumber, SDKReadinessGates gates) {
        _segmentName = segmentName;
        _segmentChangeFetcher = segmentChangeFetcher;
        _changeNumber = new AtomicLong(changeNumber);
        _gates = gates;

        checkNotNull(_segmentChangeFetcher);
        checkNotNull(_segmentName);
        checkNotNull(_gates);
    }

    @Override
    public void run() {
        try {
            _gates.registerSegment(_segmentName);
            while (true) {
                long start = _changeNumber.get();
                runWithoutExceptionHandling();
                long end = _changeNumber.get();
                if (_log.isDebugEnabled()) {
                    _log.debug(_segmentName + " segment fetch before: " + start + ", after: " + _changeNumber.get() + " size: " + _concurrentKeySet.size());
                }
                if (start >= end) {
                    break;
                }
            }

            _gates.segmentIsReady(_segmentName);

        } catch (Throwable t) {
            _log.error("RefreshableSegmentFetcher failed: " + t.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug("Reason:", t);
            }
        }
    }

    private void runWithoutExceptionHandling() {
        SegmentChange change = _segmentChangeFetcher.fetch(_segmentName, _changeNumber.get());

        if (change == null) {
            throw new IllegalStateException("SegmentChange was null");
        }

        if (change.till == _changeNumber.get()) {
            // no change.
            return;
        }

        if (change.since != _changeNumber.get()
                || change.since < _changeNumber.get()) {
            // some other thread may have updated the shared state. exit
            return;
        }


        if (change.added.isEmpty() && change.removed.isEmpty()) {
            // there are no changes. weird!
            _changeNumber.set(change.till);
            return;
        }

        synchronized (_lock) {
            // check state one more time.
            if (change.since != _changeNumber.get()
                    || change.till < _changeNumber.get()) {
                // some other thread may have updated the shared state. exit
                return;
            }

            for (String added : change.added) {
                _concurrentKeySet.add(added);
            }

            if (!change.added.isEmpty()) {
                _log.info(_segmentName + " added keys: " + summarize(change.added));
            }

            for (String removed : change.removed) {
                _concurrentKeySet.remove(removed);
            }

            if (!change.removed.isEmpty()) {
                _log.info(_segmentName + " removed keys: " + summarize(change.removed));
            }

            _changeNumber.set(change.till);
        }
    }

    private String summarize(List<String> changes) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("[");
        for (int i = 0; i < Math.min(3, changes.size()); i++) {
            if (i != 0) {
                bldr.append(", ");
            }
            bldr.append(changes.get(i));
        }

        if (changes.size() > 3) {
            bldr.append("... ");
            bldr.append((changes.size() - 3));
            bldr.append(" others");
        }
        bldr.append("]");

        return bldr.toString();
    }


    @Override
    public String toString() {
        return "RefreshableSegmentFetcher[" + _segmentName + "]";
    }

}
