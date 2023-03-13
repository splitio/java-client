package io.split.client.utils;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class LocalhostSanitizer {

    private static final int MILLI_SECONDS = 1000;
    private static final int ALGO = 2;
    private static final int TRAFFIC_ALLOCATION_LIMIT = 100;
    private static final String TREATMENT_ON = "on";
    private static final String TREATMENT_OFF = "off";
    private static final String DEFAULT_RULE = "default rule";
    private static final String TRAFFIC_TYPE_USER = "user";

    public static SplitChange sanitization(SplitChange splitChange) {
        Random random = new Random();
        List<Split> splitsToRemove = new ArrayList<>();
        if (splitChange.till < -1 || splitChange.till == 0) {
            splitChange.till = -1L;
        }
        if (splitChange.since < -1 || splitChange.since > splitChange.till) {
            splitChange.since = splitChange.till;
        }
        if (splitChange.splits != null) {
            for (Split split: splitChange.splits) {
                if (split.name == null){
                    splitsToRemove.add(split);
                    continue;
                }
                if (split.trafficTypeName == null || split.trafficTypeName.isEmpty()) {
                    split.trafficTypeName = TRAFFIC_TYPE_USER;
                }
                if (split.trafficAllocation == null || split.trafficAllocation < 0 || split.trafficAllocation > TRAFFIC_ALLOCATION_LIMIT) {
                    split.trafficAllocation = TRAFFIC_ALLOCATION_LIMIT;
                }
                if (split.trafficAllocationSeed == null || split.trafficAllocationSeed == 0) {
                    split.trafficAllocationSeed = new Integer(- random.nextInt(10) * MILLI_SECONDS) ;
                }
                if (split.seed == 0) {
                    split.seed = - random.nextInt(10) * MILLI_SECONDS;
                }
                if (split.status == null || split.status != Status.ACTIVE && split.status != Status.ARCHIVED) {
                    split.status = Status.ACTIVE;
                }
                if (split.defaultTreatment == null || split.defaultTreatment.isEmpty()) {
                    split.defaultTreatment = TREATMENT_ON;
                }
                if (split.changeNumber < 0) {
                    split.changeNumber = 0;
                }
                if (split.algo != ALGO){
                    split.algo = ALGO;
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
                    split.conditions.add(createRolloutCondition(rolloutCondition, split.trafficTypeName));
                }
            }
            splitChange.splits.removeAll(splitsToRemove);
            return splitChange;
        }
        splitChange.splits = new ArrayList<>();
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

        if (segmentChange.till <-1 || segmentChange.till == 0){
            segmentChange.till = -1L;
        }
        if (segmentChange.since < -1 || segmentChange.since > segmentChange.till) {
            segmentChange.since = segmentChange.till;
        }
        return segmentChange;
    }

    private static Condition createRolloutCondition(Condition condition, String trafficType) {
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

        Partition partitionOn = new Partition();
        partitionOn.treatment = TREATMENT_ON;
        partitionOn.size = 0;
        Partition partitionOff = new Partition();
        partitionOff.treatment = TREATMENT_OFF;
        partitionOff.size = 100;

        condition.partitions = new ArrayList<>();
        condition.partitions.add(partitionOn);
        condition.partitions.add(partitionOff);

        condition.label = DEFAULT_RULE;

        return condition;
    }
}
