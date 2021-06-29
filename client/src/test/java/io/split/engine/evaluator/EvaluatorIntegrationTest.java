package io.split.engine.evaluator;

import com.google.common.collect.Lists;
import io.split.storages.memory.InMemoryCacheImp;
import io.split.storages.SplitCache;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.Partition;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.strings.EndsWithAnyOfMatcher;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class EvaluatorIntegrationTest {
    private static final String DEFAULT_TREATMENT_VALUE = "defaultTreatment";
    private static final String TRAFFIC_TYPE_VALUE = "tt";
    private static final String TEST_LABEL_VALUE_WHITELIST = "test label whitelist";
    private static final String TEST_LABEL_VALUE_ROLL_OUT = "test label roll out";
    private static final String ON_TREATMENT = "on";

    @Test
    public void evaluateFeatureWithWhitelistShouldReturnOn() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 100);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("test_1", null, "split_3", null);
        Assert.assertEquals(ON_TREATMENT, result.treatment);
        Long changeNumberExpected = 223366554L;
        Assert.assertEquals(changeNumberExpected, result.changeNumber);
        Assert.assertEquals(TEST_LABEL_VALUE_WHITELIST, result.label);
    }

    @Test
    public void evaluateFeatureWithWhitelistShouldReturnDefault() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 100);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("test_2", null, "split_3", null);
        Assert.assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        Long changeNumberExpected = 223366554L;
        Assert.assertEquals(changeNumberExpected, result.changeNumber);
        Assert.assertEquals(Labels.DEFAULT_RULE, result.label);
    }

    @Test
    public void evaluateFeatureWithWhitelistWhenSplitIsKilledShouldReturnDefaultTreatment() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 100);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("test_1", null, "split_2", null);
        Assert.assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        Long changeNumberExpected = 223366552L;
        Assert.assertEquals(changeNumberExpected, result.changeNumber);
        Assert.assertEquals(Labels.KILLED, result.label);
    }

    @Test
    public void evaluateFeatureWithRollOutShouldReturnDefault() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 100);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("@mail2.com", null, "split_1", null);
        Assert.assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        Long changeNumberExpected = 223366551L;
        Assert.assertEquals(changeNumberExpected, result.changeNumber);
        Assert.assertEquals(Labels.DEFAULT_RULE, result.label);
    }

    @Test
    public void evaluateFeatureWithRollOutShouldReturnOn() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 100);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("mauro@test.io", null, "split_1", null);
        Assert.assertEquals(ON_TREATMENT, result.treatment);
        Long changeNumberExpected = 223366551L;
        Assert.assertEquals(changeNumberExpected, result.changeNumber);
        Assert.assertEquals(TEST_LABEL_VALUE_ROLL_OUT, result.label);
    }

    @Test
    public void evaluateFeatureWithRollOutShouldReturnDefaultOutOfSplit() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 20);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("mauro@test.io", null, "split_test", null);
        Assert.assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        Long changeNumberExpected = 223366555L;
        Assert.assertEquals(changeNumberExpected, result.changeNumber);
        Assert.assertEquals(Labels.NOT_IN_SPLIT, result.label);
    }

    @Test
    public void evaluateFeatureWithRollOutWhenTrafficAllocationIs50ShouldReturnOn() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 50);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("mauro@test.io", null, "split_test", null);
        Assert.assertEquals(ON_TREATMENT, result.treatment);
        Long changeNumberExpected = 223366555L;
        Assert.assertEquals(changeNumberExpected, result.changeNumber);
        Assert.assertEquals(TEST_LABEL_VALUE_ROLL_OUT, result.label);
    }

    @Test
    public void evaluateFeatureWithRollOutWhenSplitIsKilledShouldReturnDefault() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 100);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("mauro@test.io", null, "split_2", null);
        Assert.assertEquals(DEFAULT_TREATMENT_VALUE, result.treatment);
        Long changeNumberExpected = 223366552L;
        Assert.assertEquals(changeNumberExpected, result.changeNumber);
        Assert.assertEquals(Labels.KILLED, result.label);
    }

    @Test
    public void evaluateFeatureWhenSplitNotExistsShouldReturnControl() {
        Evaluator evaluator = buildEvaluatorAndLoadCache(false, 100);

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("mauro@test.io", null, "test", null);
        Assert.assertEquals("control", result.treatment);
        Assert.assertEquals(Labels.DEFINITION_NOT_FOUND, result.label);
    }

    private Evaluator buildEvaluatorAndLoadCache(boolean killed, int trafficAllocation) {
        SplitCache splitCache = new InMemoryCacheImp();
        Evaluator evaluator = new EvaluatorImp(splitCache);

        Partition partition = new Partition();
        partition.treatment = ON_TREATMENT;
        partition.size = 100;

        List<Partition> partitions = Lists.newArrayList(partition);

        AttributeMatcher whiteListMatcher = AttributeMatcher.vanilla(new WhitelistMatcher(Lists.newArrayList("test_1", "admin")));
        AttributeMatcher endsWithMatcher = AttributeMatcher.vanilla(new EndsWithAnyOfMatcher(Lists.newArrayList("@test.io", "@mail.io")));

        CombiningMatcher whitelistCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(whiteListMatcher));
        CombiningMatcher endsWithCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(endsWithMatcher));

        ParsedCondition whitelistCondition = new ParsedCondition(ConditionType.WHITELIST, whitelistCombiningMatcher, partitions, TEST_LABEL_VALUE_WHITELIST);
        ParsedCondition rollOutCondition = new ParsedCondition(ConditionType.ROLLOUT, endsWithCombiningMatcher, partitions, TEST_LABEL_VALUE_ROLL_OUT);

        List<ParsedCondition> conditions = Lists.newArrayList(whitelistCondition, rollOutCondition);

        splitCache.put(new ParsedSplit("split_1", 0, false, DEFAULT_TREATMENT_VALUE, conditions, TRAFFIC_TYPE_VALUE, 223366551, 100, 0, 2, null));
        splitCache.put(new ParsedSplit("split_2", 0, true, DEFAULT_TREATMENT_VALUE, conditions, TRAFFIC_TYPE_VALUE, 223366552, 100, 0, 2, null));
        splitCache.put(new ParsedSplit("split_3", 0, false, DEFAULT_TREATMENT_VALUE, conditions, TRAFFIC_TYPE_VALUE, 223366554, 100, 0, 2, null));
        splitCache.put(new ParsedSplit("split_test", 0, killed, DEFAULT_TREATMENT_VALUE, conditions, TRAFFIC_TYPE_VALUE, 223366555, trafficAllocation, 0, 2, null));

        return evaluator;
    }
}
