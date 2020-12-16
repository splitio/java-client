package io.split.engine.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.split.cache.SplitCache;
import io.split.engine.experiments.SplitFetcherImp;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.RefreshableSegmentFetcher;
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
    private final RefreshableSegmentFetcher _segmentFetcher;
    private final ScheduledExecutorService _syncAllScheduledExecutorService;
    private final SplitCache _splitCache;

    public SynchronizerImp(SplitSynchronizationTask splitSynchronizationTask,
                           SplitFetcherImp splitFetcher,
                           RefreshableSegmentFetcher segmentFetcher,
                           SplitCache splitCache) {
        _splitSynchronizationTask = checkNotNull(splitSynchronizationTask);
        _splitFetcher = checkNotNull(splitFetcher);
        _segmentFetcher = checkNotNull(segmentFetcher);
        _splitCache = checkNotNull(splitCache);

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
            _segmentFetcher.forceRefreshAll();
        }, 0, TimeUnit.SECONDS);
    }

    @Override
    public void startPeriodicFetching() {
        _log.debug("Starting Periodic Fetching ...");
        _splitSynchronizationTask.startPeriodicFetching();
        _segmentFetcher.startPeriodicFetching();
    }

    @Override
    public void stopPeriodicFetching() {
        _log.debug("Stop Periodic Fetching ...");
        _splitSynchronizationTask.stop();
        _segmentFetcher.stop();
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
        if (changeNumber > _segmentFetcher.getChangeNumber(segmentName)) {
            _segmentFetcher.forceRefresh(segmentName);
        }
    }
}
