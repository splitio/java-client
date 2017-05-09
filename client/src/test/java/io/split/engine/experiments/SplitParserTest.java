package io.split.engine.experiments;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.split.client.dtos.Condition;
import io.split.client.dtos.DataType;
import io.split.client.dtos.Matcher;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.MatcherType;
import io.split.client.dtos.Partition;
import io.split.client.dtos.Split;
import io.split.client.dtos.Status;
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
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.segments.StaticSegment;
import io.split.engine.segments.StaticSegmentFetcher;
import io.split.grammar.Treatments;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for ExperimentParser
 *
 * @author adil
 */
public class SplitParserTest {

    @Test
    public void works() {

        StaticSegment employees = new StaticSegment("employees", Sets.newHashSet("adil", "pato", "trevor"));
        StaticSegment salespeople = new StaticSegment("salespeople", Sets.newHashSet("kunal"));

        Map<String, StaticSegment> fetcherMap = Maps.newHashMap();
        fetcherMap.put(employees.segmentName(), employees);
        fetcherMap.put(salespeople.segmentName(), salespeople);

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(fetcherMap);

        SplitParser parser = new SplitParser(segmentFetcher);


        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(employees.segmentName(), false);
        Matcher notSalespeople = ConditionsTestUtil.userDefinedSegmentMatcher(salespeople.segmentName(), true);

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.and(employeesMatcher, notSalespeople, partitions);

        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher employeesMatcherLogic = AttributeMatcher.vanilla(new UserDefinedSegmentMatcher(employees));
        AttributeMatcher notSalesPeopleMatcherLogic = new AttributeMatcher(null, new UserDefinedSegmentMatcher(salespeople), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(employeesMatcherLogic, notSalesPeopleMatcherLogic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void works_for_two_conditions() {

        StaticSegment employees = new StaticSegment("employees", Sets.newHashSet("adil", "pato", "trevor"));
        StaticSegment salespeople = new StaticSegment("salespeople", Sets.newHashSet("kunal"));

        Map<String, StaticSegment> fetcherMap = Maps.newHashMap();
        fetcherMap.put(employees.segmentName(), employees);
        fetcherMap.put(salespeople.segmentName(), salespeople);

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(fetcherMap);

        SplitParser parser = new SplitParser(segmentFetcher);

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(employees.segmentName(), false);

        Matcher salespeopleMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(salespeople.segmentName(), false);

        List<Partition> fullyRollout = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));
        List<Partition> turnOff = Lists.newArrayList(ConditionsTestUtil.partition(Treatments.CONTROL, 100));

        Condition c1 = ConditionsTestUtil.and(employeesMatcher, fullyRollout);
        Condition c2 = ConditionsTestUtil.and(salespeopleMatcher, turnOff);

        List<Condition> conditions = Lists.newArrayList(c1, c2);

        Split split = makeSplit("first.name", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        ParsedCondition parsedCondition1 = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new UserDefinedSegmentMatcher(employees)), fullyRollout);
        ParsedCondition parsedCondition2 = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new UserDefinedSegmentMatcher(salespeople)), turnOff);
        List<ParsedCondition> listOfParsedConditions = Lists.newArrayList(parsedCondition1, parsedCondition2);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfParsedConditions, "user", 1, 1);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void fails_for_long_conditions() {

        StaticSegment employees = new StaticSegment("employees", Sets.newHashSet("adil", "pato", "trevor"));

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(Maps.<String, StaticSegment>newHashMap());
        SplitParser parser = new SplitParser(segmentFetcher);

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher(employees.segmentName(), false);

        List<Condition> conditions = Lists.newArrayList();
        List<Partition> p1 = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));
        for (int i = 0 ; i < SplitParser.CONDITIONS_UPPER_LIMIT+1 ; i++) {
            Condition c = ConditionsTestUtil.and(employeesMatcher, p1);
            conditions.add(c);
        }

        Split split = makeSplit("first.name", 123, conditions, 1);

        assertThat(parser.parse(split), is(nullValue()));
    }


    @Test
    public void works_with_attributes() {

        StaticSegment employees = new StaticSegment("employees", Sets.newHashSet("adil", "pato", "trevor"));
        StaticSegment salespeople = new StaticSegment("salespeople", Sets.newHashSet("kunal"));

        Map<String, StaticSegment> fetcherMap = Maps.newHashMap();
        fetcherMap.put(employees.segmentName(), employees);
        fetcherMap.put(salespeople.segmentName(), salespeople);

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(fetcherMap);

        SplitParser parser = new SplitParser(segmentFetcher);

        Matcher employeesMatcher = ConditionsTestUtil.userDefinedSegmentMatcher("user", "name", employees.segmentName(), false);

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

        AttributeMatcher employeesMatcherLogic = new AttributeMatcher("name", new UserDefinedSegmentMatcher(employees), false);
        AttributeMatcher creationDateNotOlderThanAPointLogic = new AttributeMatcher("creation_date", new GreaterThanOrEqualToMatcher(1457386741L, DataType.DATETIME), true);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(employeesMatcherLogic, creationDateNotOlderThanAPointLogic));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void less_than_or_equal_to() {


        Map<String, StaticSegment> fetcherMap = Maps.newHashMap();

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(fetcherMap);

        SplitParser parser = new SplitParser(segmentFetcher);

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

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void equal_to() {

        Map<String, StaticSegment> fetcherMap = Maps.newHashMap();

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(fetcherMap);

        SplitParser parser = new SplitParser(segmentFetcher);

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

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void equal_to_negative_number() {

        Map<String, StaticSegment> fetcherMap = Maps.newHashMap();

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(fetcherMap);

        SplitParser parser = new SplitParser(segmentFetcher);

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

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void between() {

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(Collections.<String, StaticSegment>emptyMap());
        SplitParser parser = new SplitParser(segmentFetcher);

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

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("first.name", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void contains_any_of_set() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsAnyOfSet("user",
                "products",
                set,
                false,
                partitions
                );

        ContainsAnyOfSetMatcher m = new ContainsAnyOfSetMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void contains_all_of_set() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsAllOfSet("user",
                "products",
                set,
                false,
                partitions
        );

        ContainsAllOfSetMatcher m = new ContainsAllOfSetMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void equal_to_set() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.equalToSet("user",
                "products",
                set,
                false,
                partitions
        );

        EqualToSetMatcher m = new EqualToSetMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void is_part_of_set() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.isPartOfSet("user",
                "products",
                set,
                false,
                partitions
        );

        PartOfSetMatcher m = new PartOfSetMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void starts_with_string() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.startsWithString("user",
                "products",
                set,
                false,
                partitions
        );

        StartsWithAnyOfMatcher m = new StartsWithAnyOfMatcher(set);

        set_matcher_test(c, m);
    }

    @Test
    public void ends_with_string() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.endsWithString("user",
                "products",
                set,
                false,
                partitions
        );

        EndsWithAnyOfMatcher m = new EndsWithAnyOfMatcher(set);

        set_matcher_test(c, m);
    }


    @Test
    public void contains_string() {

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));

        Condition c = ConditionsTestUtil.containsString("user",
                "products",
                set,
                false,
                partitions
        );

        ContainsAnyOfMatcher m = new ContainsAnyOfMatcher(set);

        set_matcher_test(c, m);
    }

    public void set_matcher_test(Condition c, io.split.engine.matchers.Matcher m) {

        SegmentFetcher segmentFetcher = new StaticSegmentFetcher(Collections.<String, StaticSegment>emptyMap());
        SplitParser parser = new SplitParser(segmentFetcher);

        ArrayList<String> set = Lists.<String>newArrayList("sms", "voice");

        List<Partition> partitions = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));


        List<Condition> conditions = Lists.newArrayList(c);

        Split split = makeSplit("splitName", 123, conditions, 1);

        ParsedSplit actual = parser.parse(split);

        AttributeMatcher attrMatcher = new AttributeMatcher("products", m, false);
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(attrMatcher));
        ParsedCondition parsedCondition = ParsedCondition.createParsedConditionForTests(combiningMatcher, partitions);
        List<ParsedCondition> listOfMatcherAndSplits = Lists.newArrayList(parsedCondition);

        ParsedSplit expected = ParsedSplit.createParsedSplitForTests("splitName", 123, false, Treatments.OFF, listOfMatcherAndSplits, "user", 1, 1);

        assertThat(actual, is(equalTo(expected)));
    }

    private Split makeSplit(String name, int seed, List<Condition> conditions, long changeNumber) {
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
        return split;
    }

}
