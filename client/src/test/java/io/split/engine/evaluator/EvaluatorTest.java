package io.split.engine.evaluator;

import io.split.client.*;
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
    private SplitFetcher _splitFetcher;
    private SDKReadinessGates _gates;
    private Evaluator _evaluator;
    private SplitClientImpl _splitClient;
    private CombiningMatcher _matcher;
    private Map<String, String> _configurations;
    private List<ParsedCondition> _conditions;
    private List<Partition> _partitions;

    private String _matchingKey = "test";
    private String _bucketingKey = "test";
    private String _splitName = "split_name_test";
    private Long _changeNumber = 123123L;

    @Before
    public void before() {
        _splitFetcher = Mockito.mock(SplitFetcher.class);
        _gates = Mockito.mock(SDKReadinessGates.class);
        _evaluator = new EvaluatorImp(_gates, _splitFetcher);
        _splitClient = getSplitClient(_splitFetcher, _gates, _evaluator);
        _matcher = Mockito.mock(CombiningMatcher.class);

        _configurations = new HashMap<>();
        _conditions = new ArrayList<>();
        _partitions = new ArrayList<>();
    }

    @Test
    public void evaluateWhenSplitNameDoesNotExistReturnControl() throws ChangeNumberExceptionWrapper {
        Mockito.when(_splitFetcher.fetch(_splitName)).thenReturn(null);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(_matchingKey, _bucketingKey, _splitName, null, _splitClient);

        assertEquals("control", result.treatment);
        assertEquals("definition not found", result.label);
    }

    @Test
    public void evaluateWhenSplitIsKilledReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(_splitName, 0, true, "defaultTreatment", _conditions, "tt", _changeNumber, 2);
        Mockito.when(_splitFetcher.fetch(_splitName)).thenReturn(split);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(_matchingKey, _bucketingKey, _splitName, null, _splitClient);

        assertEquals("defaultTreatment", result.treatment);
        assertEquals("killed", result.label);
        assertEquals(_changeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithoutConditionsReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        ParsedSplit split = ParsedSplit.createParsedSplitForTests(_splitName, 0, false, "defaultTreatment", _conditions, "tt", _changeNumber, 2);
        Mockito.when(_splitFetcher.fetch(_splitName)).thenReturn(split);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(_matchingKey, _bucketingKey, _splitName, null, _splitClient);

        assertEquals("defaultTreatment", result.treatment);
        assertEquals("default rule", result.label);
        assertEquals(_changeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithRollOutConditionBucketIsBiggerTrafficAllocationReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        Partition partition = new Partition();
        partition.treatment = "treatment_test";
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.ROLLOUT, _matcher,_partitions, "test label");
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(_splitName, 0, false, "default_treatment", _conditions, "tt", _changeNumber, 10, 12, 2, _configurations);

        Mockito.when(_splitFetcher.fetch(_splitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(_matchingKey, _bucketingKey, null, _splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(_matchingKey, _bucketingKey, _splitName, null, _splitClient);

        assertEquals("default_treatment", result.treatment);
        assertEquals("not in split", result.label);
        assertEquals(_changeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithRollOutConditionTrafficAllocationIsBiggerBucketReturnTreatment() throws ChangeNumberExceptionWrapper {
        Partition partition = new Partition();
        partition.treatment = "treatment_test";
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.ROLLOUT, _matcher, _partitions, "test label");
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(_splitName, 0, false, "default_treatment", _conditions, "tt", _changeNumber, 60, 18, 2, _configurations);

        Mockito.when(_splitFetcher.fetch(_splitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(_matchingKey, _bucketingKey, null, _splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(_matchingKey, _bucketingKey, _splitName, null, _splitClient);

        assertEquals("treatment_test", result.treatment);
        assertEquals("test label", result.label);
        assertEquals(_changeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithWhitelistConditionReturnTreatment() throws ChangeNumberExceptionWrapper {
        Partition partition = new Partition();
        partition.treatment = "treatment_test";
        partition.size = 100;
        _partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.WHITELIST, _matcher, _partitions, "test whitelist label");
        _conditions.add(condition);

        ParsedSplit split = new ParsedSplit(_splitName, 0, false, "default_treatment", _conditions, "tt", _changeNumber, 60, 18, 2, _configurations);

        Mockito.when(_splitFetcher.fetch(_splitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(_matchingKey, _bucketingKey, null, _splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = _evaluator.evaluateFeature(_matchingKey, _bucketingKey, _splitName, null, _splitClient);

        assertEquals("treatment_test", result.treatment);
        assertEquals("test whitelist label", result.label);
        assertEquals(_changeNumber, result.changeNumber);
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
