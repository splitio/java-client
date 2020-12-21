package io.split.engine.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcherImpMauro;
import io.split.engine.segments.SegmentSynchronizationTaskMauro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class SynchronizerImp implements Synchronizer {
    private static final Logger _log = LoggerFactory.getLogger(Synchronizer.class);

    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final SplitFetcherImp _splitFetcher;
    private final SegmentSynchronizationTaskMauro _segmentSynchronizationTaskMauro;
    private final ScheduledExecutorService _syncAllScheduledExecutorService;
    private final SplitCache _splitCache;
    private final SegmentCache _segmentCache;

    public SynchronizerImp(SplitSynchronizationTask splitSynchronizationTask,
                           SplitFetcherImp splitFetcher,
                           SegmentSynchronizationTaskMauro segmentSynchronizationTaskMauro,
                           SplitCache splitCache,
                           SegmentCache segmentCache) {
        _splitSynchronizationTask = checkNotNull(splitSynchronizationTask);
        _splitFetcher = checkNotNull(splitFetcher);
        _segmentSynchronizationTaskMauro = checkNotNull(segmentSynchronizationTaskMauro);
        _splitCache = checkNotNull(splitCache);
        _segmentCache = checkNotNull(segmentCache);

        ThreadFactory splitsThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SyncAll-%d")
                .build();
        _syncAllScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(splitsThreadFactory);
    }

    @Override
    public void syncAll() {
        _syncAllScheduledExecutorService.schedule(() -> {
            _splitFetcher.run();
            _segmentSynchronizationTaskMauro.run();
        }, 0, TimeUnit.SECONDS);
    }

    @Override
    public void startPeriodicFetching() {
        _log.debug("Starting Periodic Fetching ...");
        _splitSynchronizationTask.startPeriodicFetching();
        _segmentSynchronizationTaskMauro.startPeriodicFetching();
    }

    @Override
    public void stopPeriodicFetching() {
        _log.debug("Stop Periodic Fetching ...");
        _splitSynchronizationTask.stop();
        _segmentSynchronizationTaskMauro.stop();
    }

    @Override
    public void refreshSplits(long targetChangeNumber) {
        if (targetChangeNumber > _splitCache.getChangeNumber()) {
            _splitFetcher.forceRefresh();
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
        if (changeNumber > _segmentCache.getChangeNumber(segmentName)) {
            SegmentFetcherImpMauro fetcher = _segmentSynchronizationTaskMauro.getFetcher(segmentName);
            fetcher.fetch();
        }
    }
}
