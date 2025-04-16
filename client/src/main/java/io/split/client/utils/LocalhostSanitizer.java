package io.split.client.utils;

import com.google.common.base.Preconditions;
import io.split.client.dtos.Condition;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.KeySelector;
import io.split.client.dtos.Matcher;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.MatcherGroup;
import io.split.client.dtos.MatcherType;
import io.split.client.dtos.Partition;
import io.split.client.dtos.SegmentChange;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.client.dtos.WhitelistMatcherData;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class LocalhostSanitizer {
    private static final String DEFAULT_RULE = "default rule";

    private LocalhostSanitizer() {
        throw new IllegalStateException("Utility class");
    }

    public static SplitChange sanitization(SplitChange splitChange) {
        SecureRandom random = new SecureRandom();
        List<Split> splitsToRemove = new ArrayList<>();
        if (splitChange.featureFlags.t < LocalhostConstants.DEFAULT_TS || splitChange.featureFlags.t == 0) {
            splitChange.featureFlags.t = LocalhostConstants.DEFAULT_TS;
        }
        if (splitChange.featureFlags.s < LocalhostConstants.DEFAULT_TS || splitChange.featureFlags.s > splitChange.featureFlags.t) {
            splitChange.featureFlags.s = splitChange.featureFlags.t;
        }
        if (splitChange.featureFlags.d != null) {
            for (Split split: splitChange.featureFlags.d) {
                if (split.name == null){
                    splitsToRemove.add(split);
                    continue;
                }
                if (split.trafficTypeName == null || split.trafficTypeName.isEmpty()) {
                    split.trafficTypeName = LocalhostConstants.USER;
                }
                if (split.trafficAllocation == null || split.trafficAllocation < 0 || split.trafficAllocation > LocalhostConstants.SIZE_100) {
                    split.trafficAllocation = LocalhostConstants.SIZE_100;
                }
                if (split.trafficAllocationSeed == null || split.trafficAllocationSeed == 0) {
                    split.trafficAllocationSeed = - random.nextInt(10) * LocalhostConstants.MILLI_SECONDS;
                }
                if (split.seed == 0) {
                    split.seed = - random.nextInt(10) * LocalhostConstants.MILLI_SECONDS;
                }
                if (split.status == null || split.status != Status.ACTIVE && split.status != Status.ARCHIVED) {
                    split.status = Status.ACTIVE;
                }
                if (split.defaultTreatment == null || split.defaultTreatment.isEmpty()) {
                    split.defaultTreatment = LocalhostConstants.CONTROL;
                }
                if (split.changeNumber < 0) {
                    split.changeNumber = 0;
                }
                if (split.algo != LocalhostConstants.ALGO){
                    split.algo = LocalhostConstants.ALGO;
                }
                if (split.conditions == null) {
                    split.conditions = new ArrayList<>();
                }

                Condition condition = new Condition();
                if (!split.conditions.isEmpty()){
                     condition = split.conditions.get(split.conditions.size() - 1);
                }

                if (split.conditions.isEmpty() || !condition.conditionType.equals(ConditionType.ROLLOUT) ||
                        condition.matcherGroup.matchers == null ||
                        condition.matcherGroup.matchers.isEmpty() ||
                        !condition.matcherGroup.matchers.get(0).matcherType.equals(MatcherType.ALL_KEYS)) {
                    Condition rolloutCondition = new Condition();
                    split.conditions.add(createRolloutCondition(rolloutCondition, split.trafficTypeName, null));
                }
            }
            splitChange.featureFlags.d.removeAll(splitsToRemove);
            return splitChange;
        }
        splitChange.featureFlags.d = new ArrayList<>();
        return splitChange;
    }
     public static SegmentChange sanitization(SegmentChange segmentChange) {
        if (segmentChange.name == null || segmentChange.name.isEmpty()) {
            return null;
        }
        if (segmentChange.added == null) {
            segmentChange.added =  new ArrayList<>();
        }
        if (segmentChange.removed == null) {
            segmentChange.removed = new ArrayList<>();
        }
        List<String> addedToRemoved = segmentChange.added.stream().filter(add -> segmentChange.removed.contains(add)).collect(Collectors.toList());
        segmentChange.removed.removeAll(addedToRemoved);

        if (segmentChange.till < LocalhostConstants.DEFAULT_TS || segmentChange.till == 0){
            segmentChange.till = LocalhostConstants.DEFAULT_TS;
        }
        if (segmentChange.since < LocalhostConstants.DEFAULT_TS || segmentChange.since > segmentChange.till) {
            segmentChange.since = segmentChange.till;
        }
        return segmentChange;
    }

    public static Condition createRolloutCondition(Condition condition, String trafficType, String treatment) {
        condition.conditionType = ConditionType.ROLLOUT;
        condition.matcherGroup = new MatcherGroup();
        condition.matcherGroup.combiner = MatcherCombiner.AND;
        Matcher matcher = new Matcher();
        KeySelector keySelector = new KeySelector();
        keySelector.trafficType = trafficType;

        matcher.keySelector = keySelector;
        matcher.matcherType = MatcherType.ALL_KEYS;
        matcher.negate = false;

        condition.matcherGroup.matchers = new ArrayList<>();
        condition.matcherGroup.matchers.add(matcher);

        condition.partitions = new ArrayList<>();
        Partition partition1 = new Partition();
        Partition partition2 = new Partition();
        partition1.size = LocalhostConstants.SIZE_100;
        partition2.size = LocalhostConstants.SIZE_0;
        if (treatment != null) {
            partition1.treatment = treatment;
        } else {
            partition1.treatment = LocalhostConstants.TREATMENT_OFF;
            partition2.treatment = LocalhostConstants.TREATMENT_ON;
        }
        condition.partitions.add(partition1);
        condition.partitions.add(partition2);
        condition.label = DEFAULT_RULE;

        return condition;
    }

    public static Condition createCondition(Object keyOrKeys, String treatment) {
        Condition condition = new Condition();
        if (keyOrKeys == null) {
            return LocalhostSanitizer.createRolloutCondition(condition, "user", treatment);
        } else {
            if (keyOrKeys instanceof String) {
                List keys = new ArrayList<>();
                keys.add(keyOrKeys);
                return createWhitelistCondition(condition, "user", treatment, keys);
            } else {
                Preconditions.checkArgument(keyOrKeys instanceof List, "'keys' is not a String nor a List.");
                return createWhitelistCondition(condition, "user", treatment, (List<String>) keyOrKeys);
            }
        }
    }

    public static Condition createWhitelistCondition(Condition condition, String trafficType, String treatment, List<String> keys) {
        condition.conditionType = ConditionType.WHITELIST;
        condition.matcherGroup = new MatcherGroup();
        condition.matcherGroup.combiner = MatcherCombiner.AND;
        Matcher matcher = new Matcher();
        KeySelector keySelector = new KeySelector();
        keySelector.trafficType = trafficType;

        matcher.keySelector = keySelector;
        matcher.matcherType = MatcherType.WHITELIST;
        matcher.negate = false;
        matcher.whitelistMatcherData = new WhitelistMatcherData();
        matcher.whitelistMatcherData.whitelist = new ArrayList<>(keys);

        condition.matcherGroup.matchers = new ArrayList<>();
        condition.matcherGroup.matchers.add(matcher);

        condition.partitions = new ArrayList<>();
        Partition partition1 = new Partition();
        Partition partition2 = new Partition();
        partition1.size = LocalhostConstants.SIZE_100;
        partition2.size = LocalhostConstants.SIZE_0;
        if (treatment != null) {
            partition1.treatment = treatment;
        } else {
            partition1.treatment = LocalhostConstants.TREATMENT_OFF;
            partition2.treatment = LocalhostConstants.TREATMENT_ON;
        }
        condition.partitions.add(partition1);
        condition.partitions.add(partition2);
        condition.label = "default rule";

        return condition;
    }
}