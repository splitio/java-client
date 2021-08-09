package io.split.engine.evaluator;

import io.split.cache.SegmentCache;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Partition;
import io.split.cache.SplitCache;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.CombiningMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
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

    private SplitCache _splitCache;
    private SegmentCache _segmentCache;
    private Evaluator _evaluator;
    private CombiningMatcher _matcher;
    private Map<String, String> _configurations;
    private List<ParsedCondition> _conditions;
    private List<Partition> _partitions;
    private EvaluationContext _evaluationContext;

    @Before
    public void before() {
        _splitCache = Mockito.mock(SplitCache.class);
        _segmentCache = Mockito.mock(SegmentCache.class);
        _evaluator = new EvaluatorImp(_splitCache, _segmentCache);
        _matcher = Mockito.mock(CombiningMatcher.class);
        _evaluationContext = Mockito.mock(EvaluationContext.class);

        _configurations = new HashMap<>();
        _conditions = new ArrayList<>();
        _partitions = new ArrayList<>();
    }

    @Test
    public void evaluateWhenSplitNameDoesNotExistReturnControl() {
        Mockito.when(_splitCache.get(SPLIT_NAME)).thenReturn(null);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals("control", result.treatment);
        assertEquals("definition not found", result.label);
    }

    @Test
    public void evaluateWhenSplitIsKilledReturnDefaultTreatment() {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(SPLIT_NAME, 0, true, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 2);
        Mockito.when(_splitCache.get(SPLIT_NAME)).thenReturn(split);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        assertEquals("killed", result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);
    }

    @Test
    public void evaluateWithoutConditionsReturnDefaultTreatment() {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 2);
        Mockito.when(_splitCache.get(SPLIT_NAME)).thenReturn(split);

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

        ParsedSplit split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 10, 12, 2, _configurations);

        Mockito.when(_splitCache.get(SPLIT_NAME)).thenReturn(split);
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

        ParsedSplit split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 60, 18, 2, _configurations);

        Mockito.when(_splitCache.get(SPLIT_NAME)).thenReturn(split);
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

        ParsedSplit split = new ParsedSplit(SPLIT_NAME, 0, false, DEFAULT_TREATMENT_VALUE, _conditions, TRAFFIC_TYPE_VALUE, CHANGE_NUMBER, 60, 18, 2, _configurations);

        Mockito.when(_splitCache.get(SPLIT_NAME)).thenReturn(split);
        Mockito.when(condition.matcher().match(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject())).thenReturn(true);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MATCHING_KEY, BUCKETING_KEY, SPLIT_NAME, null);

        assertEquals(TREATMENT_VALUE, result.treatment);
        assertEquals("test whitelist label", result.label);
        assertEquals(CHANGE_NUMBER, result.changeNumber);
    }
}
