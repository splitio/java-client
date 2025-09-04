package io.split.engine.evaluator;

import io.split.client.dtos.*;
import io.split.client.utils.Json;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.PrerequisitesMatcher;
import io.split.storages.RuleBasedSegmentCacheConsumer;
import io.split.storages.SegmentCacheConsumer;
import io.split.storages.SplitCacheConsumer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EvaluatorTest {
    private static final String MATCHING_KEY = "test";
    private static final String BUCKETING_KEY = "test";
    private static final String SPLIT_NAME = "split_name_test";
    private static final Long CHANGE_NUMBER = 123123L;
    private static final String DEFAULT_TREATMENT_VALUE = "defaultTreatment";
    private static final String TEST_LABEL_VALUE = "test label";
    private static final String TRAFFIC_TYPE_VALUE = "tt";
    private static final String TREATMENT_VALUE = "treatment_test";

    private SplitCacheConsumer _splitCacheConsumer;
    private SegmentCacheConsumer _segmentCacheConsumer;
    private RuleBasedSegmentCacheConsumer _ruleBasedSegmentCacheConsumer;
    private Evaluator _evaluator;
    private CombiningMatcher _matcher;
    private Map<String, String> _configurations;
    private List<ParsedCondition> _conditions;
    private List<Partition> _partitions;
    private EvaluationContext _evaluationContext;

    @Before
    public void before() {
        _splitCacheConsumer = Mockito.mock(SplitCacheConsumer.class);
        _segmentCacheConsumer = Mockito.mock(SegmentCacheConsumer.class);
        _ruleBasedSegmentCacheConsumer = Mockito.mock(RuleBasedSegmentCacheConsumer.class);
        _evaluator = new EvaluatorImp(_splitCacheConsumer, _segmentCacheConsumer, _ruleBasedSegmentCacheConsumer, null);
        _matcher = Mockito.mock(CombiningMatcher.class);
        _evaluationContext = Mockito.mock(EvaluationContext.class);

        _configurations = new HashMap<>();
        _conditions = new ArrayList<>();
        _partitions = new ArrayList<>();
    }

    @Test
    public void evaluateWhenSplitNameDoesNotExistReturnControl() {
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(null);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals("control", result.treatment);
        assertEquals("definition not found", result.label);
    }

    @Test
    public void evaluateWhenSplitIsKilledReturnDefaultTreatment() {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(SPLIT_NAME, 0, true, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 2, new HashSet<>(), true, new PrerequisitesMatcher(null));
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        assertEquals("killed", result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);
    }

    @Test
    public void evaluateWithoutConditionsReturnDefaultTreatment() {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 2, new HashSet<>(), true, new PrerequisitesMatcher(null));
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        assertEquals("default rule", result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);
    }

    @Test
    public void evaluateWithRollOutConditionBucketIsBiggerTrafficAllocationReturnDefaultTreatment() {
        Partition partition = new Partition();
        partition.treatment = TREATMENT_VALUE;
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.ROLLOUT, _matcher,_partitions, TEST_LABEL_VALUE);
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 10, 12, 2, _configurations, new HashSet<>(), true, new PrerequisitesMatcher(null));

        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);
        Mockito.when(condition.matcher().match(MATCHING_KEY, BUCKETING_KEY, null, _evaluationContext)).thenReturn(true);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        assertEquals("not in split", result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);
    }

    @Test
    public void evaluateWithRollOutConditionTrafficAllocationIsBiggerBucketReturnTreatment() {
        Partition partition = new Partition();
        partition.treatment = TREATMENT_VALUE;
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.ROLLOUT, _matcher, _partitions, TEST_LABEL_VALUE);
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), true, new PrerequisitesMatcher(null));

        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);
        Mockito.when(condition.matcher().match(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject())).thenReturn(true);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals(TREATMENT_VALUE, result.treatment);
        assertEquals(TEST_LABEL_VALUE, result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);
    }

    @Test
    public void evaluateWithWhitelistConditionReturnTreatment() {
        Partition partition = new Partition();
        partition.treatment = TREATMENT_VALUE;
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.WHITELIST, _matcher, _partitions, "test whitelist label");
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), true, new PrerequisitesMatcher(null));

        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);
        Mockito.when(condition.matcher().match(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject())).thenReturn(true);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals(TREATMENT_VALUE, result.treatment);
        assertEquals("test whitelist label", result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);
    }

    @Test
    public void evaluateWithSets() {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 2, new HashSet<>(Arrays.asList("set1", "set2")), true, new PrerequisitesMatcher(null));
        List<String> sets = new ArrayList<>(Arrays.asList("set1", "empty_set"));
        Map<String, HashSet<String>> flagSets = new HashMap<>();
        flagSets.put("set1", new HashSet<>(Arrays.asList(SPLIT_NAME)));
        flagSets.put("empty_set", null);
        Mockito.when(_splitCacheConsumer.getNamesByFlagSets(sets)).thenReturn(flagSets);
        Map<String, ParsedSplit> parsedSplits = new HashMap<>();
        parsedSplits.put(SPLIT_NAME, split);
        Mockito.when(_splitCacheConsumer.fetchMany(Arrays.asList(SPLIT_NAME))).thenReturn(parsedSplits);

        Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> result = _evaluator.evaluateFeaturesByFlagSets(MATCHING_KEY, BUCKETING_KEY, sets, null);

        EvaluatorImp.TreatmentLabelAndChangeNumber treatmentLabelAndChangeNumber = result.get(SPLIT_NAME);

        assertEquals(DEFAULT_TREATMENT_VALUE, treatmentLabelAndChangeNumber.treatment);
        assertEquals("default rule", treatmentLabelAndChangeNumber.label);
        assertEquals(CHANGE_NUMBER, treatmentLabelAndChangeNumber.changeNumber);
    }

    @Test
    public void evaluateWithSetsNotHaveFlags() {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 2, new HashSet<>(Arrays.asList("set1", "set2")), true, new PrerequisitesMatcher(null));
        List<String> sets = new ArrayList<>(Arrays.asList("set2"));
        Map<String, HashSet<String>> flagSets = new HashMap<>();
        Mockito.when(_splitCacheConsumer.getNamesByFlagSets(sets)).thenReturn(flagSets);
        Map<String, ParsedSplit> parsedSplits = new HashMap<>();
        Mockito.when(_splitCacheConsumer.fetchMany(Arrays.asList(SPLIT_NAME))).thenReturn(parsedSplits);

        Map<String, EvaluatorImp.TreatmentLabelAndChangeNumber> result = _evaluator.evaluateFeaturesByFlagSets(MATCHING_KEY, BUCKETING_KEY, sets, null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void evaluateWithPrerequisites() {
        Partition partition = new Partition();
        partition.treatment = TREATMENT_VALUE;
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.WHITELIST, _matcher, _partitions, "test whitelist label");
        _conditions.add(condition);
        List<Prerequisites> prerequisites = Arrays.asList(Json.fromJson("{\"n\": \"split1\", \"ts\": [\"" + TREATMENT_VALUE + "\"]}", Prerequisites.class));

        ParsedSplit split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), true, new PrerequisitesMatcher(prerequisites));
        ParsedSplit split1 = new ParsedSplit("split1", 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), true, new PrerequisitesMatcher(null));

        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);
        Mockito.when(_splitCacheConsumer.get("split1")).thenReturn(split1);
        Mockito.when(condition.matcher().match(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject())).thenReturn(true);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals(TREATMENT_VALUE, result.treatment);
        assertEquals("test whitelist label", result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);

        Mockito.when(condition.matcher().match(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject())).thenReturn(false);
        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        assertEquals(Labels.PREREQUISITES_NOT_MET, result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);

        // if split is killed, label should be killed.
        split = new ParsedSplit(SPLIT_NAME, 0, true, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), true, new PrerequisitesMatcher(prerequisites));
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);
        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        assertEquals(Labels.KILLED, result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);
    }

    @Test
    public void evaluateFallbackTreatmentWorks() {
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(null);
        FallbackTreatmentsConfiguration fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(new FallbackTreatment("on"), null);
        _evaluator = new EvaluatorImp(_splitCacheConsumer, _segmentCacheConsumer, _ruleBasedSegmentCacheConsumer, fallbackTreatmentsConfiguration);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals("on", result.treatment);
        assertEquals("fallback - definition not found", result.label);

        ParsedSplit split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, null, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), false, null);
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);
        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals("on", result.treatment);
        assertEquals("fallback - exception", result.label);

        // using byflag only
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(null);
        Mockito.when(_splitCacheConsumer.get("another_name")).thenReturn(null);
        fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(null, new HashMap<String, FallbackTreatment>() {{ put(SPLIT_NAME, new FallbackTreatment("off")); }} );
        _evaluator = new EvaluatorImp(_splitCacheConsumer, _segmentCacheConsumer, _ruleBasedSegmentCacheConsumer, fallbackTreatmentsConfiguration);

        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals("off", result.treatment);
        assertEquals("fallback - definition not found", result.label);

        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, "another_name", null);
        assertEquals("control", result.treatment);
        assertEquals("definition not found", result.label);

        split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, null, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), false, null);
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);
        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals("off", result.treatment);
        assertEquals("fallback - exception", result.label);

        split = new ParsedSplit("another_name", 0, false, DEFAULT_TREATMENT_VALUE, _conditions, null, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), false, null);
        Mockito.when(_splitCacheConsumer.get("another_name")).thenReturn(split);
        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, "another_name", null);
        assertEquals("control", result.treatment);
        assertEquals("exception", result.label);

        // with byflag
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(null);
        Mockito.when(_splitCacheConsumer.get("another_name")).thenReturn(null);
        fallbackTreatmentsConfiguration = new FallbackTreatmentsConfiguration(new FallbackTreatment("on"), new HashMap<String, FallbackTreatment>() {{ put(SPLIT_NAME, new FallbackTreatment("off")); }} );
        _evaluator = new EvaluatorImp(_splitCacheConsumer, _segmentCacheConsumer, _ruleBasedSegmentCacheConsumer, fallbackTreatmentsConfiguration);

        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals("off", result.treatment);
        assertEquals("fallback - definition not found", result.label);

        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, "another_name", null);
        assertEquals("on", result.treatment);
        assertEquals("fallback - definition not found", result.label);

        split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, null, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), false, null);
        Mockito.when(_splitCacheConsumer.get(SPLIT_NAME)).thenReturn(split);
        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);
        assertEquals("off", result.treatment);
        assertEquals("fallback - exception", result.label);

        split = new ParsedSplit("another_name", 0, false, DEFAULT_TREATMENT_VALUE, _conditions, null, CHANGE_NUMBER, 60, 18, 2, _configurations, new HashSet<>(), false, null);
        Mockito.when(_splitCacheConsumer.get("another_name")).thenReturn(split);
        result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, "another_name", null);
        assertEquals("on", result.treatment);
        assertEquals("fallback - exception", result.label);
    }
}