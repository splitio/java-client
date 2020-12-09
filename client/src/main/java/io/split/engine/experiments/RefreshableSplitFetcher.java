package io.split.engine.experiments;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.engine.SDKReadinessGates;
import io.split.engine.cache.SplitCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An ExperimentFetcher that refreshes experiment definitions periodically.
 *
 * @author adil
 */
public class RefreshableSplitFetcher implements SplitFetcher, Runnable {

    private static final Logger _log = LoggerFactory.getLogger(RefreshableSplitFetcher.class);

    private final SplitParser _parser;
    private final SplitChangeFetcher _splitChangeFetcher;
    private final SplitCache _splitCache;
    private final SDKReadinessGates _gates;
    private final Object _lock = new Object();

    /**
     * Contains all the traffic types that are currently being used by the splits and also the count
     *
     * For example if there are three splits, one of traffic type "account" and two of traffic type "user",
     * this multiset will contain [{"user", 2}, {"account", 1}]
     *
     * The count is used to maintain how many splits are using a traffic type, so when
     * an ARCHIVED split is received, we know if we need to remove a traffic type from the multiset.
     */
    Multiset<String> _concurrentTrafficTypeNameSet = ConcurrentHashMultiset.create();

    public RefreshableSplitFetcher(SplitChangeFetcher splitChangeFetcher, SplitParser parser, SDKReadinessGates gates, SplitCache splitCache) {
        _splitChangeFetcher = checkNotNull(splitChangeFetcher);
        _parser = checkNotNull(parser);
        _gates = checkNotNull(gates);
        _splitCache = checkNotNull(splitCache);
    }

    @Override
    public void forceRefresh() {
        _log.debug("Force Refresh splits starting ...");
        try {
            while (true) {
                long start = _splitCache.getChangeNumber();
                runWithoutExceptionHandling();
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
    public long changeNumber() {
        return _splitCache.getChangeNumber();
    }

    @Override
    public void killSplit(String splitName, String defaultTreatment, long changeNumber) {
        synchronized (_lock) {
            ParsedSplit parsedSplit = _splitCache.get(splitName);

            ParsedSplit updatedSplit = new ParsedSplit(parsedSplit.feature(),
                    parsedSplit.seed(),
                    true,
                    defaultTreatment,
                    parsedSplit.parsedConditions(),
                    parsedSplit.trafficTypeName(),
                    changeNumber,
                    parsedSplit.trafficAllocation(),
                    parsedSplit.trafficAllocationSeed(),
                    parsedSplit.algo(),
                    parsedSplit.configurations());

            _splitCache.put(updatedSplit);
        }
    }

    @Override
    public ParsedSplit fetch(String test) {
        return _splitCache.get(test);
    }

    public List<ParsedSplit> fetchAll() {
        return Lists.newArrayList(_splitCache.getAll());
    }

    @Override
    public Set<String> fetchKnownTrafficTypes() {
        // We return the "keys" of the multiset that have a count greater than 0
        // If the multiset has [{"user",2}.{"account",0}], elementSet only returns
        // ["user"] (it ignores "account")
        return Sets.newHashSet(_concurrentTrafficTypeNameSet.elementSet());
    }

    public Collection<ParsedSplit> fetch() {
        return _splitCache.getAll();
    }

    public void clear() {
        _splitCache.clear();
        _concurrentTrafficTypeNameSet.clear();
    }

    @Override
    public void run() {
        _log.debug("Fetch splits starting ...");
        long start = _splitCache.getChangeNumber();
        try {
            runWithoutExceptionHandling();
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

    public void runWithoutExceptionHandling() throws InterruptedException {
        SplitChange change = _splitChangeFetcher.fetch(_splitCache.getChangeNumber());

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

            Set<String> toRemove = Sets.newHashSet();
            Map<String, ParsedSplit> toAdd = Maps.newHashMap();
            List<String> trafficTypeNamesToRemove = Lists.newArrayList();
            List<String> trafficTypeNamesToAdd = Lists.newArrayList();

            for (Split split : change.splits) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                if (split.status != Status.ACTIVE) {
                    // archive.
                    toRemove.add(split.name);
                    if (split.trafficTypeName != null) {
                        trafficTypeNamesToRemove.add(split.trafficTypeName);
                    }
                    continue;
                }

                ParsedSplit parsedSplit = _parser.parse(split);
                if (parsedSplit == null) {
                    _log.info("We could not parse the experiment definition for: " + split.name + " so we are removing it completely to be careful");
                    toRemove.add(split.name);
                    if (split.trafficTypeName != null) {
                        trafficTypeNamesToRemove.add(split.trafficTypeName);
                    }
                    continue;
                }

                toAdd.put(split.name, parsedSplit);

                // If the split already exists, this is either an update, or the split has been
                // deleted and recreated (possibly with a different traffic type).
                // If it's an update, the traffic type should NOT be increased.
                // If it's deleted & recreated, the old one should be decreased and the new one increased.
                // To handle both cases, we simply delete the old one if the split is present.
                // The new one is always increased.
                ParsedSplit current = _splitCache.get(split.name);
                if (current != null && current.trafficTypeName() != null) {
                    trafficTypeNamesToRemove.add(current.trafficTypeName());
                }

                if (split.trafficTypeName != null) {
                    trafficTypeNamesToAdd.add(split.trafficTypeName);
                }
            }

            _splitCache.putAll(toAdd);
            _concurrentTrafficTypeNameSet.addAll(trafficTypeNamesToAdd);
            //removeAll does not work here, since it wont remove all the occurrences, just one
            Multisets.removeOccurrences(_concurrentTrafficTypeNameSet, trafficTypeNamesToRemove);

            for (String remove : toRemove) {
                _splitCache.remove(remove);
            }

            if (!toAdd.isEmpty()) {
                _log.debug("Updated features: " + toAdd.keySet());
            }

            if (!toRemove.isEmpty()) {
                _log.debug("Deleted features: " + toRemove);
            }

            _splitCache.setChangeNumber(change.till);
        }
    }
}
