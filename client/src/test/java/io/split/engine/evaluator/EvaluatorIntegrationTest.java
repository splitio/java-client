package io.split.engine.evaluator;

import com.google.common.collect.Lists;
import io.split.cache.InMemoryCacheImp;
import io.split.cache.SplitCache;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.Partition;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedSplit;
import io.split.engine.matchers.AttributeMatcher;
import io.split.engine.matchers.CombiningMatcher;
import io.split.engine.matchers.strings.WhitelistMatcher;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class EvaluatorIntegrationTest {
    private static final String DEFAULT_TREATMENT_VALUE = "defaultTreatment";
    private static final String TRAFFIC_TYPE_VALUE = "tt";
    private static final String TEST_LABEL_VALUE = "test label";

    @Test
    public void TestTestInMemory() {
        SplitCache splitCache = new InMemoryCacheImp();
        Evaluator evaluator = new EvaluatorImp(splitCache);

        Partition partition = new Partition();
        partition.treatment = "on";
        partition.size = 50;

        List<Partition> partitions = new ArrayList<>();
        partitions.add(partition);

        AttributeMatcher whitelistAtt = AttributeMatcher.vanilla(new WhitelistMatcher(Lists.newArrayList("test_1", "admin")));
        CombiningMatcher combiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(whitelistAtt));

        ParsedCondition condition = new ParsedCondition(ConditionType.WHITELIST, combiningMatcher, partitions, TEST_LABEL_VALUE);

        List<ParsedCondition> conditions = new ArrayList<>();
        conditions.add(condition);

        splitCache.put(ParsedSplit.createParsedSplitForTests("split_1", 0, false, DEFAULT_TREATMENT_VALUE, conditions, TRAFFIC_TYPE_VALUE, 223366551, 2));
        splitCache.put(ParsedSplit.createParsedSplitForTests("split_2", 0, true, DEFAULT_TREATMENT_VALUE, conditions, TRAFFIC_TYPE_VALUE, 223366552, 2));
        splitCache.put(ParsedSplit.createParsedSplitForTests("split_3", 0, false, DEFAULT_TREATMENT_VALUE, conditions, TRAFFIC_TYPE_VALUE, 223366553, 2));

        EvaluatorImp.TreatmentLabelAndChangeNumber result = evaluator.evaluateFeature("test_1", null, "split_3", null);
        EvaluatorImp.TreatmentLabelAndChangeNumber result1 = evaluator.evaluateFeature("test_1", null, "split_3", null);
        EvaluatorImp.TreatmentLabelAndChangeNumber result2 = evaluator.evaluateFeature("test_1", null, "split_3", null);
        EvaluatorImp.TreatmentLabelAndChangeNumber result3 = evaluator.evaluateFeature("test_1", null, "split_3", null);
    }
}
