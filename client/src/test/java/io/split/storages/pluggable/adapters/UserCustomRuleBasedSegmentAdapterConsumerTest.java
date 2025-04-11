package io.split.storages.pluggable.adapters;

import com.google.common.collect.Lists;
import io.split.client.dtos.*;
import io.split.client.utils.Json;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.experiments.*;
import io.split.storages.pluggable.domain.PrefixAdapter;
import io.split.storages.pluggable.domain.UserStorageWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pluggable.CustomStorageWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.split.TestHelper.makeRuleBasedSegment;

public class UserCustomRuleBasedSegmentAdapterConsumerTest {

    private static final String RULE_BASED_SEGMENT_NAME = "RuleBasedSegmentName";
    private CustomStorageWrapper _customStorageWrapper;
    private UserStorageWrapper _userStorageWrapper;
    private UserCustomRuleBasedSegmentAdapterConsumer _userCustomRuleBasedSegmentAdapterConsumer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        _customStorageWrapper = Mockito.mock(CustomStorageWrapper.class);
        _userStorageWrapper = Mockito.mock(UserStorageWrapper.class);
        _userCustomRuleBasedSegmentAdapterConsumer = new UserCustomRuleBasedSegmentAdapterConsumer(_customStorageWrapper);
        Field userCustomRuleBasedSegmentAdapterConsumer = UserCustomRuleBasedSegmentAdapterConsumer.class.getDeclaredField("_userStorageWrapper");
        userCustomRuleBasedSegmentAdapterConsumer.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(userCustomRuleBasedSegmentAdapterConsumer, userCustomRuleBasedSegmentAdapterConsumer.getModifiers() & ~Modifier.FINAL);
        userCustomRuleBasedSegmentAdapterConsumer.set(_userCustomRuleBasedSegmentAdapterConsumer, _userStorageWrapper);
    }

    @Test
    public void testGetChangeNumber() {
        Mockito.when(_userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentChangeNumber())).thenReturn(getLongAsJson(120L));
        Assert.assertEquals(120L, _userCustomRuleBasedSegmentAdapterConsumer.getChangeNumber());
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testGetChangeNumberWithWrapperFailing() {
        Mockito.when(_userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentChangeNumber())).thenReturn(null);
        Assert.assertEquals(-1L, _userCustomRuleBasedSegmentAdapterConsumer.getChangeNumber());
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testGetChangeNumberWithGsonFailing() {
        Mockito.when(_userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentChangeNumber())).thenReturn("a");
        Assert.assertEquals(-1L, _userCustomRuleBasedSegmentAdapterConsumer.getChangeNumber());
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).get(Mockito.anyString());
    }

    @Test
    public void testGetRuleBasedSegment() {
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = getRuleBasedSegment(RULE_BASED_SEGMENT_NAME);
        Mockito.when(_userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentKey(RULE_BASED_SEGMENT_NAME))).thenReturn(getRuleBasedSegmentAsJson(ruleBasedSegment));
        ParsedRuleBasedSegment result = _userCustomRuleBasedSegmentAdapterConsumer.get(RULE_BASED_SEGMENT_NAME);
        ParsedRuleBasedSegment expected = ruleBasedSegmentParser.parse(ruleBasedSegment);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetRuleBasedSegmentNotFound() {
        Mockito.when(_userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentKey(RULE_BASED_SEGMENT_NAME))).thenReturn(null);
        Mockito.when(_userStorageWrapper.get(PrefixAdapter.buildRuleBasedSegmentKey(RULE_BASED_SEGMENT_NAME))).thenReturn(null);
        ParsedRuleBasedSegment result = _userCustomRuleBasedSegmentAdapterConsumer.get(RULE_BASED_SEGMENT_NAME);
        Assert.assertNull(result);
    }

    @Test
    public void testGetAll() {
        RuleBasedSegment ruleBasedSegment = getRuleBasedSegment(RULE_BASED_SEGMENT_NAME);
        RuleBasedSegment ruleBasedSegment2 = getRuleBasedSegment(RULE_BASED_SEGMENT_NAME+"2");
        List<RuleBasedSegment> listResultExpected = Stream.of(ruleBasedSegment, ruleBasedSegment2).collect(Collectors.toList());
        Set<String> keysResult = Stream.of(RULE_BASED_SEGMENT_NAME, RULE_BASED_SEGMENT_NAME+"2").collect(Collectors.toSet());
        Mockito.when(_userStorageWrapper.getKeysByPrefix(Mockito.anyObject())).
                thenReturn(keysResult);
        List<String> getManyExpected = Stream.of(Json.toJson(ruleBasedSegment), Json.toJson(ruleBasedSegment2)).collect(Collectors.toList());
        Mockito.when(_userStorageWrapper.getMany(Mockito.anyObject())).
                thenReturn(getManyExpected);
        List<ParsedRuleBasedSegment> ruleBasedSegmentsResult = (List<ParsedRuleBasedSegment>) _userCustomRuleBasedSegmentAdapterConsumer.getAll();
        Assert.assertNotNull(ruleBasedSegmentsResult);
        Assert.assertEquals(listResultExpected.size(), ruleBasedSegmentsResult.size());
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).getKeysByPrefix(Mockito.anyString());
        Mockito.verify(_userStorageWrapper, Mockito.times(1)).getMany(Mockito.anyObject());
    }

    @Test
    public void testGetAllWithWrapperFailing() {
        Mockito.when(_userStorageWrapper.get(PrefixAdapter.buildGetAllSplit())).
                thenReturn(null);
        List<ParsedRuleBasedSegment> ruleBasedSegmentsResult = (List<ParsedRuleBasedSegment>) _userCustomRuleBasedSegmentAdapterConsumer.getAll();
        Assert.assertNotNull(ruleBasedSegmentsResult);
        Assert.assertEquals(0, ruleBasedSegmentsResult.size());
    }

    @Test
    public void testGetAllNullOnWrappers() {
        Mockito.when(_userStorageWrapper.getKeysByPrefix(PrefixAdapter.buildGetAllRuleBasedSegment())).
                thenReturn(null);
        List<ParsedRuleBasedSegment> ruleBasedSegmentsResult = (List<ParsedRuleBasedSegment>) _userCustomRuleBasedSegmentAdapterConsumer.getAll();
        Assert.assertEquals(0, ruleBasedSegmentsResult.size());
    }

    @Test
    public void testGetAllNullOnGetMany() {
        Set<String> keysResult = Stream.of(RULE_BASED_SEGMENT_NAME, RULE_BASED_SEGMENT_NAME+"2").collect(Collectors.toSet());
        Mockito.when(_userStorageWrapper.getKeysByPrefix(Mockito.anyObject())).
                thenReturn(keysResult);
        Mockito.when(_userStorageWrapper.getMany(Mockito.anyObject())).
                thenReturn(null);
        List<ParsedRuleBasedSegment> ruleBasedSegmentsResult = (List<ParsedRuleBasedSegment>) _userCustomRuleBasedSegmentAdapterConsumer.getAll();
        Assert.assertEquals(0, ruleBasedSegmentsResult.size());
    }

    @Test
    public void testGetSegments() {
        Condition condition = ConditionsTestUtil.makeUserDefinedSegmentCondition(ConditionType.WHITELIST, "employee",
                null, false);
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("rbs", Arrays.asList(condition), 1);
        List<String> getManyExpected = Stream.of(Json.toJson(ruleBasedSegment)).collect(Collectors.toList());
        Mockito.when(_userStorageWrapper.getMany(Mockito.anyObject())).
                thenReturn(getManyExpected);
        HashSet<String> segmentResult = (HashSet<String>) _userCustomRuleBasedSegmentAdapterConsumer.getSegments();
        Assert.assertTrue(segmentResult.contains("employee"));
    }

    @Test
    public void testGetruleBasedSegmentNames() {
        RuleBasedSegment ruleBasedSegment = getRuleBasedSegment(RULE_BASED_SEGMENT_NAME);
        RuleBasedSegment ruleBasedSegment2 = getRuleBasedSegment(RULE_BASED_SEGMENT_NAME+"2");
        Set<String> keysResult = Stream.of(RULE_BASED_SEGMENT_NAME, RULE_BASED_SEGMENT_NAME+"2").collect(Collectors.toSet());
        Mockito.when(_userStorageWrapper.getKeysByPrefix(Mockito.anyObject())).
                thenReturn(keysResult);
        List<String> getManyExpected = Stream.of(Json.toJson(ruleBasedSegment), Json.toJson(ruleBasedSegment2)).collect(Collectors.toList());
        Mockito.when(_userStorageWrapper.getMany(Mockito.anyObject())).
                thenReturn(getManyExpected);
        List<String> ruleBasedSegmentsResult = _userCustomRuleBasedSegmentAdapterConsumer.ruleBasedSegmentNames();
        Assert.assertNotNull(ruleBasedSegmentsResult);
        Assert.assertEquals(keysResult.size(), ruleBasedSegmentsResult.size());
        Assert.assertEquals(keysResult, new HashSet<>(ruleBasedSegmentsResult));
    }

    public static String getLongAsJson(long value) {
        return Json.toJson(value);
    }

    public static  String getRuleBasedSegmentAsJson(RuleBasedSegment ruleBasedSegment) {
        return Json.toJson(ruleBasedSegment);
    }

    private RuleBasedSegment getRuleBasedSegment(String name) {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");
        Condition c = ConditionsTestUtil.containsString("user",
                "products",
                set,
                false,
                null
        );

        List<Condition> conditions = Lists.newArrayList(c);
        return makeRuleBasedSegment(name, conditions, 1);
    }
}