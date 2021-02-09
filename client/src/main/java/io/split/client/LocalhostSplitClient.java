package io.split.client;

import com.google.common.collect.Lists;
import io.split.cache.SplitCache;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.Partition;
import io.split.client.impressions.ImpressionsManager;
import io.split.engine.SDKReadinessGates;
import io.split.engine.evaluator.EvaluatorImp;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.strings.WhitelistMatcher;
import io.split.engine.metrics.Metrics;
import io.split.grammar.Treatments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Codigo in localhost environment.
 *
 * @author adil
 */
public final class LocalhostSplitClient extends SplitClientImpl {
    private static final Logger _log = LoggerFactory.getLogger(LocalhostSplitClient.class);
    private static String LOCALHOST = "localhost";

    public LocalhostSplitClient(Map<SplitAndKey, LocalhostSplit> map, SplitCache splitCache) throws URISyntaxException {
        super(new SplitFactoryImpl(LOCALHOST, SplitClientConfig.builder().build()), splitCache,
                new ImpressionsManager.NoOpImpressionsManager(),  new Metrics.NoopMetrics(), new NoopEventClient(),
                SplitClientConfig.builder().build(), new SDKReadinessGates(), new EvaluatorImp(splitCache));

        checkNotNull(map, "map must not be null");
        updateCache(map);
    }

    @Override
    public void destroy() {
        _splitCache.clear();
    }

    @Override
    public void blockUntilReady() throws TimeoutException, InterruptedException {
        // LocalhostSplitClient is always ready
    }

    public void updateFeatureToTreatmentMap(Map<SplitAndKey, LocalhostSplit> map) {
        if (map  == null) {
            _log.warn("A null map was passed as an update. Ignoring this update.");
            return;
        }
        updateCache(map);
    }

    private void updateCache(Map<SplitAndKey, LocalhostSplit> map) {
        _splitCache.clear();
        for (Map.Entry<SplitAndKey,LocalhostSplit> entrySplit : map.entrySet()) {
            SplitAndKey splitAndKey = entrySplit.getKey();
            String splitName = splitAndKey.split();
            String splitKey = splitAndKey.key();
            LocalhostSplit localhostSplit = entrySplit.getValue();
            ParsedSplit split = _splitCache.get(splitName);
            List<ParsedCondition> conditions = getConditions(splitKey, split, localhostSplit.treatment);
            String treatment = conditions.size() > 0 ? Treatments.CONTROL : localhostSplit.treatment;
            Map<String, String> configurations = new HashMap<>();
            if(split != null && split.configurations().size() > 0) {
                configurations = split.configurations();
            }
            configurations.put(localhostSplit.treatment, localhostSplit.config);

            split = new ParsedSplit(splitName, 0, false, treatment,conditions, LOCALHOST, 0, 100, 0, 0,            configurations);
            _splitCache.put(split);
        }
    }

    private List<ParsedCondition> getConditions(String splitKey, ParsedSplit split, String treatment){
        List<ParsedCondition> conditions = split == null ? new ArrayList<>() : split.parsedConditions().stream().collect(Collectors.toList());
        Partition partition = new Partition();
        partition.treatment = treatment;
        partition.size = 100;

        if(splitKey != null) {
            conditions.add(createWhitelistCondition(splitKey, partition));
        }
        else {
            conditions = conditions.stream().filter(pc -> ConditionType.WHITELIST.equals(pc.conditionType())).collect(Collectors.toList());
            conditions.add(createRolloutCondition(partition));
        }
        conditions.sort(Comparator.comparing(ParsedCondition::conditionType));
        return conditions;
    }

    private ParsedCondition createWhitelistCondition(String splitKey, Partition partition) {
        ParsedCondition parsedCondition = new ParsedCondition(ConditionType.WHITELIST,
                new CombiningMatcher(MatcherCombiner.AND,
                        Lists.newArrayList(new AttributeMatcher(null, new WhitelistMatcher(Lists.newArrayList(splitKey)), false))),
                Lists.newArrayList(partition), splitKey);
        return parsedCondition;
    }

    private ParsedCondition createRolloutCondition(Partition partition) {
        Partition rolloutPartition = new Partition();
        rolloutPartition.treatment = "-";
        rolloutPartition.size = 0;
        ParsedCondition parsedCondition = new ParsedCondition(ConditionType.ROLLOUT,
                new CombiningMatcher(MatcherCombiner.AND,
                        Lists.newArrayList(new AttributeMatcher(null,  new AllKeysMatcher(), false))),
                Lists.newArrayList(partition, rolloutPartition), "LOCAL");

        return parsedCondition;
    }
}
