package io.split.engine.experiments;

import io.split.client.dtos.SplitChange;
import io.split.client.exceptions.UriTooLongException;
import io.split.client.interceptors.FlagSetsFilter;
import io.split.client.utils.FeatureFlagsToUpdate;
import io.split.client.utils.RuleBasedSegmentsToUpdate;
import io.split.storages.RuleBasedSegmentCacheProducer;
import io.split.storages.SplitCacheProducer;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import io.split.engine.common.FetchOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.client.utils.FeatureFlagProcessor.processFeatureFlagChanges;
import static io.split.client.utils.RuleBasedSegmentProcessor.processRuleBasedSegmentChanges;
import static io.split.client.utils.Utils.checkExitConditions;

/**
 * An ExperimentFetcher that refreshes experiment definitions periodically.
 *
 * @author adil
 */
public class SplitFetcherImp implements SplitFetcher {

    private static final Logger _log = LoggerFactory.getLogger(SplitFetcherImp.class);

    private final SplitParser _parser;
    private final SplitChangeFetcher _splitChangeFetcher;
    private final SplitCacheProducer _splitCacheProducer;
    private final Object _lock = new Object();
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;
    private final FlagSetsFilter _flagSetsFilter;
    private final RuleBasedSegmentCacheProducer _ruleBasedSegmentCacheProducer;
    private final RuleBasedSegmentParser _parserRBS;

    /**
     * Contains all the traffic types that are currently being used by the splits and also the count
     *
     * For example if there are three splits, one of traffic type "account" and two of traffic type "user",
     * this multiset will contain [{"user", 2}, {"account", 1}]
     *
     * The count is used to maintain how many splits are using a traffic type, so when
     * an ARCHIVED split is received, we know if we need to remove a traffic type from the multiset.
     */

    public SplitFetcherImp(SplitChangeFetcher splitChangeFetcher, SplitParser parser, SplitCacheProducer splitCacheProducer,
                           TelemetryRuntimeProducer telemetryRuntimeProducer, FlagSetsFilter flagSetsFilter,
                           RuleBasedSegmentParser parserRBS, RuleBasedSegmentCacheProducer ruleBasedSegmentCacheProducer) {
        _splitChangeFetcher = checkNotNull(splitChangeFetcher);
        _parser = checkNotNull(parser);
        _parserRBS = checkNotNull(parserRBS);
        _splitCacheProducer = checkNotNull(splitCacheProducer);
        _ruleBasedSegmentCacheProducer = checkNotNull(ruleBasedSegmentCacheProducer);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _flagSetsFilter = flagSetsFilter;
    }

    @Override
    public FetchResult forceRefresh(FetchOptions options) {
        _log.debug("Force Refresh feature flags starting ...");
        final long INITIAL_CN = _splitCacheProducer.getChangeNumber();
        final long RBS_INITIAL_CN = _ruleBasedSegmentCacheProducer.getChangeNumber();
        Set<String> segments = new HashSet<>();
        try {
            while (true) {
                long start = _splitCacheProducer.getChangeNumber();
                long startRBS = _ruleBasedSegmentCacheProducer.getChangeNumber();
                segments.addAll(runWithoutExceptionHandling(options));
                long end = _splitCacheProducer.getChangeNumber();
                long endRBS = _ruleBasedSegmentCacheProducer.getChangeNumber();

                // If the previous execution was the first one, clear the `cdnBypass` flag
                // for the next fetches. (This will clear a local copy of the fetch options,
                // not the original object that was passed to this method).
                FetchOptions.Builder optionsBuilder = new FetchOptions.Builder(options);
                if (INITIAL_CN == start) {
                    optionsBuilder.targetChangeNumber(FetchOptions.DEFAULT_TARGET_CHANGENUMBER);
                }

                if (RBS_INITIAL_CN == startRBS) {
                    optionsBuilder.targetChangeNumberRBS(FetchOptions.DEFAULT_TARGET_CHANGENUMBER);
                }

                options = optionsBuilder.build();

                if (start >= end && startRBS >= endRBS) {
                    return new FetchResult(true, false, segments);
                }
            }
        } catch (UriTooLongException u) {
            return new FetchResult(false, false, new HashSet<>());
        } catch (InterruptedException e) {
            _log.warn("Interrupting split fetcher task");
            Thread.currentThread().interrupt();
            return new FetchResult(false, true, new HashSet<>());
        } catch (Exception e) {
            _log.error("SplitFetcherImp failed: " + e.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug("Reason:", e);
            }
            return new FetchResult(false, true, new HashSet<>());
        }
    }

    @Override
    public void run() {
        this.forceRefresh(new FetchOptions.Builder().cacheControlHeaders(false).build());
    }

    private Set<String> runWithoutExceptionHandling(FetchOptions options) throws InterruptedException, UriTooLongException {
        SplitChange change = _splitChangeFetcher.fetch(_splitCacheProducer.getChangeNumber(),
                _ruleBasedSegmentCacheProducer.getChangeNumber(), options);
        Set<String> segments = new HashSet<>();

        if (change == null) {
            throw new IllegalStateException("SplitChange was null");
        }

        if (change.clearCache) {
            _splitCacheProducer.clear();
            _ruleBasedSegmentCacheProducer.clear();
        }

        if (checkExitConditions(change.featureFlags, _splitCacheProducer.getChangeNumber()) ||
            checkExitConditions(change.ruleBasedSegments, _ruleBasedSegmentCacheProducer.getChangeNumber())) {
            return segments;
        }

        if (change.featureFlags.d.isEmpty()) {
            _splitCacheProducer.setChangeNumber(change.featureFlags.t);
        }

        if (change.ruleBasedSegments.d.isEmpty()) {
            _ruleBasedSegmentCacheProducer.setChangeNumber(change.ruleBasedSegments.t);
        }
        
        if (change.featureFlags.d.isEmpty() && change.ruleBasedSegments.d.isEmpty()) {
            return segments;
        }


        synchronized (_lock) {
            // check state one more time.
            if (checkExitConditions(change.featureFlags, _splitCacheProducer.getChangeNumber()) ||
                    checkExitConditions(change.ruleBasedSegments, _ruleBasedSegmentCacheProducer.getChangeNumber())) {
                // some other thread may have updated the shared state. exit
                return segments;
            }
            FeatureFlagsToUpdate featureFlagsToUpdate = processFeatureFlagChanges(_parser, change.featureFlags.d, _flagSetsFilter);
            segments = featureFlagsToUpdate.getSegments();
            _splitCacheProducer.update(featureFlagsToUpdate.getToAdd(), featureFlagsToUpdate.getToRemove(), change.featureFlags.t);

            RuleBasedSegmentsToUpdate ruleBasedSegmentsToUpdate = processRuleBasedSegmentChanges(_parserRBS,
                    change.ruleBasedSegments.d);
            segments.addAll(ruleBasedSegmentsToUpdate.getSegments());
            _ruleBasedSegmentCacheProducer.update(ruleBasedSegmentsToUpdate.getToAdd(),
                    ruleBasedSegmentsToUpdate.getToRemove(), change.ruleBasedSegments.t);
            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.SPLITS, System.currentTimeMillis());
        }

        return segments;
    }
}
