package io.split.engine.evaluator;

import io.split.client.EventClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitClientImpl;
import io.split.client.SplitFactory;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.Partition;
import io.split.client.dtos.TreatmentLabelAndChangeNumber;
import io.split.client.exceptions.ChangeNumberExceptionWrapper;
import io.split.client.impressions.ImpressionsManager;
import io.split.engine.SDKReadinessGates;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.experiments.SplitFetcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.metrics.Metrics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EvaluatorTest {
    private static final String MatchingKey = "test";
    private static final String BucketingKey = "test";
    private static final String SplitName = "split_name_test";
    private static final Long ChangeNumber = 123123L;
    private static final String DefaultTreatmentValue = "defaultTreatment";
    private static final String TestLabelValue = "test label";
    private static final String TrafficTypeValue = "tt";
    private static final String TreatmentValue = "treatment_test";

    private SplitFetcher _splitFetcher;
    private Evaluator _evaluator;
    private SplitClientImpl _splitClient;
    private CombiningMatcher _matcher;
    private Map<String, String> _configurations;
    private List<ParsedCondition> _conditions;
    private List<Partition> _partitions;

    @Before
    public void before() {
        SDKReadinessGates gates = Mockito.mock(SDKReadinessGates.class);
        _splitFetcher = Mockito.mock(SplitFetcher.class);
        _evaluator = new EvaluatorImp(gates, _splitFetcher);
        _splitClient = getSplitClient(_splitFetcher, gates, _evaluator);
        _matcher = Mockito.mock(CombiningMatcher.class);

        _configurations = new HashMap<>();
        _conditions = new ArrayList<>();
        _partitions = new ArrayList<>();
    }

    @Test
    public void evaluateWhenSplitNameDoesNotExistReturnControl() throws ChangeNumberExceptionWrapper {
        Mockito.when(_splitFetcher.fetch(SplitName)).thenReturn(null);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MatchingKey, BucketingKey, SplitName, null, _splitClient);

        assertEquals("control", result.treatment);
        assertEquals("definition not found", result.label);
    }

    @Test
    public void evaluateWhenSplitIsKilledReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(SplitName, 0, true, DefaultTreatmentValue, _conditions, TrafficTypeValue, ChangeNumber, 2);
        Mockito.when(_splitFetcher.fetch(SplitName)).thenReturn(split);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MatchingKey, BucketingKey, SplitName, null, _splitClient);

        assertEquals(DefaultTreatmentValue, result.treatment);
        assertEquals("killed", result.label);
        assertEquals(ChangeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithoutConditionsReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(SplitName, 0, false, DefaultTreatmentValue, _conditions, TrafficTypeValue, ChangeNumber, 2);
        Mockito.when(_splitFetcher.fetch(SplitName)).thenReturn(split);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MatchingKey, BucketingKey, SplitName, null, _splitClient);

        assertEquals(DefaultTreatmentValue, result.treatment);
        assertEquals("default rule", result.label);
        assertEquals(ChangeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithRollOutConditionBucketIsBiggerTrafficAllocationReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        Partition partition = new Partition();
        partition.treatment = TreatmentValue;
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.ROLLOUT, _matcher,_partitions, TestLabelValue);
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(SplitName, 0, false, DefaultTreatmentValue, _conditions, TrafficTypeValue, ChangeNumber, 10, 12, 2, _configurations);

        Mockito.when(_splitFetcher.fetch(SplitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(MatchingKey, BucketingKey, null, _splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MatchingKey, BucketingKey, SplitName, null, _splitClient);

        assertEquals(DefaultTreatmentValue, result.treatment);
        assertEquals("not in split", result.label);
        assertEquals(ChangeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithRollOutConditionTrafficAllocationIsBiggerBucketReturnTreatment() throws ChangeNumberExceptionWrapper {
        Partition partition = new Partition();
        partition.treatment = TreatmentValue;
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.ROLLOUT, _matcher, _partitions, TestLabelValue);
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(SplitName, 0, false, DefaultTreatmentValue, _conditions, TrafficTypeValue, ChangeNumber, 60, 18, 2, _configurations);

        Mockito.when(_splitFetcher.fetch(SplitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(MatchingKey, BucketingKey, null, _splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MatchingKey, BucketingKey, SplitName, null, _splitClient);

        assertEquals(TreatmentValue, result.treatment);
        assertEquals(TestLabelValue, result.label);
        assertEquals(ChangeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithWhitelistConditionReturnTreatment() throws ChangeNumberExceptionWrapper {
        Partition partition = new Partition();
        partition.treatment = TreatmentValue;
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.WHITELIST, _matcher, _partitions, "test whitelist label");
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(SplitName, 0, false, DefaultTreatmentValue, _conditions, TrafficTypeValue, ChangeNumber, 60, 18, 2, _configurations);

        Mockito.when(_splitFetcher.fetch(SplitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(MatchingKey, BucketingKey, null, _splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(MatchingKey, BucketingKey, SplitName, null, _splitClient);

        assertEquals(TreatmentValue, result.treatment);
        assertEquals("test whitelist label", result.label);
        assertEquals(ChangeNumber, result.changeNumber);
    }

    private SplitClientImpl getSplitClient(SplitFetcher splitFetcher, SDKReadinessGates gates, Evaluator evaluator) {
        SplitFactory container = Mockito.mock(SplitFactory.class);
        ImpressionsManager impressionManager = Mockito.mock(ImpressionsManager.class);
        Metrics metrics = Mockito.mock(Metrics.class);
        EventClient eventClient = Mockito.mock(EventClient.class);
        SplitClientConfig config = Mockito.mock(SplitClientConfig.class);

        return new SplitClientImpl(container, splitFetcher, impressionManager, metrics, eventClient, config, gates, evaluator);
    }
}
