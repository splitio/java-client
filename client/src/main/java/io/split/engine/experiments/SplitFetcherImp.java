package io.split.engine.experiments;

import io.split.client.dtos.SplitChange;
import io.split.client.exceptions.UriTooLongException;
import io.split.client.utils.FeatureFlagsToUpdate;
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
    private final HashSet<String> _flagSets;

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
                           TelemetryRuntimeProducer telemetryRuntimeProducer, HashSet<String> sets) {
        _splitChangeFetcher = checkNotNull(splitChangeFetcher);
        _parser = checkNotNull(parser);
        _splitCacheProducer = checkNotNull(splitCacheProducer);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        _flagSets = sets;
    }

    @Override
    public FetchResult forceRefresh(FetchOptions options) {
        _log.debug("Force Refresh feature flags starting ...");
        final long INITIAL_CN = _splitCacheProducer.getChangeNumber();
        Set<String> segments = new HashSet<>();
        try {
            while (true) {
                long start = _splitCacheProducer.getChangeNumber();
                segments.addAll(runWithoutExceptionHandling(options));
                long end = _splitCacheProducer.getChangeNumber();

                // If the previous execution was the first one, clear the `cdnBypass` flag
                // for the next fetches. (This will clear a local copy of the fetch options,
                // not the original object that was passed to this method).
                if (INITIAL_CN == start) {
                    options = new FetchOptions.Builder(options).targetChangeNumber(FetchOptions.DEFAULT_TARGET_CHANGENUMBER).build();
                }

                if (start >= end) {
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
            _log.error("RefreshableSplitFetcher failed: " + e.getMessage());
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
        SplitChange change = _splitChangeFetcher.fetch(_splitCacheProducer.getChangeNumber(), options);
        Set<String> segments = new HashSet<>();

        if (change == null) {
            throw new IllegalStateException("SplitChange was null");
        }

        if (change.since != _splitCacheProducer.getChangeNumber() || change.till < _splitCacheProducer.getChangeNumber()) {
            // some other thread may have updated the shared state. exit
            return segments;
        }

        if (change.splits.isEmpty()) {
            // there are no changes. weird!
            _splitCacheProducer.setChangeNumber(change.till);
            return segments;
        }

        synchronized (_lock) {
            // check state one more time.
            if (change.since != _splitCacheProducer.getChangeNumber()
                    || change.till < _splitCacheProducer.getChangeNumber()) {
                // some other thread may have updated the shared state. exit
                return segments;
            }
            FeatureFlagsToUpdate featureFlagsToUpdate = processFeatureFlagChanges(_parser, change.splits, _flagSets);
            segments = featureFlagsToUpdate.getSegments();
            _splitCacheProducer.update(featureFlagsToUpdate.getToAdd(), featureFlagsToUpdate.getToRemove(), change.till);
            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.SPLITS, System.currentTimeMillis());
        }
        return segments;
    }
}