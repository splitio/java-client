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
import io.split.storages.pluggable.domain.SafeUserStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserCustomSplitAdapterConsumerTest {

    private static final String SPLIT_NAME = "SplitName";
    private CustomStorageWrapper _customStorageWrapper;
    private SafeUserStorageWrapper _safeUserStorageWrapper;
    private UserCustomSplitAdapterConsumer _userCustomSplitAdapterConsumer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _safeUserStorageWrapper = Mockito.mock(SafeUserStorageWrapper.class);
        _userCustomSplitAdapterConsumer = new UserCustomSplitAdapterConsumer(_customStorageWrapper);
        Field userCustomSplitAdapterConsumer = UserCustomSplitAdapterConsumer.class.getDeclaredField("_safeUserStorageWrapper");
        userCustomSplitAdapterConsumer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userCustomSplitAdapterConsumer, userCustomSplitAdapterConsumer.getModifiers() & ~Modifier.FINAL);
        userCustomSplitAdapterConsumer.set(_userCustomSplitAdapterConsumer, _safeUserStorageWrapper);
    }

    @Test
    public void testGetChangeNumber() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber())).thenReturn(getLongAsJson(120L));
        Assert.assertEquals(120L, _userCustomSplitAdapterConsumer.getChangeNumber());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testGetChangeNumberWithWrapperFailing() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber())).thenReturn(null);
        Assert.assertEquals(-1L, _userCustomSplitAdapterConsumer.getChangeNumber());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testGetChangeNumberWithGsonFailing() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitChangeNumber())).thenReturn("a");
        Assert.assertEquals(-1L, _userCustomSplitAdapterConsumer.getChangeNumber());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testGetSplit() {
        SplitParser splitParser = new SplitParser();
        Split split = getSplit(SPLIT_NAME);
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(getSplitAsJson(split));
        ParsedSplit result = _userCustomSplitAdapterConsumer.get(SPLIT_NAME);
        ParsedSplit expected = splitParser.parse(split);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetSplitNotFound() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildSplitKey(SPLIT_NAME))).thenReturn(null);
        ParsedSplit result = _userCustomSplitAdapterConsumer.get(SPLIT_NAME);
        Assert.assertNull(result);
    }

    @Test
    public void testGetAll(){
        Split split = getSplit(SPLIT_NAME);
        Split split2 = getSplit(SPLIT_NAME+"2");
        List<Split> listResultExpected = Stream.of(split, split2).collect(Collectors.toList());
        Set<String> keysResult = Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toSet());
        Mockito.when(_safeUserStorageWrapper.getKeysByPrefix(Mockito.anyObject())).
                thenReturn(keysResult);
        List<String> getManyExpected = Stream.of(Json.toJson(split), Json.toJson(split2)).collect(Collectors.toList());
        Mockito.when(_safeUserStorageWrapper.getMany(Mockito.anyObject())).
                thenReturn(getManyExpected);
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) _userCustomSplitAdapterConsumer.getAll();
        Assert.assertNotNull(splitsResult);
        Assert.assertEquals(listResultExpected.size(), splitsResult.size());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).getKeysByPrefix(Mockito.anyString());
        Mockito.verify(_safeUserStorageWrapper, Mockito.times(1)).getMany(Mockito.anyObject());
    }

    @Test
    public void testGetAllWithWrapperFailing(){
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildGetAllSplit())).
                thenReturn(null);
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) _userCustomSplitAdapterConsumer.getAll();
        Assert.assertNotNull(splitsResult);
        Assert.assertEquals(0, splitsResult.size());
    }

    @Test
    public void testGetAllNullOnWrappers() {
        Mockito.when(_safeUserStorageWrapper.getKeysByPrefix(PrefixAdapter.buildGetAllSplit())).
                thenReturn(null);
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) _userCustomSplitAdapterConsumer.getAll();
        Assert.assertEquals(0, splitsResult.size());
    }

    @Test
    public void testGetAllNullOnGetMany() {
        Set<String> keysResult = Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toSet());
        Mockito.when(_safeUserStorageWrapper.getKeysByPrefix(Mockito.anyObject())).
                thenReturn(keysResult);
        Mockito.when(_safeUserStorageWrapper.getMany(Mockito.anyObject())).
                thenReturn(null);
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) _userCustomSplitAdapterConsumer.getAll();
        Assert.assertEquals(0, splitsResult.size());
    }


    @Test
    public void testTrafficTypeExists() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists("TrafficType"))).
                thenReturn(getBooleanAsJson(true));
        boolean result = _userCustomSplitAdapterConsumer.trafficTypeExists("TrafficType");
        Assert.assertTrue(result);
    }

    @Test
    public void testTrafficTypeExistsWithWrapperFailing() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists("TrafficType"))).
                thenReturn(null);
        boolean result = _userCustomSplitAdapterConsumer.trafficTypeExists("TrafficType");
        Assert.assertFalse(result);
    }

    @Test
    public void testTrafficTypeExistsWithGsonFailing() {
        Mockito.when(_safeUserStorageWrapper.get(PrefixAdapter.buildTrafficTypeExists("TrafficType"))).
                thenReturn("2");
        boolean result = _userCustomSplitAdapterConsumer.trafficTypeExists("TrafficType");
        Assert.assertFalse(result);
    }

    @Test
    public void testFetchMany(){
        Split split = getSplit(SPLIT_NAME);
        Split split2 = getSplit(SPLIT_NAME+"2");
        List<String> listResultExpected = Stream.of(Json.toJson(split), Json.toJson(split2)).collect(Collectors.toList());
        Mockito.when(_safeUserStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList())))).
                thenReturn(listResultExpected);
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) _userCustomSplitAdapterConsumer.fetchMany(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList()));
        Assert.assertNotNull(splitsResult);
        Assert.assertEquals(2, splitsResult.size());
    }

    @Test
    public void testFetchManyWithWrapperFailing(){
        Mockito.when(_safeUserStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList())))).
                thenReturn(null);
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) _userCustomSplitAdapterConsumer.fetchMany(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList()));
        Assert.assertNotNull(splitsResult);
        Assert.assertEquals(0, splitsResult.size());
    }

    @Test
    public void testFetchManyNotFound(){
        Mockito.when(_safeUserStorageWrapper.getItems(PrefixAdapter.buildFetchManySplits(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList())))).
                thenReturn(null);
        List<ParsedSplit> splitsResult = (List<ParsedSplit>) _userCustomSplitAdapterConsumer.fetchMany(Stream.of(SPLIT_NAME, SPLIT_NAME+"2").collect(Collectors.toList()));
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
        split.trafficAllocationSeed = (int) 12L;
        split.seed = (int) 12L;
        split.conditions = Lists.newArrayList(condition);
        split.name = name;
        split.defaultTreatment = Treatments.OFF;
        split.changeNumber = 12L;
        return split;
    }
}