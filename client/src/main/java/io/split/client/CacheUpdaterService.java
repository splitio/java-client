package io.split.client;

import com.google.common.collect.Lists;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.Partition;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.AllKeysMatcher;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.strings.WhitelistMatcher;
import io.split.grammar.Treatments;
import io.split.storages.SplitCacheConsumer;
import io.split.storages.SplitCacheProducer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public final  class CacheUpdaterService {

    private static String LOCALHOST = "localhost";
    private SplitCacheConsumer _splitCacheConsumer;
    private SplitCacheProducer _splitCacheProducer;

    public CacheUpdaterService(SplitCacheConsumer splitCacheConsumer, SplitCacheProducer splitCacheProducer) {
        _splitCacheConsumer = checkNotNull(splitCacheConsumer);
        _splitCacheProducer = checkNotNull(splitCacheProducer);
    }

    public void updateCache(Map<SplitAndKey, LocalhostSplit> map) {
        _splitCacheProducer.clear();
        List<ParsedSplit> parsedSplits = new ArrayList<>();
        for (Map.Entry<SplitAndKey,LocalhostSplit> entrySplit : map.entrySet()) {
            SplitAndKey splitAndKey = entrySplit.getKey();
            String splitName = splitAndKey.split();
            String splitKey = splitAndKey.key();
            LocalhostSplit localhostSplit = entrySplit.getValue();
            Optional<ParsedSplit> splitOptional = parsedSplits.stream().filter(ps -> ps.feature().equals(splitName)).findFirst();
            ParsedSplit split = splitOptional.orElse(null);
            Map<String, String> configurations = new HashMap<>();
            if(split != null && split.configurations().size() > 0) {
                configurations = split.configurations();
            }
            List<ParsedCondition> conditions = getConditions(splitKey, split, localhostSplit.treatment);
            String treatment = conditions.size() > 0 ? Treatments.CONTROL : localhostSplit.treatment;
            configurations.put(localhostSplit.treatment, localhostSplit.config);

            split = new ParsedSplit(splitName, 0, false, treatment,conditions, LOCALHOST, 0, 100, 0, 0, configurations);
            parsedSplits.removeIf(parsedSplit -> parsedSplit.feature().equals(splitName));
            parsedSplits.add(split);
        }
        _splitCacheProducer.putMany(parsedSplits);
        _splitCacheProducer.setChangeNumber(_splitCacheProducer.getChangeNumber());
    }

    private List<ParsedCondition> getConditions(String splitKey, ParsedSplit split, String treatment){
        List<ParsedCondition> conditions = split == null ? new ArrayList<>() : new ArrayList<>(split.parsedConditions());
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
