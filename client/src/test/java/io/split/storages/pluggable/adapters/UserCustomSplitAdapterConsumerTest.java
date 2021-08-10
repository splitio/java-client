package io.split.storages.pluggable.adapters;

import com.google.common.collect.Lists;
import io.split.client.dtos.*;
import io.split.client.utils.Json;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserCustomSplitAdapterConsumerTest {

    private static final String SPLIT_NAME = "SplitName";
    private CustomStorageWrapper _customStorageWrapper;

    @Before
    public void setUp() {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
    }

    @Test
    public void testGetChangeNumber() {
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber())).thenReturn(getLongAsJson(120l));
        Assert.assertEquals(120l, userCustomSplitAdapterConsumer.getChangeNumber());
        Mockito.verify(_customStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testGetSplit() {
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
         SplitParser splitParser = new SplitParser();
        Split split = getSplit(SPLIT_NAME);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(getSplitAsJson(split));
        ParsedSplit result = userCustomSplitAdapterConsumer.get(SPLIT_NAME);
        ParsedSplit expected = splitParser.parse(split);
        Assert.assertTrue(expected.equals(result));
    }

    @Test
    public void testGetSplitNotFound() {
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(null);
        ParsedSplit result = userCustomSplitAdapterConsumer.get(SPLIT_NAME);
        Assert.assertNull(result);
    }

    @Test
    public void testGetAll(){
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        SplitParser splitParser = new SplitParser();
        Split split = getSplit(SPLIT_NAME);
        Split split2 = getSplit(SPLIT_NAME+"2");
        List<Split> listResultExpected = Stream.of(split, split2).collect(Collectors.toList());
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildGetAllSplit())).
                thenReturn(getListOfSplitsAsJson(listResultExpected));
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) userCustomSplitAdapterConsumer.getAll();
        List<ParsedSplit> splitsExpected = Stream.of(splitParser.parse(split), splitParser.parse(split2)).collect(Collectors.toList());
        Assert.assertNotNull(splitsResult);
        Assert.assertEquals(splitsExpected.size(), splitsResult.size());
    }

    @Test
    public void testGetAllNoResults() {
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildGetAllSplit())).
                thenReturn(getListOfSplitsAsJson(new ArrayList<>()));
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) userCustomSplitAdapterConsumer.getAll();
        Assert.assertEquals(0, splitsResult.size());
    }

    @Test
    public void testTrafficTypeExists() {
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Mockito.when(_customStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists("TrafficType"))).
                thenReturn(getBooleanAsJson(true));
        boolean result = userCustomSplitAdapterConsumer.trafficTypeExists("TrafficType");
        Assert.assertTrue(result);
    }

    @Test
    public void testFetchMany(){
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Split split = getSplit(SPLIT_NAME);
        Split split2 = getSplit(SPLIT_NAME+"2");
        List<Split> listResultExpected = Stream.of(split, split2).collect(Collectors.toList());
        Mockito.when(_customStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList())))).
                thenReturn(getListOfSplitsAsJson(listResultExpected));
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) userCustomSplitAdapterConsumer.fetchMany(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList()));
        Assert.assertNotNull(splitsResult);
        Assert.assertEquals(2, splitsResult.size());
    }

    @Test
    public void testFetchManyNotFound(){
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Split split = getSplit(SPLIT_NAME);
        Split split2 = getSplit(SPLIT_NAME+"2");
        List<Split> listResultExpected = Stream.of(split, split2).collect(Collectors.toList());
        Mockito.when(_customStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList())))).
                thenReturn(getListOfSplitsAsJson(new ArrayList<>()));
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) userCustomSplitAdapterConsumer.fetchMany(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList()));
        Assert.assertNotNull(splitsResult);
        Assert.assertEquals(0, splitsResult.size());
    }

    @Test
    public void testGetSegments() {
        //NoOp
        UserCustomSplitAdapterConsumer userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Assert.assertEquals(0, userCustomSplitAdapterConsumer.getSegments().size());
    }

    public static String getLongAsJson(long value) {
        return Json.toJson(value);
    }

    public static  String getSplitAsJson(Split split) {
        return Json.toJson(split);
    }

    public static  String getListOfSplitsAsJson(List<Split> splitList) {
        return Json.toJson(splitList);
    }

    public static  String getBooleanAsJson(boolean result) {
        return Json.toJson(result);
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