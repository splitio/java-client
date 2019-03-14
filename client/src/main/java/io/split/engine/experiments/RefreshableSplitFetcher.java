package io.split.engine.experiments;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import io.split.client.dtos.Condition;
import io.split.client.dtos.Matcher;
import io.split.client.dtos.MatcherType;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.engine.SDKReadinessGates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicLong _changeNumber;

    private Map<String, ParsedSplit> _concurrentMap = Maps.newConcurrentMap();

    /**
     * Contains all the traffic types that are currently being used by the splits and also the count
     *
     * For example if there are three splits, one of traffic type "account" and two of traffic type "user",
     * this multiset will contain [{"user", 2}, {"account", 1}
     *
     * The count is needed since it is needed to maintain how many splits are using a traffic type, so when
     * an ARCHIVED split is message, we now if we need to remove a traffic type from the multiset.
     */
    Multiset<String> _concurrentTrafficTypeNameSet = ConcurrentHashMultiset.create();
    private final SDKReadinessGates _gates;

    private final Object _lock = new Object();


    public RefreshableSplitFetcher(SplitChangeFetcher splitChangeFetcher, SplitParser parser, SDKReadinessGates gates) {
        this(splitChangeFetcher, parser, gates, -1);
    }

    /**
     * This constructor is package private because it is meant primarily for unit tests
     * where we want to set the starting change number. All regular clients should use
     * the public constructor.
     *
     * @param splitChangeFetcher   MUST NOT be null
     * @param parser               MUST NOT be null
     * @param startingChangeNumber
     */
    /*package private*/ RefreshableSplitFetcher(SplitChangeFetcher splitChangeFetcher,
                                                SplitParser parser,
                                                SDKReadinessGates gates,
                                                long startingChangeNumber) {
        _splitChangeFetcher = splitChangeFetcher;
        _parser = parser;
        _gates = gates;
        _changeNumber = new AtomicLong(startingChangeNumber);

        checkNotNull(_parser);
        checkNotNull(_splitChangeFetcher);
    }

    @Override
    public void forceRefresh() {
        run();
    }

    public long changeNumber() {
        return _changeNumber.get();
    }


    @Override
    public ParsedSplit fetch(String test) {
        return _concurrentMap.get(test);
    }

    public List<ParsedSplit> fetchAll() {
        return Lists.newArrayList(_concurrentMap.values());
    }

    @Override
    public Set<String> fetchUsedTrafficTypes() {
        // We return the "keys" of the multiset that have a count greater than 0
        // If the multiset has [{"user",2}.{"account",0}], elementSet only returns
        // ["user"] (it ignores "account")
        return Sets.newHashSet(_concurrentTrafficTypeNameSet.elementSet());
    }

    public Collection<ParsedSplit> fetch() {
        return _concurrentMap.values();
    }

    public void clear() {
        _concurrentMap.clear();
        _concurrentTrafficTypeNameSet.clear();
    }

    @Override
    public void run() {
        long start = _changeNumber.get();
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
                _log.debug("split fetch before: " + start + ", after: " + _changeNumber.get());
            }
        }
    }

    public void runWithoutExceptionHandling() throws InterruptedException {
        SplitChange change = _splitChangeFetcher.fetch(_changeNumber.get());

        if (change == null) {
            throw new IllegalStateException("SplitChange was null");
        }

        if (change.till == _changeNumber.get()) {
            // no change.
            return;
        }

        if (change.since != _changeNumber.get()
                || change.till < _changeNumber.get()) {
            // some other thread may have updated the shared state. exit
            return;
        }

        if (change.splits.isEmpty()) {
            // there are no changes. weird!
            _changeNumber.set(change.till);
            return;
        }

        Set<String> segmentsInUse = Sets.newHashSet();


        synchronized (_lock) {
            // check state one more time.
            if (change.since != _changeNumber.get()
                    || change.till < _changeNumber.get()) {
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

                segmentsInUse.addAll(collectSegmentsInUse(split));
                toAdd.put(split.name, parsedSplit);
                if (split.trafficTypeName != null) {
                    trafficTypeNamesToAdd.add(split.trafficTypeName);
                }
            }

            _concurrentMap.putAll(toAdd);
            _concurrentTrafficTypeNameSet.addAll(trafficTypeNamesToAdd);
            //removeAll does not work here, since it wont remove all the occurrences, just one
            Multisets.removeOccurrences(_concurrentTrafficTypeNameSet, trafficTypeNamesToRemove);

            for (String remove : toRemove) {
                _concurrentMap.remove(remove);
            }

            if (!toAdd.isEmpty()) {
                _log.debug("Updated features: " + toAdd.keySet());
            }

            if (!toRemove.isEmpty()) {
                _log.debug("Deleted features: " + toRemove);
            }

            _changeNumber.set(change.till);
        }

    }

    private List<String> collectSegmentsInUse(Split split) {
        List<String> result = Lists.newArrayList();
        for (Condition condition : split.conditions) {
            for (Matcher matcher : condition.matcherGroup.matchers) {
                if (matcher.matcherType == MatcherType.IN_SEGMENT) {
                    if (matcher.userDefinedSegmentMatcherData != null && matcher.userDefinedSegmentMatcherData.segmentName != null) {
                        result.add(matcher.userDefinedSegmentMatcherData.segmentName);
                    }
                }
            }
        }
        return result;
    }
}
