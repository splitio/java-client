package io.split.engine.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
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
    private final SplitFetcher _splitFetcher;
    private final SegmentSynchronizationTask _segmentSynchronizationTaskImp;
    private final ScheduledExecutorService _syncAllScheduledExecutorService;
    private final SplitCache _splitCache;
    private final SegmentCache _segmentCache;
    private final int _onDemandFetchRetryDelayMs;
    private final int _onDemandFetchMaxRetries;
    private final int _failedAttemptsBeforeLogging;
    private final boolean _cdnResponseHeadersLogging;

    private final Gson gson = new GsonBuilder().create();

    public SynchronizerImp(SplitSynchronizationTask splitSynchronizationTask,
                           SplitFetcher splitFetcher,
                           SegmentSynchronizationTask segmentSynchronizationTaskImp,
                           SplitCache splitCache,
                           SegmentCache segmentCache,
                           int onDemandFetchRetryDelayMs,
                           int onDemandFetchMaxRetries,
                           int failedAttemptsBeforeLogging,
                           boolean cdnResponseHeadersLogging) {
        _splitSynchronizationTask = checkNotNull(splitSynchronizationTask);
        _splitFetcher = checkNotNull(splitFetcher);
        _segmentSynchronizationTaskImp = checkNotNull(segmentSynchronizationTaskImp);
        _splitCache = checkNotNull(splitCache);
        _segmentCache = checkNotNull(segmentCache);
        _onDemandFetchRetryDelayMs = checkNotNull(onDemandFetchRetryDelayMs);
        _cdnResponseHeadersLogging = cdnResponseHeadersLogging;
        _onDemandFetchMaxRetries = onDemandFetchMaxRetries;
        _failedAttemptsBeforeLogging = failedAttemptsBeforeLogging;

        ThreadFactory splitsThreadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-SyncAll-%d")
                .build();
        _syncAllScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(splitsThreadFactory);
    }

    @Override
    public void syncAll() {
        _syncAllScheduledExecutorService.schedule(() -> {
            _splitFetcher.fetchAll(new FetchOptions.Builder().cacheControlHeaders(true).build());
            _segmentSynchronizationTaskImp.fetchAll(true);
        }, 0, TimeUnit.SECONDS);
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

        if (targetChangeNumber <= _splitCache.getChangeNumber()) {
            return;
        }

        FastlyHeadersCaptor captor = new FastlyHeadersCaptor();
        FetchOptions opts = new FetchOptions.Builder()
                .cacheControlHeaders(true)
                .fastlyDebugHeader(_cdnResponseHeadersLogging)
                .responseHeadersCallback(_cdnResponseHeadersLogging ? captor::handle : null)
                .build();

        int remainingAttempts = _onDemandFetchMaxRetries;
        while(true) {
            remainingAttempts--;
            _splitFetcher.forceRefresh(opts);
            if (targetChangeNumber <= _splitCache.getChangeNumber()) {
                _log.debug(String.format("Refresh completed in %s attempts.", _onDemandFetchMaxRetries - remainingAttempts));
                break;
            } else if (remainingAttempts <= 0) {
                _log.info(String.format("No changes fetched after %s attempts.", _onDemandFetchMaxRetries));
                break;
            }
            try {
                Thread.sleep(_onDemandFetchRetryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _log.debug("Error trying to sleep current Thread.");
            }
        }

        if (_cdnResponseHeadersLogging &&
                (_onDemandFetchMaxRetries - remainingAttempts) > _failedAttemptsBeforeLogging) {
            _log.info(String.format("CDN Debug headers: %s", gson.toJson(captor.get())));
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
        while(changeNumber > _segmentCache.getChangeNumber(segmentName) && retries <= _onDemandFetchMaxRetries) {
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
