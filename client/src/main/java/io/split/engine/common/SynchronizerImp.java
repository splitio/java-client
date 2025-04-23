package io.split.engine.common;

import io.split.client.events.EventsTask;
import io.split.client.impressions.ImpressionsManager;
import io.split.client.impressions.UniqueKeysTracker;
import io.split.engine.experiments.FetchResult;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.experiments.SplitSynchronizationTask;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.SegmentSynchronizationTask;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.storages.RuleBasedSegmentCacheProducer;
import io.split.storages.SegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.synchronizer.TelemetrySyncTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final SplitCacheProducer _splitCacheProducer;
    private final RuleBasedSegmentCacheProducer _ruleBasedSegmentCacheProducer;
    private final SegmentCacheProducer segmentCacheProducer;
    private final ImpressionsManager _impressionManager;
    private final EventsTask _eventsTask;
    private final TelemetrySyncTask _telemetrySyncTask;
    private  final UniqueKeysTracker _uniqueKeysTracker;
    private final int _onDemandFetchRetryDelayMs;
    private final int _onDemandFetchMaxRetries;
    private final int _failedAttemptsBeforeLogging;
    private final String _sets;

    public SynchronizerImp(SplitTasks splitTasks,
                           SplitFetcher splitFetcher,
                           SplitCacheProducer splitCacheProducer,
                           SegmentCacheProducer segmentCacheProducer,
                           RuleBasedSegmentCacheProducer ruleBasedSegmentCacheProducer,
                           int onDemandFetchRetryDelayMs,
                           int onDemandFetchMaxRetries,
                           int failedAttemptsBeforeLogging,
                           HashSet<String> sets) {
        _splitSynchronizationTask = checkNotNull(splitTasks.getSplitSynchronizationTask());
        _splitFetcher = checkNotNull(splitFetcher);
        _segmentSynchronizationTaskImp = checkNotNull(splitTasks.getSegmentSynchronizationTask());
        _splitCacheProducer = checkNotNull(splitCacheProducer);
        _ruleBasedSegmentCacheProducer = checkNotNull(ruleBasedSegmentCacheProducer);
        this.segmentCacheProducer = checkNotNull(segmentCacheProducer);
        _onDemandFetchRetryDelayMs = checkNotNull(onDemandFetchRetryDelayMs);
        _onDemandFetchMaxRetries = onDemandFetchMaxRetries;
        _failedAttemptsBeforeLogging = failedAttemptsBeforeLogging;
        _impressionManager = splitTasks.getImpressionManager();
        _eventsTask = splitTasks.getEventsTask();
        _telemetrySyncTask = splitTasks.getTelemetrySyncTask();
        _uniqueKeysTracker = splitTasks.getUniqueKeysTracker();
        _sets = sets.stream().collect(Collectors.joining(","));
    }

    @Override
    public boolean syncAll() {
        FetchResult fetchResult = _splitFetcher.forceRefresh(new FetchOptions.Builder().flagSetsFilter(_sets).cacheControlHeaders(true).build());
        return fetchResult.isSuccess() && _segmentSynchronizationTaskImp.fetchAllSynchronous();
    }

    @Override
    public void startPeriodicFetching() {
        _log.debug("Starting Periodic Fetching ...");
        _splitSynchronizationTask.start();
        _segmentSynchronizationTaskImp.start();
    }

    @Override
    public void stopPeriodicFetching() {
        _log.debug("Stop Periodic Fetching ...");
        _splitSynchronizationTask.stop();
        _segmentSynchronizationTaskImp.stop();
    }

    private static class SyncResult {

        /* package private */ SyncResult(boolean success, int remainingAttempts, FetchResult fetchResult) {
            _success = success;
            _remainingAttempts =  remainingAttempts;
            _fetchResult = fetchResult;
        }

        public boolean success() { return _success; }
        public int remainingAttempts() { return _remainingAttempts; }

        private final boolean _success;
        private final int _remainingAttempts;
        private final FetchResult _fetchResult;
    }

    private SyncResult attemptSplitsSync(long targetChangeNumber, long ruleBasedSegmentChangeNumber,
                                         FetchOptions opts,
                                         Function<Void, Long> nextWaitMs,
                                         int maxRetries) {
        int remainingAttempts = maxRetries;
        while(true) {
            remainingAttempts--;
            FetchResult fetchResult = _splitFetcher.forceRefresh(opts);
            if (fetchResult != null && !fetchResult.retry() && !fetchResult.isSuccess()) {
                return new SyncResult(false, remainingAttempts, fetchResult);
            }
            if ((targetChangeNumber != 0 && targetChangeNumber <= _splitCacheProducer.getChangeNumber()) ||
                    (ruleBasedSegmentChangeNumber != 0 && ruleBasedSegmentChangeNumber <= _ruleBasedSegmentCacheProducer.getChangeNumber())) {
                return new SyncResult(true, remainingAttempts, fetchResult);
            } else if (remainingAttempts <= 0) {
                return new SyncResult(false, remainingAttempts, fetchResult);
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
    public void refreshSplits(Long targetChangeNumber, Long ruleBasedSegmentChangeNumber) {

        if (targetChangeNumber == null || targetChangeNumber == 0) {
            targetChangeNumber = _splitCacheProducer.getChangeNumber();
        }
        if (ruleBasedSegmentChangeNumber == null || ruleBasedSegmentChangeNumber == 0) {
            ruleBasedSegmentChangeNumber = _ruleBasedSegmentCacheProducer.getChangeNumber();
        }
        
        if (targetChangeNumber <= _splitCacheProducer.getChangeNumber() && ruleBasedSegmentChangeNumber <= _ruleBasedSegmentCacheProducer.getChangeNumber()) {
            return;
        }
            return;
        }

        FastlyHeadersCaptor captor = new FastlyHeadersCaptor();
        FetchOptions opts = new FetchOptions.Builder()
                .cacheControlHeaders(true)
                .flagSetsFilter(_sets)
                .build();

        SyncResult regularResult = attemptSplitsSync(targetChangeNumber, ruleBasedSegmentChangeNumber, opts,
                (discard) -> (long) _onDemandFetchRetryDelayMs, _onDemandFetchMaxRetries);

        int attempts =  _onDemandFetchMaxRetries - regularResult.remainingAttempts();
        if (regularResult.success()) {
            _log.debug(String.format("Refresh completed in %s attempts.", attempts));

            regularResult._fetchResult.getSegments().stream()
                    .forEach(segmentName -> forceRefreshSegment(segmentName));
            return;
        }

        _log.info(String.format("No changes fetched after %s attempts. Will retry bypassing CDN.", attempts));
        FetchOptions withCdnBypass = new FetchOptions.Builder(opts).targetChangeNumber(targetChangeNumber).build();
        Backoff backoff = new Backoff(ON_DEMAND_FETCH_BACKOFF_BASE_MS, ON_DEMAND_FETCH_BACKOFF_MAX_WAIT_MS);
        SyncResult withCDNBypassed = attemptSplitsSync(targetChangeNumber, ruleBasedSegmentChangeNumber, withCdnBypass,
                (discard) -> backoff.interval(), ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        int withoutCDNAttempts = ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES - withCDNBypassed._remainingAttempts;
        if (withCDNBypassed.success()) {
            _log.debug(String.format("Refresh completed bypassing the CDN in %s attempts.", withoutCDNAttempts));
            withCDNBypassed._fetchResult.getSegments().stream()
                    .forEach(segmentName -> forceRefreshSegment(segmentName));
        } else {
            _log.debug(String.format("No changes fetched after %s attempts with CDN bypassed.", withoutCDNAttempts));
        }
    }

    @Override
    public void localKillSplit(SplitKillNotification splitKillNotification) {
        if (splitKillNotification.getChangeNumber() > _splitCacheProducer.getChangeNumber()) {
            _splitCacheProducer.kill(splitKillNotification.getSplitName(), splitKillNotification.getDefaultTreatment(),
                    splitKillNotification.getChangeNumber());
            refreshSplits(splitKillNotification.getChangeNumber(), 0L);
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
            if (targetChangeNumber <= segmentCacheProducer.getChangeNumber(segmentName)) {
                return new SyncResult(true, remainingAttempts, new FetchResult(false, true, new HashSet<>()));
            } else if (remainingAttempts <= 0) {
                return new SyncResult(false, remainingAttempts, new FetchResult(false, true, new HashSet<>()));
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
    public void refreshSegment(String segmentName, Long targetChangeNumber) {

        if (targetChangeNumber <= segmentCacheProducer.getChangeNumber(segmentName)) {
            return;
        }

        FastlyHeadersCaptor captor = new FastlyHeadersCaptor();
        FetchOptions opts = new FetchOptions.Builder()
                .cacheControlHeaders(true)
                .build();

        SyncResult regularResult = attemptSegmentSync(segmentName, targetChangeNumber, opts,
                (discard) -> (long) _onDemandFetchRetryDelayMs, _onDemandFetchMaxRetries);

        int attempts =  _onDemandFetchMaxRetries - regularResult.remainingAttempts();
        if (regularResult.success()) {
            _log.debug(String.format("Segment %s refresh completed in %s attempts.", segmentName, attempts));

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
    }

    @Override
    public void startPeriodicDataRecording() {
        try {
            _impressionManager.start();
        } catch (Exception e) {
            _log.error("Error trying to init Impression Manager synchronizer task.", e);
        }
        if (_uniqueKeysTracker != null){
            try {
                _uniqueKeysTracker.start();
            } catch (Exception e) {
                _log.error("Error trying to init Unique Keys Tracker synchronizer task.", e);
            }
        }
        try {
            _eventsTask.start();
        } catch (Exception e) {
            _log.error("Error trying to init Events synchronizer task.", e);
        }
        try {
            _telemetrySyncTask.startScheduledTask();
        } catch (Exception e) {
            _log.error("Error trying to Telemetry synchronizer task.", e);
        }
    }

    @Override
    public void stopPeriodicDataRecording() {
        _impressionManager.close();
        _log.info("Successful shutdown of impressions manager");
        if (_uniqueKeysTracker != null){
            _uniqueKeysTracker.stop();
            _log.info("Successful stop of UniqueKeysTracker");
        }
        _eventsTask.close();
        _log.info("Successful shutdown of eventsTask");
        _telemetrySyncTask.stopScheduledTask();
        _log.info("Successful shutdown of telemetry sync task");
    }

    @Override
    public void forceRefreshSegment(String segmentName){
        SegmentFetcher segmentFetcher = _segmentSynchronizationTaskImp.getFetcher(segmentName);
        segmentFetcher.fetch(new FetchOptions.Builder().build());
    }
}