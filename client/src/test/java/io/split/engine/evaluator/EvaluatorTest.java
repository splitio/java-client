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
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EvaluatorTest {
    @Test
    public void evaluateWhenSplitNameDoesNotExistReturnControl() throws ChangeNumberExceptionWrapper {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        SDKReadinessGates gates = Mockito.mock(SDKReadinessGates.class);
        Evaluator evaluator = new EvaluatorImp(gates, splitFetcher);

        SplitClientImpl splitClient = getSplitClient(splitFetcher, gates, evaluator);

        String matchingKey = "test";
        String bucketingKey = "test";
        String split = "split_name_test";

        Mockito.when(splitFetcher.fetch(split)).thenReturn(null);

        TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature(matchingKey, bucketingKey, split, null, splitClient);

        assertEquals("control", result.treatment);
        assertEquals("definition not found", result.label);
    }

    @Test
    public void evaluateWhenSplitIsKilledReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        SDKReadinessGates gates = Mockito.mock(SDKReadinessGates.class);
        Evaluator evaluator = new EvaluatorImp(gates, splitFetcher);

        SplitClientImpl splitClient = getSplitClient(splitFetcher, gates, evaluator);

        String matchingKey = "test";
        String bucketingKey = "test";
        String splitName = "split_name_test";
        Long changeNumber = 123123L;
        List<ParsedCondition> conditions = new ArrayList<>();

        ParsedSplit split = ParsedSplit.createParsedSplitForTests(splitName, 0, true, "defaultTreatment", conditions, "tt", changeNumber, 2);

        Mockito.when(splitFetcher.fetch(splitName)).thenReturn(split);

        TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature(matchingKey, bucketingKey, splitName, null, splitClient);

        assertEquals("defaultTreatment", result.treatment);
        assertEquals("killed", result.label);
        assertEquals(changeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithoutConditionsReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        SDKReadinessGates gates = Mockito.mock(SDKReadinessGates.class);
        Evaluator evaluator = new EvaluatorImp(gates, splitFetcher);

        SplitClientImpl splitClient = getSplitClient(splitFetcher, gates, evaluator);

        String matchingKey = "test";
        String bucketingKey = "test";
        String splitName = "split_name_test";
        Long changeNumber = 123123L;
        List<ParsedCondition> conditions = new ArrayList<>();

        ParsedSplit split = ParsedSplit.createParsedSplitForTests(splitName, 0, false, "defaultTreatment", conditions, "tt", changeNumber, 2);

        Mockito.when(splitFetcher.fetch(splitName)).thenReturn(split);

        TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature(matchingKey, bucketingKey, splitName, null, splitClient);

        assertEquals("defaultTreatment", result.treatment);
        assertEquals("default rule", result.label);
        assertEquals(changeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithRollOutConditionBucketIsBiggerTrafficAllocationReturnDefaultTreatment() throws ChangeNumberExceptionWrapper {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        SDKReadinessGates gates = Mockito.mock(SDKReadinessGates.class);
        Evaluator evaluator = new EvaluatorImp(gates, splitFetcher);

        SplitClientImpl splitClient = getSplitClient(splitFetcher, gates, evaluator);

        String matchingKey = "test";
        String bucketingKey = "test";
        String splitName = "split_name_test";
        Long changeNumber = 123123L;
        Map<String, String> configurations = new HashMap<>();
        List<ParsedCondition> conditions = new ArrayList<>();

        CombiningMatcher matcher = Mockito.mock(CombiningMatcher.class);

        List<Partition> partitions = new ArrayList<>();
        Partition partition = new Partition();
        partition.treatment = "treatment_test";
        partition.size = 100;
        partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.ROLLOUT, matcher, partitions, "test label");
        conditions.add(condition);

        ParsedSplit split = new ParsedSplit(splitName, 0, false, "default_treatment", conditions, "tt", changeNumber, 10, 12, 2, configurations);

        Mockito.when(splitFetcher.fetch(splitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(matchingKey, bucketingKey, null, splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature(matchingKey, bucketingKey, splitName, null, splitClient);

        assertEquals("default_treatment", result.treatment);
        assertEquals("not in split", result.label);
        assertEquals(changeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithRollOutConditionTrafficAllocationIsBiggerBucketReturnTreatment() throws ChangeNumberExceptionWrapper {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        SDKReadinessGates gates = Mockito.mock(SDKReadinessGates.class);
        Evaluator evaluator = new EvaluatorImp(gates, splitFetcher);
        CombiningMatcher matcher = Mockito.mock(CombiningMatcher.class);

        SplitClientImpl splitClient = getSplitClient(splitFetcher, gates, evaluator);

        String matchingKey = "test";
        String bucketingKey = "test";
        String splitName = "split_name_test";
        Long changeNumber = 123123L;
        Map<String, String> configurations = new HashMap<>();
        List<ParsedCondition> conditions = new ArrayList<>();
        List<Partition> partitions = new ArrayList<>();

        Partition partition = new Partition();
        partition.treatment = "treatment_test";
        partition.size = 100;
        partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.ROLLOUT, matcher, partitions, "test label");
        conditions.add(condition);

        ParsedSplit split = new ParsedSplit(splitName, 0, false, "default_treatment", conditions, "tt", changeNumber, 60, 18, 2, configurations);

        Mockito.when(splitFetcher.fetch(splitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(matchingKey, bucketingKey, null, splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature(matchingKey, bucketingKey, splitName, null, splitClient);

        assertEquals("treatment_test", result.treatment);
        assertEquals("test label", result.label);
        assertEquals(changeNumber, result.changeNumber);
    }

    @Test
    public void evaluateWithWhitelistConditionReturnTreatment() throws ChangeNumberExceptionWrapper {
        SplitFetcher splitFetcher = Mockito.mock(SplitFetcher.class);
        SDKReadinessGates gates = Mockito.mock(SDKReadinessGates.class);
        Evaluator evaluator = new EvaluatorImp(gates, splitFetcher);
        CombiningMatcher matcher = Mockito.mock(CombiningMatcher.class);

        SplitClientImpl splitClient = getSplitClient(splitFetcher, gates, evaluator);

        String matchingKey = "test";
        String bucketingKey = "test";
        String splitName = "split_name_test";
        Long changeNumber = 123123L;
        Map<String, String> configurations = new HashMap<>();
        List<ParsedCondition> conditions = new ArrayList<>();
        List<Partition> partitions = new ArrayList<>();

        Partition partition = new Partition();
        partition.treatment = "treatment_test";
        partition.size = 100;
        partitions.add(partition);
        ParsedCondition condition = new ParsedCondition(ConditionType.WHITELIST, matcher, partitions, "test whitelist label");
        conditions.add(condition);

        ParsedSplit split = new ParsedSplit(splitName, 0, false, "default_treatment", conditions, "tt", changeNumber, 60, 18, 2, configurations);

        Mockito.when(splitFetcher.fetch(splitName)).thenReturn(split);
        Mockito.when(condition.matcher().match(matchingKey, bucketingKey, null, splitClient)).thenReturn(true);

        TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature(matchingKey, bucketingKey, splitName, null, splitClient);

        assertEquals("treatment_test", result.treatment);
        assertEquals("test whitelist label", result.label);
        assertEquals(changeNumber, result.changeNumber);
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
