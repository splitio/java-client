package io.split.engine.experiments;

import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.engine.SDKReadinessGates;
import io.split.cache.SplitCache;
import io.split.telemetry.domain.enums.LastSynchronizationRecordsEnum;
import io.split.telemetry.storage.TelemetryRuntimeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An ExperimentFetcher that refreshes experiment definitions periodically.
 *
 * @author adil
 */
public class SplitFetcherImp implements SplitFetcher {

    private static final Logger _log = LoggerFactory.getLogger(SplitFetcherImp.class);

    private final SplitParser _parser;
    private final SplitChangeFetcher _splitChangeFetcher;
    private final SplitCache _splitCache;
    private final SDKReadinessGates _gates;
    private final Object _lock = new Object();
    private final TelemetryRuntimeProducer _telemetryRuntimeProducer;

    /**
     * Contains all the traffic types that are currently being used by the splits and also the count
     *
     * For example if there are three splits, one of traffic type "account" and two of traffic type "user",
     * this multiset will contain [{"user", 2}, {"account", 1}]
     *
     * The count is used to maintain how many splits are using a traffic type, so when
     * an ARCHIVED split is received, we know if we need to remove a traffic type from the multiset.
     */

    public SplitFetcherImp(SplitChangeFetcher splitChangeFetcher, SplitParser parser, SDKReadinessGates gates, SplitCache splitCache, TelemetryRuntimeProducer telemetryRuntimeProducer) {
        _splitChangeFetcher = checkNotNull(splitChangeFetcher);
        _parser = checkNotNull(parser);
        _gates = checkNotNull(gates);
        _splitCache = checkNotNull(splitCache);
        _telemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public void forceRefresh(boolean addCacheHeader) {
        _log.debug("Force Refresh splits starting ...");
        try {
            while (true) {
                long start = _splitCache.getChangeNumber();
                runWithoutExceptionHandling(addCacheHeader);
                long end = _splitCache.getChangeNumber();

                if (start >= end) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            _log.warn("Interrupting split fetcher task");
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            _log.error("RefreshableSplitFetcher failed: " + t.getMessage());
        }
    }

    @Override
    public void run() {
        this.fetchAll(false);
    }

    private void runWithoutExceptionHandling(boolean addCacheHeader) throws InterruptedException {
        long initTime = System.currentTimeMillis();
        SplitChange change = _splitChangeFetcher.fetch(_splitCache.getChangeNumber(), addCacheHeader);

        if (change == null) {
            throw new IllegalStateException("SplitChange was null");
        }

        if (change.till == _splitCache.getChangeNumber()) {
            // no change.
            return;
        }

        if (change.since != _splitCache.getChangeNumber() || change.till < _splitCache.getChangeNumber()) {
            // some other thread may have updated the shared state. exit
            return;
        }

        if (change.splits.isEmpty()) {
            // there are no changes. weird!
            _splitCache.setChangeNumber(change.till);
            return;
        }

        synchronized (_lock) {
            // check state one more time.
            if (change.since != _splitCache.getChangeNumber()
                    || change.till < _splitCache.getChangeNumber()) {
                // some other thread may have updated the shared state. exit
                return;
            }

            for (Split split : change.splits) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                if (split.status != Status.ACTIVE) {
                    // archive.
                    _splitCache.remove(split.name);
                    continue;
                }

                ParsedSplit parsedSplit = _parser.parse(split);
                if (parsedSplit == null) {
                    _log.info("We could not parse the experiment definition for: " + split.name + " so we are removing it completely to be careful");

                    _splitCache.remove(split.name);
                    _log.debug("Deleted feature: " + split.name);

                    continue;
                }

                // If the split already exists, this is either an update, or the split has been
                // deleted and recreated (possibly with a different traffic type).
                // If it's an update, the traffic type should NOT be increased.
                // If it's deleted & recreated, the old one should be decreased and the new one increased.
                // To handle both cases, we simply delete the old one if the split is present.
                // The new one is always increased.
                ParsedSplit current = _splitCache.get(split.name);
                if (current != null) {
                    _splitCache.remove(split.name);
                }

                _splitCache.put(parsedSplit);
                _log.debug("Updated feature: " + parsedSplit.feature());
            }

            _splitCache.setChangeNumber(change.till);
            long endtime = System.currentTimeMillis();
            _telemetryRuntimeProducer.recordSuccessfulSync(LastSynchronizationRecordsEnum.SPLITS, endtime-initTime);
        }
    }
    @Override
    public void fetchAll(boolean addCacheHeader) {
        _log.debug("Fetch splits starting ...");
        long start = _splitCache.getChangeNumber();
        try {
            runWithoutExceptionHandling(addCacheHeader);
            _gates.splitsAreReady();
        } catch (InterruptedException e) {
            _log.warn("Interrupting split fetcher task");
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            _log.error("RefreshableSplitFetcher failed: " + t.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug("Reason:", t);
            }
        } finally {
            if (_log.isDebugEnabled()) {
                _log.debug("split fetch before: " + start + ", after: " + _splitCache.getChangeNumber());
            }
        }
    }
}
