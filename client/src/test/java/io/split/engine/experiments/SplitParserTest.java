package io.split.engine.experiments;

import com.google.common.collect.Lists;
import io.split.client.dtos.Condition;
import io.split.client.dtos.DataType;
import io.split.client.dtos.Matcher;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.MatcherType;
import io.split.client.dtos.Partition;
import io.split.client.dtos.SegmentChange;
import io.split.client.dtos.Split;
import io.split.client.dtos.Status;
import io.split.storages.SegmentCache;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.BetweenMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.EqualToMatcher;
import io.split.engine.matchers.GreaterThanOrEqualToMatcher;
import io.split.engine.matchers.LessThanOrEqualToMatcher;
import io.split.engine.matchers.UserDefinedSegmentMatcher;
import io.split.engine.matchers.collections.ContainsAllOfSetMatcher;
import io.split.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.engine.matchers.collections.EqualToSetMatcher;
import io.split.engine.matchers.collections.PartOfSetMatcher;
import io.split.engine.matchers.strings.ContainsAnyOfMatcher;
import io.split.engine.matchers.strings.EndsWithAnyOfMatcher;
import io.split.engine.matchers.strings.StartsWithAnyOfMatcher;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.grammar.Treatments;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests for ExperimentParser
 *
 * @author adil
 */
public class SplitParserTest {

    public static final String EMPLOYEES = "employees";
    public static final String SALES_PEOPLE = "salespeople";
    public static final int CONDITIONS_UPPER_LIMIT = 50;

    @Test
    public void works() {
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        segmentCache.updateSegment(EMPLOYEES, Stream.of("adil", "pato", "trevor").collect(Collectors.toList()), new ArrayList<>(), 1L);
        segmentCache.updateSegment(SALES_PEOPLE, Stream.of("kunal").collect(Collectors.toList()), new ArrayList<>(), 1L);
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        SplitParser parser = new SplitParser();


        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(EMPLOYEES, false);
        Matcher notSalespeople = ConditionsTestUtil.userDefinedSegmentMatcher(SALES_PEOPLE, true);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(employeesMatcher, notSalespeople, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher employeesMatcherLogic = AttributeMatcher.vanilla(new UserDefinedSegmentMatcher(EMPLOYEES));
        AttributeMatcher notSalesPeopleMatcherLogic = new AttributeMatcher(null, new UserDefinedSegmentMatcher(SALES_PEOPLE), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(employeesMatcherLogic, notSalesPeopleMatcherLogic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, new HashSet<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void worksWithConfig() {
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        segmentCache.updateSegment(EMPLOYEES, Stream.of("adil", "pato", "trevor").collect(Collectors.toList()), new ArrayList<>(), 1L);
        segmentCache.updateSegment(SALES_PEOPLE, Stream.of("kunal").collect(Collectors.toList()), new ArrayList<>(), 1L);
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        SplitParser parser = new SplitParser();


        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(EMPLOYEES, false);
        Matcher notSalespeople = ConditionsTestUtil.userDefinedSegmentMatcher(SALES_PEOPLE, true);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(employeesMatcher, notSalespeople, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Map<String, String> configurations = new HashMap<>();
        configurations.put("on", "{\"size\":15,\"test\":20}");
        configurations.put("off", "{\"size\":10}");
        Split split = makeSplit("first.name", 123, conditions, 1, configurations);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher employeesMatcherLogic = AttributeMatcher.vanilla(new UserDefinedSegmentMatcher(EMPLOYEES));
        AttributeMatcher notSalesPeopleMatcherLogic = new AttributeMatcher(null, new UserDefinedSegmentMatcher(SALES_PEOPLE), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(employeesMatcherLogic, notSalesPeopleMatcherLogic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF,
                listOfMatcherAndSplits, "user", 1, 1, configurations, new HashSet<>());

        Assert.assertEquals(actual, expected);
        Assert.assertEquals(actual.configurations().get("on"), configurations.get("on"));
    }

    @Test
    public void worksForTwoConditions() {
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        segmentCache.updateSegment(EMPLOYEES, Stream.of("adil", "pato", "trevor").collect(Collectors.toList()), new ArrayList<>(), 1L);
        segmentCache.updateSegment(SALES_PEOPLE, Stream.of("kunal").collect(Collectors.toList()), new ArrayList<>(), 1L);
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);


        SplitParser parser = new SplitParser();

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(EMPLOYEES, false);

        Matcher salespeopleMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(SALES_PEOPLE, false);

        List<Partition> fullyRollout = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));
        List<Partition> turnOff = Lists.newArrayList(ConditionsTestUtil.partition(Treatments.CONTROL, 100));

        Condition c1 = ConditionsTestUtil.and(employeesMatcher, fullyRollout);
        Condition c2 = ConditionsTestUtil.and(salespeopleMatcher, turnOff);

        List<Condition> conditions = Lists.newArrayList(c1, c2);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        ParsedCondition parsedCondition1 = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new UserDefinedSegmentMatcher(EMPLOYEES)), fullyRollout);
        ParsedCondition parsedCondition2 = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new UserDefinedSegmentMatcher(EMPLOYEES)), turnOff);
        List<ParsedCondition> listOfParsedConditions = Lists.newArrayList(parsedCondition1, parsedCondition2);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfParsedConditions, "user", 1, 1, new HashSet<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void successForLongConditions() {
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        segmentCache.updateSegment(EMPLOYEES, Stream.of("adil", "pato", "trevor").collect(Collectors.toList()), new ArrayList<>(), 1L);
        segmentCache.updateSegment(SALES_PEOPLE, Stream.of("kunal").collect(Collectors.toList()), new ArrayList<>(), 1L);
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee);


        SplitParser parser = new SplitParser();

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(EMPLOYEES, false);

        List<Condition> conditions = Lists.newArrayList();
        List<Partition> p1 = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));
        for (int i = 0 ; i < CONDITIONS_UPPER_LIMIT+1 ; i++) {
            Condition c = ConditionsTestUtil.and(employeesMatcher, p1);
            conditions.add(c);
        }

        Split split = makeSplit("first.name", 123, conditions, 1);

        Assert.assertNotNull(parser.parse(split));
    }


    @Test
    public void worksWithAttributes() {
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        segmentCache.updateSegment(EMPLOYEES, Stream.of("adil", "pato", "trevor").collect(Collectors.toList()), new ArrayList<>(), 1L);
        segmentCache.updateSegment(SALES_PEOPLE, Stream.of("kunal").collect(Collectors.toList()), new ArrayList<>(), 1L);
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);


        SplitParser parser = new SplitParser();

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher("user", "name", EMPLOYEES, false);

        Matcher creationDateNotOlderThanAPoint = ConditionsTestUtil.numericMatcher("user", "creation_date",
                MatcherType.GREATER_THAN_OR_EQUAL_TO,
                DataType.DATETIME,
                1457386741L,
                true);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(employeesMatcher, creationDateNotOlderThanAPoint, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher employeesMatcherLogic = new AttributeMatcher("name", new UserDefinedSegmentMatcher(EMPLOYEES), false);
        AttributeMatcher creationDateNotOlderThanAPointLogic = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(1457386741L, DataType.DATETIME), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(employeesMatcherLogic, creationDateNotOlderThanAPointLogic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, new HashSet<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void lessThanOrEqualTo() {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);


        SplitParser parser = new SplitParser();

        Matcher ageLessThan10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.LESS_THAN_OR_EQUAL_TO, DataType.NUMBER, 10L, false);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageLessThan10, partitions);


        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageLessThan10Logic = new AttributeMatcher("age", new LessThanOrEqualToMatcher(10, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageLessThan10Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, new HashSet<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void equalTo() {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);


        SplitParser parser = new SplitParser();

        Matcher ageLessThan10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.EQUAL_TO, DataType.NUMBER, 10L, true);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageLessThan10, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher equalToMatcher = new AttributeMatcher("age", new EqualToMatcher(10, DataType.NUMBER), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(equalToMatcher));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, new HashSet<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void equalToNegativeNumber() {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        SplitParser parser = new SplitParser();

        Matcher equalToNegative10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.EQUAL_TO, DataType.NUMBER, -10L, false);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(equalToNegative10, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageEqualTo10Logic = new AttributeMatcher("age", new EqualToMatcher(-10, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageEqualTo10Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, new HashSet<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void between() {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        SplitParser parser = new SplitParser();

        Matcher ageBetween10And11 = ConditionsTestUtil.betweenMatcher("user",
                "age",
                DataType.NUMBER,
                10,
                12,
                false);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(ageBetween10And11, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher ageBetween10And11Logic = new AttributeMatcher("age", new BetweenMatcher(10, 12, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageBetween10And11Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, new HashSet<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void containsAnyOfSet() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsAnyOfSet("user",
                "products",
                set,
                false,
                partitions
                );

        ContainsAnyOfSetMatcher m = new ContainsAnyOfSetMatcher(set);

        setMatcherTest(c, m);
    }

    @Test
    public void containsAllOfSet() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsAllOfSet("user",
                "products",
                set,
                false,
                partitions
        );

        ContainsAllOfSetMatcher m = new ContainsAllOfSetMatcher(set);

        setMatcherTest(c, m);
    }

    @Test
    public void equalToSet() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.equalToSet("user",
                "products",
                set,
                false,
                partitions
        );

        EqualToSetMatcher m = new EqualToSetMatcher(set);

        setMatcherTest(c, m);
    }

    @Test
    public void isPartOfSet() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.isPartOfSet("user",
                "products",
                set,
                false,
                partitions
        );

        PartOfSetMatcher m = new PartOfSetMatcher(set);

        setMatcherTest(c, m);
    }

    @Test
    public void startsWithString() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.startsWithString("user",
                "products",
                set,
                false,
                partitions
        );

        StartsWithAnyOfMatcher m = new StartsWithAnyOfMatcher(set);

        setMatcherTest(c, m);
    }

    @Test
    public void endsWithString() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.endsWithString("user",
                "products",
                set,
                false,
                partitions
        );

        EndsWithAnyOfMatcher m = new EndsWithAnyOfMatcher(set);

        setMatcherTest(c, m);
    }


    @Test
    public void containsString() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsString("user",
                "products",
                set,
                false,
                partitions
        );

        ContainsAnyOfMatcher m = new ContainsAnyOfMatcher(set);

        setMatcherTest(c, m);
    }

    public void setMatcherTest(Condition c, io.split.engine.matchers.Matcher m) {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        SplitParser parser = new SplitParser();

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));


        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("splitName", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher attrMatcher = new AttributeMatcher("products", m, false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(attrMatcher));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("splitName", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1, new HashSet<>());

        Assert.assertEquals(actual, expected);
    }

    private Split makeSplit(String name, int seed, List<Condition> conditions, long changeNumber) {
        return makeSplit(name, seed, conditions, changeNumber, null);
    }

    private Split makeSplit(String name, int seed, List<Condition> conditions, long changeNumber, Map<String, String> configurations) {
        Split split = new Split();
        split.name = name;
        split.seed = seed;
        split.trafficAllocation = 100;
        split.trafficAllocationSeed = seed;
        split.status = Status.ACTIVE;
        split.conditions = conditions;
        split.defaultTreatment = Treatments.OFF;
        split.trafficTypeName = "user";
        split.changeNumber = changeNumber;
        split.algo = 1;
        split.configurations = configurations;
        return split;
    }

    private SegmentChange getSegmentChange(long since, long till, String segmentName){
        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = segmentName;
        segmentChange.since = since;
        segmentChange.till = till;
        segmentChange.added = new ArrayList<>();
        segmentChange.removed = new ArrayList<>();
        return  segmentChange;
    }
}