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
 * Created on 3/12/19.
 */
public class SplitChangeFetcherWIthTrafficTypeNames implements SplitChangeFetcher {

    private final Map<Long, List<Split>> _trafficTypesToAdd = Maps.newHashMap();
    private final Map<Long, List<Split>> _trafficTypesToRemove = Maps.newHashMap();

    public SplitChangeFetcherWIthTrafficTypeNames() { }

    public void addSplitForSince(Long since, String name, String trafficTypeName) {
        List<Split> splits = _trafficTypesToAdd.get(since);
        if (splits == null) {
            splits = Lists.newArrayList();
        }
        splits.add(stubSplit(name, trafficTypeName, Status.ACTIVE, since));
        _trafficTypesToAdd.put(since, splits);
    }

    public void removeSplitForSince(Long since, String name, String trafficTypeName) {
        List<Split> splits = _trafficTypesToRemove.get(since);
        if (splits == null) {
            splits = Lists.newArrayList();
        }
        splits.add(stubSplit(name, trafficTypeName, Status.ARCHIVED, since));
        _trafficTypesToRemove.put(since, splits);
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
}
