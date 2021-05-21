package io.split.engine.segments;

import com.google.common.annotations.VisibleForTesting;
import io.split.cache.SegmentCache;
import io.split.client.dtos.SegmentChange;
import io.split.engine.SDKReadinessGates;
import io.split.telemetry.domain.enums.HTTPLatenciesEnum;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class SegmentFetcherImp implements SegmentFetcher {
    private static final Logger _log = LoggerFactory.getLogger(SegmentFetcherImp.class);

    private final String _segmentName;
    private final SegmentChangeFetcher _segmentChangeFetcher;
    private final SegmentCache _segmentCache;
    private final SDKReadinessGates _gates;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    private final Object _lock = new Object();

    public SegmentFetcherImp(String segmentName, SegmentChangeFetcher segmentChangeFetcher, SDKReadinessGates gates, SegmentCache segmentCache, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _segmentName = checkNotNull(segmentName);
        _segmentChangeFetcher = checkNotNull(segmentChangeFetcher);
        _segmentCache = checkNotNull(segmentCache);
        _gates = checkNotNull(gates);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);

        _segmentCache.updateSegment(segmentName, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public void fetch(boolean addCacheHeader){
        try {
            callLoopRun(false, addCacheHeader);
        } catch (Throwable t) {
            _log.error("RefreshableSegmentFetcher failed: " + t.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug("Reason:", t);
            }
        }
    }

    private void runWithoutExceptionHandling(boolean addCacheHeader) {
        long initTime = System.currentTimeMillis();
        SegmentChange change = _segmentChangeFetcher.fetch(_segmentName, _segmentCache.getChangeNumber(_segmentName), addCacheHeader);

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
            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.SEGMENTS, System.currentTimeMillis());
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

    @VisibleForTesting
    void callLoopRun(boolean isFetch, boolean addCacheHeader){
        while (true) {
            long start = _segmentCache.getChangeNumber(_segmentName);
            runWithoutExceptionHandling(addCacheHeader);
            long end = _segmentCache.getChangeNumber(_segmentName);
            if (isFetch && _log.isDebugEnabled()) {
                _log.debug(_segmentName + " segment fetch before: " + start + ", after: " + _segmentCache.getChangeNumber(_segmentName) /*+ " size: " + _concurrentKeySet.size()*/);
            }
            if (start >= end) {
                break;
            }
        }
    }

    @Override
    public boolean runWhitCacheHeader(){
        return this.fetchAndUpdate(true);
    }

    /**
     * Calls callLoopRun and after fetchs segment.
     * @param addCacheHeader indicates if CacheHeader is required
     */
    @VisibleForTesting
    boolean fetchAndUpdate(boolean addCacheHeader) {
        try {
            // Do this again in case the previous call errored out.
            callLoopRun(true, addCacheHeader);
            return true;
        } catch (Throwable t) {
            _log.error("RefreshableSegmentFetcher failed: " + t.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug("Reason:", t);
            }
            return false;
        }
    }

    @Override
    public void fetchAll() {
        this.fetchAndUpdate(false);
    }
}
