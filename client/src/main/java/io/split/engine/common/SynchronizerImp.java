package io.split.engine.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.split.cache.SegmentCache;
import io.split.cache.SplitCache;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class SynchronizerImp implements Synchronizer {

    // The boxing here IS necessary, so that the constants are not inlined by the compiler
    // and can be modified for the test (we don't want to wait that much in an UT)
    private static final long ON_DEMAND_FETCH_BACKOFF_BASE_MS = new Long(10000); //backoff base starting at 10 seconds (!)
    private static final long ON_DEMAND_FETCH_BACKOFF_MAX_WAIT_MS = new Long(60000); // don't sleep for more than 1 second
    private static final int ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES = 10;

    private static final Logger _log = LoggerFactory.getLogger(Synchronizer.class);
    private final SplitSynchronizationTask _splitSynchronizationTask;
    private final SplitFetcher _splitFetcher;
    private final SegmentSynchronizationTask _segmentSynchronizationTaskImp;
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
                           boolean cdnResponseHeadersLogging,
                           SDKReadinessGates gates) {
        _splitSynchronizationTask = checkNotNull(splitSynchronizationTask);
        _splitFetcher = checkNotNull(splitFetcher);
        _segmentSynchronizationTaskImp = checkNotNull(segmentSynchronizationTaskImp);
        _splitCache = checkNotNull(splitCache);
        _segmentCache = checkNotNull(segmentCache);
        _onDemandFetchRetryDelayMs = checkNotNull(onDemandFetchRetryDelayMs);
        _cdnResponseHeadersLogging = cdnResponseHeadersLogging;
        _onDemandFetchMaxRetries = onDemandFetchMaxRetries;
        _failedAttemptsBeforeLogging = failedAttemptsBeforeLogging;

    }

    @Override
    public boolean syncAll() {
        return _splitFetcher.fetchAll(new FetchOptions.Builder().cacheControlHeaders(true).build()) && _segmentSynchronizationTaskImp.fetchAllSynchronous();
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

    private static class SyncResult {

        /* package private */ SyncResult(boolean success, int remainingAttempts) {
            _success = success;
            _remainingAttempts =  remainingAttempts;
        }

        public boolean success() { return _success; }
        public int remainingAttempts() { return _remainingAttempts; }

        private final boolean _success;
        private final int _remainingAttempts;
    }

    private SyncResult attemptSplitsSync(long targetChangeNumber,
                                         FetchOptions opts,
                                         Function<Void, Long> nextWaitMs,
                                         int maxRetries) {
        int remainingAttempts = maxRetries;
        while(true) {
            remainingAttempts--;
            _splitFetcher.forceRefresh(opts);
            if (targetChangeNumber <= _splitCache.getChangeNumber()) {
                return new SyncResult(true, remainingAttempts);
            } else if (remainingAttempts <= 0) {
                return new SyncResult(false, remainingAttempts);
            }
            try {
                long howLong = nextWaitMs.apply(null);
                Thread.sleep(howLong);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _log.debug("Error trying to sleep current Thread.");
            }
        }
    }

    private void logCdnHeaders(String prefix, int maxRetries, int remainingAttempts, List<Map<String, String>> headers) {
        if (maxRetries - remainingAttempts > _failedAttemptsBeforeLogging) {
            _log.info(String.format("%s: CDN Debug headers: %s", prefix, gson.toJson(headers)));
        }
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

        SyncResult regularResult = attemptSplitsSync(targetChangeNumber, opts,
                (discard) -> (long) _onDemandFetchRetryDelayMs, _onDemandFetchMaxRetries);

        int attempts =  _onDemandFetchMaxRetries - regularResult.remainingAttempts();
        if (regularResult.success()) {
            _log.debug(String.format("Refresh completed in %s attempts.", attempts));
            if (_cdnResponseHeadersLogging) {
                logCdnHeaders("[splits]", _onDemandFetchMaxRetries , regularResult.remainingAttempts(), captor.get());
            }
            return;
        }

        _log.info(String.format("No changes fetched after %s attempts. Will retry bypassing CDN.", attempts));
        FetchOptions withCdnBypass = new FetchOptions.Builder(opts).targetChangeNumber(targetChangeNumber).build();
        Backoff backoff = new Backoff(ON_DEMAND_FETCH_BACKOFF_BASE_MS, ON_DEMAND_FETCH_BACKOFF_MAX_WAIT_MS);
        SyncResult withCDNBypassed = attemptSplitsSync(targetChangeNumber, withCdnBypass,
                (discard) -> backoff.interval(), ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        int withoutCDNAttempts = ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES - withCDNBypassed._remainingAttempts;
        if (withCDNBypassed.success()) {
            _log.debug(String.format("Refresh completed bypassing the CDN in %s attempts.", withoutCDNAttempts));
        } else {
            _log.debug(String.format("No changes fetched after %s attempts with CDN bypassed.", withoutCDNAttempts));
        }

        if (_cdnResponseHeadersLogging) {
            logCdnHeaders("[splits]", _onDemandFetchMaxRetries + ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES,
                    withCDNBypassed.remainingAttempts(), captor.get());
        }
    }

    @Override
    public void localKillSplit(String splitName, String defaultTreatment, long newChangeNumber) {
        if (newChangeNumber > _splitCache.getChangeNumber()) {
            _splitCache.kill(splitName, defaultTreatment, newChangeNumber);
            refreshSplits(newChangeNumber);
        }
    }

    public SyncResult attemptSegmentSync(String segmentName,
                                         long targetChangeNumber,
                                         FetchOptions opts,
                                         Function<Void, Long> nextWaitMs,
                                         int maxRetries) {

        int remainingAttempts = maxRetries;
        SegmentFetcher fetcher = _segmentSynchronizationTaskImp.getFetcher(segmentName);
        checkNotNull(fetcher);

        while(true) {
            remainingAttempts--;
            fetcher.fetch(opts);
            if (targetChangeNumber <= _segmentCache.getChangeNumber(segmentName)) {
                return new SyncResult(true, remainingAttempts);
            } else if (remainingAttempts <= 0) {
                return new SyncResult(false, remainingAttempts);
            }
            try {
                long howLong = nextWaitMs.apply(null);
                Thread.sleep(howLong);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _log.debug("Error trying to sleep current Thread.");
            }
        }
    }

    @Override
    public void refreshSegment(String segmentName, long targetChangeNumber) {

        if (targetChangeNumber <= _segmentCache.getChangeNumber(segmentName)) {
            return;
        }

        FastlyHeadersCaptor captor = new FastlyHeadersCaptor();
        FetchOptions opts = new FetchOptions.Builder()
                .cacheControlHeaders(true)
                .fastlyDebugHeader(_cdnResponseHeadersLogging)
                .responseHeadersCallback(_cdnResponseHeadersLogging ? captor::handle : null)
                .build();

        SyncResult regularResult = attemptSegmentSync(segmentName, targetChangeNumber, opts,
                (discard) -> (long) _onDemandFetchRetryDelayMs, _onDemandFetchMaxRetries);

        int attempts =  _onDemandFetchMaxRetries - regularResult.remainingAttempts();
        if (regularResult.success()) {
            _log.debug(String.format("Segment %s refresh completed in %s attempts.", segmentName, attempts));
            if (_cdnResponseHeadersLogging) {
                logCdnHeaders(String.format("[segment/%s]", segmentName), _onDemandFetchMaxRetries , regularResult.remainingAttempts(), captor.get());
            }
            return;
        }

        _log.info(String.format("No changes fetched for segment %s after %s attempts. Will retry bypassing CDN.", segmentName, attempts));
        FetchOptions withCdnBypass = new FetchOptions.Builder(opts).targetChangeNumber(targetChangeNumber).build();
        Backoff backoff = new Backoff(ON_DEMAND_FETCH_BACKOFF_BASE_MS, ON_DEMAND_FETCH_BACKOFF_MAX_WAIT_MS);
        SyncResult withCDNBypassed = attemptSegmentSync(segmentName, targetChangeNumber, withCdnBypass,
                (discard) -> backoff.interval(), ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        int withoutCDNAttempts = ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES - withCDNBypassed._remainingAttempts;
        if (withCDNBypassed.success()) {
            _log.debug(String.format("Segment %s refresh completed bypassing the CDN in %s attempts.", segmentName, withoutCDNAttempts));
        } else {
            _log.debug(String.format("No changes fetched for segment %s after %s attempts with CDN bypassed.", segmentName, withoutCDNAttempts));
        }

        if (_cdnResponseHeadersLogging) {
            logCdnHeaders(String.format("[segment/%s]", segmentName), _onDemandFetchMaxRetries + ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES,
                    withCDNBypassed.remainingAttempts(), captor.get());
        }
    }
}
