package io.split.engine.segments;

import io.split.client.dtos.SegmentChange;
import io.split.engine.SDKReadinessGates;
import io.split.cache.SegmentCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    private final SegmentCache _segmentCache;
    private final SDKReadinessGates _gates;

    private final Object _lock = new Object();

    @Override
    public String segmentName() {
        return _segmentName;
    }

    @Override
    public boolean contains(String key) {
        return _segmentCache.isInSegment(_segmentName, key);
    }

    /*package private*/ /*Set<String> fetch() {
        return Collections.unmodifiableSet(_concurrentKeySet);
    }*/

    @Override
    public void forceRefresh() {
        try {
            _log.debug("Force Refresh segment starting ...");
            while (true) {
                long start = _segmentCache.getChangeNumber(_segmentName);
                runWithoutExceptionHandling();
                long end = _segmentCache.getChangeNumber(_segmentName);

                if (start >= end) {
                    break;
                }
            }
        } catch (Throwable t) {
            _log.error("forceRefresh segment failed: " + t.getMessage());
        }
    }

    @Override
    public long changeNumber() {
        return _segmentCache.getChangeNumber(_segmentName);
    }

    public static RefreshableSegment create(String segmentName, SegmentChangeFetcher segmentChangeFetcher, SDKReadinessGates gates, SegmentCache segmentCache) {
        return new RefreshableSegment(segmentName, segmentChangeFetcher, -1L, gates, segmentCache);
    }


    public RefreshableSegment(String segmentName, SegmentChangeFetcher segmentChangeFetcher, long changeNumber, SDKReadinessGates gates, SegmentCache segmentCache) {
        _segmentName = segmentName;
        _segmentChangeFetcher = segmentChangeFetcher;
        _segmentCache = segmentCache;
        _gates = gates;

        checkNotNull(_segmentChangeFetcher);
        checkNotNull(_segmentName);
        checkNotNull(_gates);
        checkNotNull(_segmentCache);
        _segmentCache.updateSegment(segmentName, new ArrayList<>(), new ArrayList<>());
        _segmentCache.setChangeNumber(segmentName, changeNumber);

    }

    @Override
    public void run() {
        try {
            // Do this again in case the previous call errored out.
            _gates.registerSegment(_segmentName);
            while (true) {
                long start = _segmentCache.getChangeNumber(_segmentName);
                runWithoutExceptionHandling();
                long end = _segmentCache.getChangeNumber(_segmentName);
                if (_log.isDebugEnabled()) {
                    _log.debug(_segmentName + " segment fetch before: " + start + ", after: " + _segmentCache.getChangeNumber(_segmentName) /*+ " size: " + _concurrentKeySet.size()*/);
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
        SegmentChange change = _segmentChangeFetcher.fetch(_segmentName, _segmentCache.getChangeNumber(_segmentName));

        if (change == null) {
            throw new IllegalStateException("SegmentChange was null");
        }

        if (change.till == _segmentCache.getChangeNumber(_segmentName)) {
            // no change.
            return;
        }

        if (change.since != _segmentCache.getChangeNumber(_segmentName)
                || change.since < _segmentCache.getChangeNumber(_segmentName)) {
            // some other thread may have updated the shared state. exit
            return;
        }


        if (change.added.isEmpty() && change.removed.isEmpty()) {
            // there are no changes. weird!
            _segmentCache.setChangeNumber(_segmentName,change.till);
            return;
        }

        synchronized (_lock) {
            // check state one more time.
            if (change.since != _segmentCache.getChangeNumber(_segmentName)
                    || change.till < _segmentCache.getChangeNumber(_segmentName)) {
                // some other thread may have updated the shared state. exit
                return;
            }
            //updateSegment(sn, toadd, tormv, chngN)
            _segmentCache.updateSegment(_segmentName,change.added, change.removed);

            if (!change.added.isEmpty()) {
                _log.info(_segmentName + " added keys: " + summarize(change.added));
            }

            if (!change.removed.isEmpty()) {
                _log.info(_segmentName + " removed keys: " + summarize(change.removed));
            }

            _segmentCache.setChangeNumber(_segmentName,change.till);
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
