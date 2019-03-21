package io.split.engine.experiments;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.split.client.dtos.Condition;
import io.split.client.dtos.Split;
import io.split.client.dtos.SplitChange;
import io.split.client.dtos.Status;
import io.split.engine.ConditionsTestUtil;
import io.split.grammar.Treatments;

import java.util.List;
import java.util.Map;

/**
 * Mock Class of SplitChangeFetcher for testing.
 *
 * Every time you run this inside RefreshableSplitFetcher it will add +1 to the since.
 * So the first time you run this, RefreshableSplitFetcher will send since -1 and next time will be run with 0
 * So next time you run it, it will receive since 0 and will set to 1
 * Next 1 and prepare for 2, etc.
 *
 * This is important since you can mock the changes that it will return given a specific since
 * with addSplitForSince and removeSplitForSince.
 * With those methods you can mock what changes (ACTIVE and ARCHIVED) are goint to be returned for since -1, 0, etc
 *
 */
public class SplitChangeFetcherWithTrafficTypeNames implements SplitChangeFetcher {

    private final Map<Long, List<Split>> _trafficTypesToAdd = Maps.newHashMap();
    private final Map<Long, List<Split>> _trafficTypesToRemove = Maps.newHashMap();

    public SplitChangeFetcherWithTrafficTypeNames() { }

    public void addSplitForSince(Long since, String name, String trafficTypeName) {
        modifyTrafficTypeMap(_trafficTypesToAdd, since, name, trafficTypeName, Status.ACTIVE);
    }

    public void removeSplitForSince(Long since, String name, String trafficTypeName) {
        modifyTrafficTypeMap(_trafficTypesToRemove, since, name, trafficTypeName, Status.ARCHIVED);
    }

    @Override
    public SplitChange fetch(long since) {
        long latestChangeNumber = since + 1;

        SplitChange splitChange = new SplitChange();
        splitChange.splits = Lists.newArrayList();
        splitChange.since = since;
        splitChange.till = latestChangeNumber;

        if (_trafficTypesToAdd.get(since) != null) {
            splitChange.splits.addAll(_trafficTypesToAdd.get(since));
        }

        if (_trafficTypesToRemove.get(since) != null) {
            splitChange.splits.addAll(_trafficTypesToRemove.get(since));
        }
        return splitChange;
    }

    private void modifyTrafficTypeMap(Map<Long, List<Split>> map, Long since, String name, String trafficTypeName, Status status) {
        List<Split> splits = map.get(since);
        if (splits == null) {
            splits = Lists.newArrayList();
        }
        splits.add(stubSplit(name, trafficTypeName, status, since));
        map.put(since, splits);
    }

    private Split stubSplit(String name, String trafficTypeName, Status status, Long changeNumber) {
        Split add = new Split();
        Condition condition = ConditionsTestUtil.makeAllKeysCondition(Lists.newArrayList(ConditionsTestUtil.partition("on", 10)));
        add.status = status;
        add.trafficAllocation = 100;
        add.trafficAllocationSeed = changeNumber.intValue();
        add.seed = changeNumber.intValue();
        add.conditions = Lists.newArrayList(condition);
        add.name = name;
        add.trafficTypeName = trafficTypeName;
        add.defaultTreatment = Treatments.OFF;
        add.changeNumber = changeNumber;
        return add;
    }
}
