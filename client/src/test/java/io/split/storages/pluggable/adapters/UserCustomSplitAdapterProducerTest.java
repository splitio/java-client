package io.split.storages.pluggable.adapters;

import com.google.common.collect.Lists;
import io.split.client.dtos.Condition;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Split;
import io.split.client.dtos.Status;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitParser;
import io.split.grammar.Treatments;
import io.split.storages.pluggable.CustomStorageWrapper;
import io.split.storages.pluggable.domain.PrefixAdapter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserCustomSplitAdapterProducerTest{

    private static final String SPLIT_NAME = "SplitName";
    private CustomStorageWrapper _customStorageWrapper;

    @Before
    public void setUp() {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
    }

    @Test
    public void testGetChangeNumber() {
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber())).thenReturn(UserCustomSplitAdapterConsumerTest.getLongAsJson(120l));
        Assert.assertEquals(120l, userCustomSplitAdapterProducer.getChangeNumber());
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testPut() {
        //Noop
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        userCustomSplitAdapterProducer.put(Mockito.mock(ParsedSplit.class));
    }

    @Test
    public void testRemove () {
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        Assert.assertTrue(userCustomSplitAdapterProducer.remove(SPLIT_NAME));
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).delete(Mockito.anyObject());
    }

    @Test
    public void testSetChangeNumber()  {
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        userCustomSplitAdapterProducer.setChangeNumber(1l);
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testKill() {
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(UserCustomSplitAdapterConsumerTest.getSplitAsJson(getSplit(SPLIT_NAME)));
        userCustomSplitAdapterProducer.kill(SPLIT_NAME, "DefaultTreatment", 2l);
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testKillSplitNotFound() {
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(null);
        userCustomSplitAdapterProducer.kill(SPLIT_NAME, "DefaultTreatment", 2l);
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
        Mockito.verify(_customStorageWrapper, Mockito.times(0)).set(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testPutMany() {
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        SplitParser splitParser = new SplitParser();
        ParsedSplit parsedSplit = splitParser.parse(getSplit(SPLIT_NAME));
        ParsedSplit parsedSplit2 = splitParser.parse(getSplit(SPLIT_NAME+"2"));
        userCustomSplitAdapterProducer.putMany(Stream.of(parsedSplit, parsedSplit2).collect(Collectors.toList()), 1l);
        Mockito.verify(_customStorageWrapper, Mockito.times(3)).set(Mockito.anyString(), Mockito.anyString());

    }

    @Test
    public void testIncreaseTrafficType() {
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        userCustomSplitAdapterProducer.increaseTrafficType("TrafficType");
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).increment(PrefixAdapter.buildTrafficTypeExists("TrafficType"), 1);
    }

    @Test
    public void testDecreaseTrafficType() {
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        userCustomSplitAdapterProducer.decreaseTrafficType("TrafficType");
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).decrement(PrefixAdapter.buildTrafficTypeExists("TrafficType"), 1);
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).delete(Mockito.anyObject());

    }

    @Test
    public void testClear() {
        //Noop
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        userCustomSplitAdapterProducer.clear();
    }

    @Test
    public void testGetSegments() {
        //NoOp
        UserCustomSplitAdapterProducer userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        Assert.assertEquals(0, userCustomSplitAdapterProducer.getSegments().size());
    }



    private Split getSplit(String name) {
        Condition condition = ConditionsTestUtil.makeUserDefinedSegmentCondition(ConditionType.ROLLOUT, "Segment", Lists.newArrayList(ConditionsTestUtil.partition("on", 10)));
        Split split = new Split();
        split.status = Status.ACTIVE;
        split.trafficAllocation = 100;
        split.trafficAllocationSeed = (int) 12l;
        split.seed = (int) 12l;
        split.conditions = Lists.newArrayList(condition);
        split.name = name;
        split.defaultTreatment = Treatments.OFF;
        split.changeNumber = 12l;
        return split;
    }

}