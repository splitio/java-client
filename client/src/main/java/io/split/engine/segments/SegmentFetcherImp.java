package io.split.engine.segments;

import com.google.common.annotations.VisibleForTesting;
import io.split.client.dtos.SegmentChange;
import io.split.storages.SegmentCacheProducer;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.engine.common.FetchOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class SegmentFetcherImp implements SegmentFetcher {
    private static final Logger _log = LoggerFactory.getLogger(SegmentFetcherImp.class);

    private final String _segmentName;
    private final SegmentChangeFetcher _segmentChangeFetcher;
    private final SegmentCacheProducer _segmentCacheProducer;
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    private final Object _lock = new Object();

    public SegmentFetcherImp(String segmentName, SegmentChangeFetcher segmentChangeFetcher, SegmentCacheProducer segmentCacheProducer, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _segmentName = checkNotNull(segmentName);
        _segmentChangeFetcher = checkNotNull(segmentChangeFetcher);
        _segmentCacheProducer = checkNotNull(segmentCacheProducer);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);

        _segmentCacheProducer.updateSegment(segmentName, new ArrayList<>(), new ArrayList<>(), -1L);
    }

    @Override
    public void fetch(FetchOptions opts){
        try {
            fetchUntil(opts);
        } catch (Exception e){
            _log.error("RefreshableSegmentFetcher failed: " + e.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug("Reason:", e);
            }
        }
    }

    private void runWithoutExceptionHandling(FetchOptions options) {
        SegmentChange change = _segmentChangeFetcher.fetch(_segmentName, _segmentCacheProducer.getChangeNumber(_segmentName), options);

        if (change == null) {
            throw new IllegalStateException("SegmentChange was null");
        }

        if (change.till == _segmentCacheProducer.getChangeNumber(_segmentName)) {
            // no change.
            return;
        }

        if (change.since != _segmentCacheProducer.getChangeNumber(_segmentName)
                || change.since < _segmentCacheProducer.getChangeNumber(_segmentName)) {
            // some other thread may have updated the shared state. exit
            return;
        }


        if (change.added.isEmpty() && change.removed.isEmpty()) {
            // there are no changes. weird!
            _segmentCacheProducer.setChangeNumber(_segmentName,change.till);
            return;
        }

        synchronized (_lock) {
            // check state one more time.
            if (change.since != _segmentCacheProducer.getChangeNumber(_segmentName)
                    || change.till < _segmentCacheProducer.getChangeNumber(_segmentName)) {
                // some other thread may have updated the shared state. exit
                return;
            }
            _segmentCacheProducer.updateSegment(_segmentName,change.added, change.removed, change.till);

            if (!change.added.isEmpty()) {
                _log.info(_segmentName + " added keys: " + summarize(change.added));
            }

            if (!change.removed.isEmpty()) {
                _log.info(_segmentName + " removed keys: " + summarize(change.removed));
            }

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
    void fetchUntil(FetchOptions opts){
        final long INITIAL_CN = _segmentCacheProducer.getChangeNumber(_segmentName);
        while (true) {
            long start = _segmentCacheProducer.getChangeNumber(_segmentName);
            runWithoutExceptionHandling(opts);
            if (INITIAL_CN == start) {
                opts = new FetchOptions.Builder(opts).targetChangeNumber(FetchOptions.DEFAULT_TARGET_CHANGENUMBER).build();
            }
            long end = _segmentCacheProducer.getChangeNumber(_segmentName);
            if (start >= end) {
                break;
            }
        }
    }

    @Override
    public boolean runWhitCacheHeader(){
       return this.fetchAndUpdate(new FetchOptions.Builder().cacheControlHeaders(true).build());
    }

    /**
     * Calls callLoopRun and after fetchs segment.
     * @param opts contains all soft of options used when issuing the fetch request
     */
    @VisibleForTesting
    boolean fetchAndUpdate(FetchOptions opts) {
        try {
            // Do this again in case the previous call errored out.
            fetchUntil(opts);
            return true;

        }  catch (Exception e){
            _log.error("RefreshableSegmentFetcher failed: " + e.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug("Reason:", e);
            }
            return false;
        }
    }
}
