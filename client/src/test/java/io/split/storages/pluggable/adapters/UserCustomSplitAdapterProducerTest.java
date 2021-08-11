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
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserCustomSplitAdapterProducerTest{

    private static final String SPLIT_NAME = "SplitName";
    private CustomStorageWrapper _customStorageWrapper;
    private SafeUserStorageWrapper _safeUserStorageWrapper;
    private UserCustomSplitAdapterProducer _userCustomSplitAdapterProducer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _safeUserStorageWrapper = Mockito.mock(SafeUserStorageWrapper.class);
        _userCustomSplitAdapterProducer = new UserCustomSplitAdapterProducer(_customStorageWrapper);
        Field userCustomSplitAdapterProducer = UserCustomSplitAdapterProducer.class.getDeclaredField("_safeUserStorageWrapper");
        userCustomSplitAdapterProducer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userCustomSplitAdapterProducer, userCustomSplitAdapterProducer.getModifiers() & ~Modifier.FINAL);
        userCustomSplitAdapterProducer.set(_userCustomSplitAdapterProducer, _safeUserStorageWrapper);
    }

    @Test
    public void testGetChangeNumber() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber())).thenReturn(UserCustomSplitAdapterConsumerTest.getLongAsJson(120L));
        Assert.assertEquals(120L, _userCustomSplitAdapterProducer.getChangeNumber());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testGetChangeNumberWithWrapperFailing() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber())).thenReturn(null);
        Assert.assertEquals(0L, _userCustomSplitAdapterProducer.getChangeNumber());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testRemove() {
        Split split = getSplit(SPLIT_NAME);
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME)))
                .thenReturn(UserCustomSplitAdapterConsumerTest.getSplitAsJson(split));
        _userCustomSplitAdapterProducer.remove(SPLIT_NAME);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(2)).delete(Mockito.anyObject());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).decrement(Mockito.anyObject(), Mockito.anyLong());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testRemoveWithWrapperFailing() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME)))
                .thenReturn(null);
        Assert.assertFalse(_userCustomSplitAdapterProducer.remove(SPLIT_NAME));
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(0)).delete(Mockito.anyObject());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(0)).decrement(Mockito.anyObject(), Mockito.anyLong());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testSetChangeNumber()  {
        _userCustomSplitAdapterProducer.setChangeNumber(1L);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testKill() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(UserCustomSplitAdapterConsumerTest.getSplitAsJson(getSplit(SPLIT_NAME)));
        _userCustomSplitAdapterProducer.kill(SPLIT_NAME, "DefaultTreatment", 2L);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testKillSplitNotFound() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(null);
        _userCustomSplitAdapterProducer.kill(SPLIT_NAME, "DefaultTreatment", 2L);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(0)).set(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testPutMany() {
        SplitParser splitParser = new SplitParser();
        ParsedSplit parsedSplit = splitParser.parse(getSplit(SPLIT_NAME));
        ParsedSplit parsedSplit2 = splitParser.parse(getSplit(SPLIT_NAME+"2"));
        _userCustomSplitAdapterProducer.putMany(Stream.of(parsedSplit, parsedSplit2).collect(Collectors.toList()), 1L);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(3)).set(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(2)).increment(Mockito.anyString(), Mockito.anyLong());

    }

    @Test
    public void testIncreaseTrafficType() {
        _userCustomSplitAdapterProducer.increaseTrafficType("TrafficType");
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).increment(PrefixAdapter.buildTrafficTypeExists("TrafficType"), 1);
    }

    @Test
    public void testDecreaseTrafficType() {
        _userCustomSplitAdapterProducer.decreaseTrafficType("TrafficType");
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).decrement(PrefixAdapter.buildTrafficTypeExists("TrafficType"), 1);
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).delete(Mockito.anyObject());

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
        split.trafficAllocationSeed = (int) 12L;
        split.seed = (int) 12L;
        split.conditions = Lists.newArrayList(condition);
        split.name = name;
        split.defaultTreatment = Treatments.OFF;
        split.changeNumber = 12L;
        split.trafficTypeName = "TrafficType";
        return split;
    }

}