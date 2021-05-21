package io.split.engine.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public class SynchronizerImp implements Synchronizer {
    private static final Logger _log = LoggerFactory.getLogger(Synchronizer.class);
    private static final int RETRIES_NUMBER = 10;

    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final SplitFetcher _splitFetcher;
    private final SegmentSynchronizationTask _segmentSynchronizationTaskImp;
    private final ScheduledExecutorService _syncAllScheduledExecutorService;
    private final SplitCache _splitCache;
    private final SegmentCache _segmentCache;
    private final int _onDemandFetchRetryDelayMs;
    private final SDKReadinessGates _gates;

    public SynchronizerImp(SplitSynchronizationTask splitSynchronizationTask,
                           SplitFetcher splitFetcher,
                           SegmentSynchronizationTask segmentSynchronizationTaskImp,
                           SplitCache splitCache,
                           SegmentCache segmentCache,
                           int onDemandFetchRetryDelayMs,
                           SDKReadinessGates gates) {
        _splitSynchronizationTask = checkNotNull(splitSynchronizationTask);
        _splitFetcher = checkNotNull(splitFetcher);
        _segmentSynchronizationTaskImp = checkNotNull(segmentSynchronizationTaskImp);
        _splitCache = checkNotNull(splitCache);
        _segmentCache = checkNotNull(segmentCache);
        _onDemandFetchRetryDelayMs = checkNotNull(onDemandFetchRetryDelayMs);
        _gates = checkNotNull(gates);

        ThreadFactory splitsThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SyncAll-%d")
                .build();
        _syncAllScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(splitsThreadFactory);
    }

    @Override
    public boolean syncAll() {
        return _splitFetcher.fetchAll(true) && _segmentSynchronizationTaskImp.fetchAllSynchronous();
    }

    @Override
    public void startPeriodicFetching() {
        _log.debug("Starting Periodic Fetching ...");
        _splitSynchronizationTask.startPeriodicFetching();
        _segmentSynchronizationTaskImp.startPeriodicFetching();
    }

    @Override
    public void stopPeriodicFetching() {
        _log.debug("Stop Periodic Fetching ...");
        _splitSynchronizationTask.stop();
        _segmentSynchronizationTaskImp.stop();
    }

    @Override
    public void refreshSplits(long targetChangeNumber) {
        int retries = RETRIES_NUMBER;
        while(targetChangeNumber > _splitCache.getChangeNumber()) {
            retries--;
            _splitFetcher.forceRefresh(true);
            if (targetChangeNumber <= _splitCache.getChangeNumber()) {
                _log.debug("Refresh completed in %s attempts.", RETRIES_NUMBER - retries);
                return;
            } else if (retries <= 0) {
                _log.warn("No changes fetched after %s attempts.", RETRIES_NUMBER);
                return;
            }
            try {
                Thread.sleep(_onDemandFetchRetryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _log.debug("Error trying to sleep current Thread.");
            }
        }
    }

    @Override
    public void localKillSplit(String splitName, String defaultTreatment, long newChangeNumber) {
        if (newChangeNumber > _splitCache.getChangeNumber()) {
            _splitCache.kill(splitName, defaultTreatment, newChangeNumber);
            refreshSplits(newChangeNumber);
        }
    }

    @Override
    public void refreshSegment(String segmentName, long changeNumber) {
        int retries = 1;
        while(changeNumber > _segmentCache.getChangeNumber(segmentName) && retries <= RETRIES_NUMBER) {
            SegmentFetcher fetcher = _segmentSynchronizationTaskImp.getFetcher(segmentName);
            try{
                fetcher.fetch(true);
            }
            //We are sure this will never happen because getFetcher firts initiate the segment. This try/catch is for safe only.
            catch (NullPointerException np){
                throw new NullPointerException();
            }
            retries++;
        }
    }
}
