package io.split.storages.memory;

import com.google.common.collect.Lists;
import io.split.client.dtos.Partition;
import io.split.engine.ConditionsTestUtil;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.UserDefinedSegmentMatcher;
import io.split.grammar.Treatments;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InMemoryCacheTest {
    private static final String EMPLOYEES = "Employees";
    private InMemoryCacheImp _cache;

    @Before
    public void before() {
        _cache = new InMemoryCacheImp();
    }

    @Test
    public void putAndGetSplit() {
        ParsedSplit split = getParsedSplit("split_name");
        _cache.putMany(Stream.of(split).collect(Collectors.toList()));

        ParsedSplit result = _cache.get("split_name");
        Assert.assertNotNull(result);
        Assert.assertEquals(split.changeNumber(), result.changeNumber());
        Assert.assertEquals(split.trafficTypeName(), result.trafficTypeName());
        Assert.assertEquals(split.defaultTreatment(), result.defaultTreatment());
    }

    @Test
    public void putDuplicateSplit() {
        ParsedSplit split = getParsedSplit("split_name");
        ParsedSplit split2 = getParsedSplit("split_name");
        _cache.putMany(Stream.of(split, split2).collect(Collectors.toList()));

        int result = _cache.getAll().size();

        Assert.assertEquals(1, result);
    }

    @Test
    public void getInExistentSplit() {
        ParsedSplit split = getParsedSplit("split_name");
        _cache.putMany(Stream.of(split).collect(Collectors.toList()));

        ParsedSplit result = _cache.get("split_name_2");
        Assert.assertNull(result);
    }

    @Test
    public void removeSplit() {
        ParsedSplit split = getParsedSplit("split_name");
        ParsedSplit split2 = getParsedSplit("split_name_2");
        _cache.putMany(Stream.of(split, split2).collect(Collectors.toList()));

        int result = _cache.getAll().size();
        Assert.assertEquals(2, result);

        _cache.remove("split_name");
        result = _cache.getAll().size();
        Assert.assertEquals(1, result);

        Assert.assertNull(_cache.get("split_name"));
    }

    @Test
    public void setAndGetChangeNumber() {
        _cache.setChangeNumber(223);

        long changeNumber = _cache.getChangeNumber();
        Assert.assertEquals(223, changeNumber);

        _cache.setChangeNumber(539);
        changeNumber = _cache.getChangeNumber();
        Assert.assertEquals(539, changeNumber);
    }

    @Test
    public void getMany() {
        ParsedSplit split = getParsedSplit("split_name_1");
        ParsedSplit split2 = getParsedSplit("split_name_2");
        ParsedSplit split3 = getParsedSplit("split_name_3");
        ParsedSplit split4 = getParsedSplit("split_name_4");
        _cache.putMany(Stream.of(split, split2, split3, split4).collect(Collectors.toList()));

        List<String> names = new ArrayList<>();
        names.add("split_name_2");
        names.add("split_name_3");

        Collection<ParsedSplit> result = _cache.fetchMany(names);
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void trafficTypesExist() {

        ParsedSplit split = ParsedSplit.createParsedSplitForTests("splitName_1", 0, false, "default_treatment", new ArrayList<>(), "tt", 123, 2);
        ParsedSplit split2 = ParsedSplit.createParsedSplitForTests("splitName_2", 0, false, "default_treatment", new ArrayList<>(), "tt", 123, 2);
        ParsedSplit split3 = ParsedSplit.createParsedSplitForTests("splitName_3", 0, false, "default_treatment", new ArrayList<>(), "tt_2", 123, 2);
        ParsedSplit split4 = ParsedSplit.createParsedSplitForTests("splitName_4", 0, false, "default_treatment", new ArrayList<>(), "tt_3", 123, 2);

        _cache.putMany(Stream.of(split, split2, split3, split4).collect(Collectors.toList()));
        assertTrue(_cache.trafficTypeExists("tt_2"));
        assertTrue(_cache.trafficTypeExists("tt"));
        assertFalse(_cache.trafficTypeExists("tt_5"));

        _cache.remove("splitName_2");
        assertTrue(_cache.trafficTypeExists("tt"));

        _cache.remove("splitName_1");
        assertFalse(_cache.trafficTypeExists("tt"));
    }

    @Test
    public void testSegmentNames() {
        List<Partition> fullyRollout = Lists.newArrayList(ConditionsTestUtil.partition("on", 100));
        List<Partition> turnOff = Lists.newArrayList(ConditionsTestUtil.partition(Treatments.CONTROL, 100));
        ParsedCondition parsedCondition1 = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new UserDefinedSegmentMatcher(EMPLOYEES)), fullyRollout);
        ParsedCondition parsedCondition2 = ParsedCondition.createParsedConditionForTests(CombiningMatcher.of(new UserDefinedSegmentMatcher(EMPLOYEES+"2")), turnOff);

        ParsedSplit split = ParsedSplit.createParsedSplitForTests("splitName_1", 0, false, "default_treatment", Stream.of(parsedCondition1).collect(Collectors.toList()), "tt", 123, 2);
        ParsedSplit split2 = ParsedSplit.createParsedSplitForTests("splitName_2", 0, false, "default_treatment", Stream.of(parsedCondition2).collect(Collectors.toList()), "tt", 123, 2);
        ParsedSplit split3 = ParsedSplit.createParsedSplitForTests("splitName_3", 0, false, "default_treatment", Stream.of(parsedCondition1).collect(Collectors.toList()), "tt_2", 123, 2);
        ParsedSplit split4 = ParsedSplit.createParsedSplitForTests("splitName_4", 0, false, "default_treatment", Stream.of(parsedCondition2).collect(Collectors.toList()), "tt_3", 123, 2);

        _cache.putMany(Stream.of(split, split2, split3, split4).collect(Collectors.toList()));

        Set<String> segments = _cache.getSegments();
        Assert.assertEquals(2, segments.size());
        Assert.assertTrue(segments.contains(EMPLOYEES));
        Assert.assertTrue(segments.contains(EMPLOYEES+"2"));

    }

    private ParsedSplit getParsedSplit(String splitName) {
        return ParsedSplit.createParsedSplitForTests(splitName, 0, false, "default_treatment", new ArrayList<>(), "tt", 123, 2);
    }

    @Test
    public void testPutMany() {
        _cache.putMany(Stream.of(getParsedSplit("split_name_1"),getParsedSplit("split_name_2"),getParsedSplit("split_name_3"),getParsedSplit("split_name_4")).collect(Collectors.toList()));
        List<String> names = Stream.of("split_name_1","split_name_2","split_name_3","split_name_4").collect(Collectors.toList());

        Collection<ParsedSplit> result = _cache.fetchMany(names);
        Assert.assertEquals(4, result.size());
    }

    @Test
    public void testIncreaseTrafficType() {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests("splitName_1", 0, false, "default_treatment", new ArrayList<>(), "tt", 123, 2);
        _cache.putMany(Stream.of(split).collect(Collectors.toList()));
        _cache.increaseTrafficType("tt_2");
        assertTrue(_cache.trafficTypeExists("tt_2"));
    }

    @Test
    public void testDecreaseTrafficType() {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests("splitName_1", 0, false, "default_treatment", new ArrayList<>(), "tt", 123, 2);
        _cache.putMany(Stream.of(split).collect(Collectors.toList()));
        _cache.decreaseTrafficType("tt");
        assertFalse(_cache.trafficTypeExists("tt_2"));
    }
}
