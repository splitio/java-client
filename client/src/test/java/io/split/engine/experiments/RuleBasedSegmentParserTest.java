package io.split.engine.experiments;

import com.google.common.collect.Lists;
import io.split.client.dtos.*;
import io.split.client.dtos.Matcher;
import io.split.client.utils.GenericClientUtil;
import io.split.client.utils.Json;
import io.split.client.utils.RuleBasedSegmentsToUpdate;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.evaluator.Labels;
import io.split.engine.matchers.*;
import io.split.engine.matchers.collections.ContainsAllOfSetMatcher;
import io.split.engine.matchers.collections.ContainsAnyOfSetMatcher;
import io.split.engine.matchers.collections.EqualToSetMatcher;
import io.split.engine.matchers.collections.PartOfSetMatcher;
import io.split.engine.matchers.strings.ContainsAnyOfMatcher;
import io.split.engine.matchers.strings.EndsWithAnyOfMatcher;
import io.split.engine.matchers.strings.StartsWithAnyOfMatcher;
import io.split.engine.segments.SegmentChangeFetcher;
import io.split.grammar.Treatments;
import io.split.storages.SegmentCache;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.split.client.utils.RuleBasedSegmentProcessor.processRuleBasedSegmentChanges;
import static org.junit.Assert.assertTrue;

/**
 * Tests for ExperimentParser
 *
 * @author adil
 */
public class RuleBasedSegmentParserTest {

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

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(EMPLOYEES, false);
        Matcher notSalespeople = ConditionsTestUtil.userDefinedSegmentMatcher(SALES_PEOPLE, true);
        Condition c = ConditionsTestUtil.and(employeesMatcher, notSalespeople, null);
        List<Condition> conditions = Lists.newArrayList(c);

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);
        ParsedRuleBasedSegment actual = parser.parse(ruleBasedSegment);

        AttributeMatcher employeesMatcherLogic = AttributeMatcher.vanilla(new UserDefinedSegmentMatcher(EMPLOYEES));
        AttributeMatcher notSalesPeopleMatcherLogic = new AttributeMatcher(null, new UserDefinedSegmentMatcher(SALES_PEOPLE), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(employeesMatcherLogic, notSalesPeopleMatcherLogic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, null);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedRuleBasedSegment expected = ParsedRuleBasedSegment.createParsedRuleBasedSegmentForTests ("first.name",   listOfMatcherAndSplits, "user", 1,
                new ArrayList<>(), new ArrayList<>());

        Assert.assertEquals(actual, expected);
        assertTrue(expected.hashCode() != 0);
        assertTrue(expected.equals(expected));
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

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(EMPLOYEES, false);

        Matcher salespeopleMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(SALES_PEOPLE, false);

        List<Partition> fullyRollout = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));
        List<Partition> turnOff = Lists.newArrayList(ConditionsTestUtil.partition(Treatments.CONTROL, 100));

        Condition c1 = ConditionsTestUtil.and(employeesMatcher, fullyRollout);
        Condition c2 = ConditionsTestUtil.and(salespeopleMatcher, turnOff);

        List<Condition> conditions = Lists.newArrayList(c1, c2);

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);
        ParsedRuleBasedSegment actual = parser.parse(ruleBasedSegment);

        ParsedCondition parsedCondition1 = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new UserDefinedSegmentMatcher(EMPLOYEES)), fullyRollout);
        ParsedCondition parsedCondition2 = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new UserDefinedSegmentMatcher(EMPLOYEES)), turnOff);
        List<ParsedCondition> listOfParsedConditions = Lists.newArrayList(parsedCondition1, parsedCondition2);

        ParsedRuleBasedSegment expected = ParsedRuleBasedSegment.createParsedRuleBasedSegmentForTests ("first.name",   listOfParsedConditions, "user", 1,
                new ArrayList<>(), new ArrayList<>());

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

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(EMPLOYEES, false);

        List<Condition> conditions = Lists.newArrayList();
        List<Partition> p1 = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));
        for (int i = 0 ; i < CONDITIONS_UPPER_LIMIT+1 ; i++) {
            Condition c = ConditionsTestUtil.and(employeesMatcher, p1);
            conditions.add(c);
        }

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);

        Assert.assertNotNull(parser.parse(ruleBasedSegment));
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

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher("user", "name", EMPLOYEES, false);

        Matcher creationDateNotOlderThanAPoint = ConditionsTestUtil.numericMatcher("user", "creation_date",
                MatcherType.GREATER_THAN_OR_EQUAL_TO,
                DataType.DATETIME,
                1457386741L,
                true);

        Condition c = ConditionsTestUtil.and(employeesMatcher, creationDateNotOlderThanAPoint, null);

        List<Condition> conditions = Lists.newArrayList(c);

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);
        ParsedRuleBasedSegment actual = parser.parse(ruleBasedSegment);

        AttributeMatcher employeesMatcherLogic = new AttributeMatcher("name", new UserDefinedSegmentMatcher(EMPLOYEES), false);
        AttributeMatcher creationDateNotOlderThanAPointLogic = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(1457386741L, DataType.DATETIME), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(employeesMatcherLogic, creationDateNotOlderThanAPointLogic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, null);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedRuleBasedSegment expected = ParsedRuleBasedSegment.createParsedRuleBasedSegmentForTests ("first.name",   listOfMatcherAndSplits, "user", 1,
                new ArrayList<>(), new ArrayList<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void lessThanOrEqualTo() {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        Matcher ageLessThan10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.LESS_THAN_OR_EQUAL_TO, DataType.NUMBER, 10L, false);
        Condition c = ConditionsTestUtil.and(ageLessThan10, null);

        List<Condition> conditions = Lists.newArrayList(c);

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);
        ParsedRuleBasedSegment actual = parser.parse(ruleBasedSegment);

        AttributeMatcher ageLessThan10Logic = new AttributeMatcher("age", new LessThanOrEqualToMatcher(10, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageLessThan10Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, null);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedRuleBasedSegment expected = ParsedRuleBasedSegment.createParsedRuleBasedSegmentForTests ("first.name",   listOfMatcherAndSplits, "user", 1,
                new ArrayList<>(), new ArrayList<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void equalTo() {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        Matcher ageLessThan10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.EQUAL_TO, DataType.NUMBER, 10L, true);
        Condition c = ConditionsTestUtil.and(ageLessThan10, null);
        List<Condition> conditions = Lists.newArrayList(c);

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);
        ParsedRuleBasedSegment actual = parser.parse(ruleBasedSegment);

        AttributeMatcher equalToMatcher = new AttributeMatcher("age", new EqualToMatcher(10, DataType.NUMBER), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(equalToMatcher));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, null);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedRuleBasedSegment expected = ParsedRuleBasedSegment.createParsedRuleBasedSegmentForTests ("first.name",   listOfMatcherAndSplits, "user", 1,
                new ArrayList<>(), new ArrayList<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void equalToNegativeNumber() {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        Matcher equalToNegative10 = ConditionsTestUtil.numericMatcher("user", "age", MatcherType.EQUAL_TO, DataType.NUMBER, -10L, false);
        Condition c = ConditionsTestUtil.and(equalToNegative10, null);
        List<Condition> conditions = Lists.newArrayList(c);

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);
        ParsedRuleBasedSegment actual = parser.parse(ruleBasedSegment);

        AttributeMatcher ageEqualTo10Logic = new AttributeMatcher("age", new EqualToMatcher(-10, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageEqualTo10Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, null);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedRuleBasedSegment expected = ParsedRuleBasedSegment.createParsedRuleBasedSegmentForTests ("first.name",   listOfMatcherAndSplits, "user", 1,
                new ArrayList<>(), new ArrayList<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void between() {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        Matcher ageBetween10And11 = ConditionsTestUtil.betweenMatcher("user",
                "age",
                DataType.NUMBER,
                10,
                12,
                false);

        Condition c = ConditionsTestUtil.and(ageBetween10And11, null);
        List<Condition> conditions = Lists.newArrayList(c);

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);
        ParsedRuleBasedSegment actual = parser.parse(ruleBasedSegment);

        AttributeMatcher ageBetween10And11Logic = new AttributeMatcher("age", new BetweenMatcher(10, 12, DataType.NUMBER), false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ageBetween10And11Logic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, null);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedRuleBasedSegment expected = ParsedRuleBasedSegment.createParsedRuleBasedSegmentForTests ("first.name",   listOfMatcherAndSplits, "user", 1,
                new ArrayList<>(), new ArrayList<>());

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void containsAnyOfSet() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        Condition c = ConditionsTestUtil.containsAnyOfSet("user",
                "products",
                set,
                false,
                null
                );

        ContainsAnyOfSetMatcher m = new ContainsAnyOfSetMatcher(set);
        setMatcherTest(c, m);
    }

    @Test
    public void containsAllOfSet() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");
        Condition c = ConditionsTestUtil.containsAllOfSet("user",
                "products",
                set,
                false,
                null
        );

        ContainsAllOfSetMatcher m = new ContainsAllOfSetMatcher(set);
        setMatcherTest(c, m);
    }

    @Test
    public void equalToSet() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");
        Condition c = ConditionsTestUtil.equalToSet("user",
                "products",
                set,
                false,
                null
        );

        EqualToSetMatcher m = new EqualToSetMatcher(set);
        setMatcherTest(c, m);
    }

    @Test
    public void isPartOfSet() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");
        Condition c = ConditionsTestUtil.isPartOfSet("user",
                "products",
                set,
                false,
                null
        );

        PartOfSetMatcher m = new PartOfSetMatcher(set);
        setMatcherTest(c, m);
    }

    @Test
    public void startsWithString() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");
        Condition c = ConditionsTestUtil.startsWithString("user",
                "products",
                set,
                false,
                null
        );

        StartsWithAnyOfMatcher m = new StartsWithAnyOfMatcher(set);
        setMatcherTest(c, m);
    }

    @Test
    public void endsWithString() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");
        Condition c = ConditionsTestUtil.endsWithString("user",
                "products",
                set,
                false,
                null
        );

        EndsWithAnyOfMatcher m = new EndsWithAnyOfMatcher(set);
        setMatcherTest(c, m);
    }


    @Test
    public void containsString() {
        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");
        Condition c = ConditionsTestUtil.containsString("user",
                "products",
                set,
                false,
                null
        );

        ContainsAnyOfMatcher m = new ContainsAnyOfMatcher(set);
        setMatcherTest(c, m);
    }

    @Test
    public void UnsupportedMatcher() {
        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        String splitWithUndefinedMatcher = "{\"ff\":{\"s\":-1,\"t\":-1,\"d\":[]},\"rbs\":{\"s\":-1,\"t\":1457726098069,\"d\":[{ \"changeNumber\": 123, \"trafficTypeName\": \"user\", \"name\": \"some_name\","
                + "\"status\": \"ACTIVE\",\"conditions\": [{\"contitionType\": \"ROLLOUT\","
                + "\"label\": \"some_label\", \"matcherGroup\": { \"matchers\": [{ \"matcherType\": \"UNKNOWN\", \"negate\": false}],"
                + "\"combiner\": \"AND\"}}],\"excluded\":{\"keys\":[],\"segments\":[]}}]}}";
        SplitChange change = Json.fromJson(splitWithUndefinedMatcher, SplitChange.class);
        for (RuleBasedSegment ruleBasedSegment : change.ruleBasedSegments.d) {
            // should not cause exception
            ParsedRuleBasedSegment parsedRuleBasedSegment = parser.parse(ruleBasedSegment);
            for (ParsedCondition parsedCondition : parsedRuleBasedSegment.parsedConditions()) {
                assertTrue(parsedCondition.label() == Labels.UNSUPPORTED_MATCHER);
                for (AttributeMatcher matcher : parsedCondition.matcher().attributeMatchers()) {
                    // Check the matcher is ALL_KEYS
                    assertTrue(matcher.matcher().toString().equals(" in segment all"));
                }
            }
        }
    }

    @Test
    public void EqualToSemverMatcher() throws IOException {
        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        String load = new String(Files.readAllBytes(Paths.get("src/test/resources/semver/semver-splits.json")), StandardCharsets.UTF_8);
        SplitChange change = Json.fromJson(load, SplitChange.class);
        for (RuleBasedSegment ruleBasedSegment : change.ruleBasedSegments.d) {
            // should not cause exception
            ParsedRuleBasedSegment parsedRuleBasedSegment = parser.parse(ruleBasedSegment);
            if (ruleBasedSegment.name.equals("rbs_semver_equalto")) {
                for (ParsedCondition parsedCondition : parsedRuleBasedSegment.parsedConditions()) {
                    assertTrue(parsedCondition.label().equals("equal to semver"));
                    for (AttributeMatcher matcher : parsedCondition.matcher().attributeMatchers()) {
                        // Check the matcher is ALL_KEYS
                        assertTrue(matcher.matcher().toString().equals(" == semver 1\\.22\\.9"));
                        return;
                    }
                }
            }
        }
        assertTrue(false);
    }

    @Test
    public void GreaterThanOrEqualSemverMatcher() throws IOException {
        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        String load = new String(Files.readAllBytes(Paths.get("src/test/resources/semver/semver-splits.json")), StandardCharsets.UTF_8);
        SplitChange change = Json.fromJson(load, SplitChange.class);
        for (RuleBasedSegment ruleBasedSegment : change.ruleBasedSegments.d) {
            // should not cause exception
            ParsedRuleBasedSegment parsedRuleBasedSegment = parser.parse(ruleBasedSegment);
            if (ruleBasedSegment.name.equals("rbs_semver_greater_or_equalto")) {
                for (ParsedCondition parsedCondition : parsedRuleBasedSegment.parsedConditions()) {
                    assertTrue(parsedCondition.label().equals("greater than or equal to semver"));
                    for (AttributeMatcher matcher : parsedCondition.matcher().attributeMatchers()) {
                        // Check the matcher is ALL_KEYS
                        assertTrue(matcher.matcher().toString().equals(" >= semver 1\\.22\\.9"));
                        return;
                    }
                }
            }
        }
        assertTrue(false);
    }

    @Test
    public void LessThanOrEqualSemverMatcher() throws IOException {
        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        String load = new String(Files.readAllBytes(Paths.get("src/test/resources/semver/semver-splits.json")), StandardCharsets.UTF_8);
        SplitChange change = Json.fromJson(load, SplitChange.class);
        for (RuleBasedSegment ruleBasedSegment : change.ruleBasedSegments.d) {
            // should not cause exception
            ParsedRuleBasedSegment parsedRuleBasedSegment = parser.parse(ruleBasedSegment);
            if (ruleBasedSegment.name.equals("rbs_semver_less_or_equalto")) {
                for (ParsedCondition parsedCondition : parsedRuleBasedSegment.parsedConditions()) {
                    assertTrue(parsedCondition.label().equals("less than or equal to semver"));
                    for (AttributeMatcher matcher : parsedCondition.matcher().attributeMatchers()) {
                        // Check the matcher is ALL_KEYS
                        assertTrue(matcher.matcher().toString().equals(" <= semver 1\\.22\\.9"));
                        return;
                    }
                }
            }
        }
        assertTrue(false);
    }

    @Test
    public void BetweenSemverMatcher() throws IOException {
        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        String load = new String(Files.readAllBytes(Paths.get("src/test/resources/semver/semver-splits.json")), StandardCharsets.UTF_8);
        SplitChange change = Json.fromJson(load, SplitChange.class);
        RuleBasedSegmentsToUpdate ruleBasedSegmentsToUpdate = processRuleBasedSegmentChanges(parser, change.ruleBasedSegments.d);
        for (ParsedRuleBasedSegment parsedRuleBasedSegment : ruleBasedSegmentsToUpdate.getToAdd()) {
            // should not cause exception
            if (parsedRuleBasedSegment.ruleBasedSegment().equals("rbs_semver_between")) {
                for (ParsedCondition parsedCondition : parsedRuleBasedSegment.parsedConditions()) {
                    assertTrue(parsedCondition.label().equals("between semver"));
                    for (AttributeMatcher matcher : parsedCondition.matcher().attributeMatchers()) {
                        // Check the matcher is ALL_KEYS
                        assertTrue(matcher.matcher().toString().equals(" between semver 1\\.22\\.9 and 2\\.1\\.0"));
                        return;
                    }
                }
            }
        }
        assertTrue(false);
    }

    @Test
    public void InListSemverMatcher() throws IOException {
        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        String load = new String(Files.readAllBytes(Paths.get("src/test/resources/semver/semver-splits.json")), StandardCharsets.UTF_8);
        SplitChange change = Json.fromJson(load, SplitChange.class);
        for (RuleBasedSegment ruleBasedSegment : change.ruleBasedSegments.d) {
            // should not cause exception
            ParsedRuleBasedSegment parsedRuleBasedSegment = parser.parse(ruleBasedSegment);
            if (ruleBasedSegment.name.equals("rbs_semver_inlist")) {
                for (ParsedCondition parsedCondition : parsedRuleBasedSegment.parsedConditions()) {
                    assertTrue(parsedCondition.label().equals("in list semver"));
                    for (AttributeMatcher matcher : parsedCondition.matcher().attributeMatchers()) {
                        // Check the matcher is ALL_KEYS
                        assertTrue(matcher.matcher().toString().startsWith(" in semver list"));
                        return;
                    }
                }
            }
        }
        assertTrue(false);
    }

    public void setMatcherTest(Condition c, io.split.engine.matchers.Matcher m) {
        SegmentChangeFetcher segmentChangeFetcher = Mockito.mock(SegmentChangeFetcher.class);
        SegmentChange segmentChangeEmployee = getSegmentChange(-1L, -1L, EMPLOYEES);
        SegmentChange segmentChangeSalesPeople = getSegmentChange(-1L, -1L, SALES_PEOPLE);
        Mockito.when(segmentChangeFetcher.fetch(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(segmentChangeEmployee).thenReturn(segmentChangeSalesPeople);

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");
        List<Condition> conditions = Lists.newArrayList(c);

        RuleBasedSegmentParser parser = new RuleBasedSegmentParser();
        RuleBasedSegment ruleBasedSegment = makeRuleBasedSegment("first.name", conditions, 1);
        ParsedRuleBasedSegment actual = parser.parse(ruleBasedSegment);

        AttributeMatcher attrMatcher = new AttributeMatcher("products", m, false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(attrMatcher));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, null);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedRuleBasedSegment expected = ParsedRuleBasedSegment.createParsedRuleBasedSegmentForTests ("first.name",   listOfMatcherAndSplits, "user", 1,
                new ArrayList<>(), new ArrayList<>());

        Assert.assertEquals(actual, expected);
    }

    private RuleBasedSegment makeRuleBasedSegment(String name, List<Condition> conditions, long changeNumber) {
        Excluded excluded = new Excluded();
        excluded.segments = new ArrayList<>();
        excluded.keys = new ArrayList<>();

        RuleBasedSegment ruleBasedSegment = new RuleBasedSegment();
        ruleBasedSegment.name = name;
        ruleBasedSegment.status = Status.ACTIVE;
        ruleBasedSegment.conditions = conditions;
        ruleBasedSegment.trafficTypeName = "user";
        ruleBasedSegment.changeNumber = changeNumber;
        ruleBasedSegment.excluded = excluded;
        return ruleBasedSegment;
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