package io.split.rules.engine;

import io.split.rules.matchers.AllKeysMatcher;
import io.split.rules.matchers.CombiningMatcher;
import io.split.rules.model.Condition;
import io.split.rules.model.ConditionType;
import io.split.rules.model.Partition;
import io.split.rules.model.Prerequisite;
import io.split.rules.model.TargetingRule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TargetingEngineImplTest {

    private TargetingEngine _engine;
    private EvaluationContext _context;

    @Before
    public void setUp() {
        _engine = new TargetingEngineImpl();
        _context = Mockito.mock(EvaluationContext.class);
    }

    private TargetingRule buildRule(boolean killed, List<Condition> conditions, List<Prerequisite> prerequisites,
                                    int trafficAllocation) {
        return new TargetingRule(
                "test_flag", 12345, killed, "off",
                conditions, "user", 1L,
                trafficAllocation, 12345, 2,
                null, new HashSet<>(), false,
                prerequisites
        );
    }

    private Condition rolloutCondition(String treatment) {
        return new Condition(
                ConditionType.ROLLOUT,
                CombiningMatcher.of(new AllKeysMatcher()),
                Collections.singletonList(new Partition(treatment, 100)),
                "test label"
        );
    }

    private Condition whitelistCondition(String treatment) {
        return new Condition(
                ConditionType.WHITELIST,
                CombiningMatcher.of(new AllKeysMatcher()),
                Collections.singletonList(new Partition(treatment, 100)),
                "whitelist label"
        );
    }

    @Test
    public void killedRuleReturnsDefaultTreatment() throws Exception {
        TargetingRule rule = buildRule(true, Collections.singletonList(rolloutCondition("on")), null, 100);
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals("off", result.treatment);
        assertEquals(EvaluationLabels.KILLED, result.label);
        assertEquals(Long.valueOf(1L), result.version);
    }

    @Test
    public void emptyConditionsReturnsDefaultRule() throws Exception {
        TargetingRule rule = buildRule(false, Collections.<Condition>emptyList(), null, 100);
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals("off", result.treatment);
        assertEquals(EvaluationLabels.DEFAULT_RULE, result.label);
    }

    @Test
    public void matchingConditionReturnsTreatment() throws Exception {
        TargetingRule rule = buildRule(false, Collections.singletonList(rolloutCondition("on")), null, 100);
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals("on", result.treatment);
        assertEquals("test label", result.label);
    }

    @Test
    public void whitelistConditionMatchesBeforeTrafficAllocation() throws Exception {
        TargetingRule rule = buildRule(false, Collections.singletonList(whitelistCondition("on")), null, 0);
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals("on", result.treatment);
        assertEquals("whitelist label", result.label);
    }

    @Test
    public void trafficAllocationZeroReturnsNotInSplit() throws Exception {
        TargetingRule rule = buildRule(false, Collections.singletonList(rolloutCondition("on")), null, 0);
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals("off", result.treatment);
        assertEquals(EvaluationLabels.NOT_IN_SPLIT, result.label);
    }

    @Test
    public void prerequisitesNotMetReturnsDefaultTreatment() throws Exception {
        Mockito.when(_context.evaluate("user1", "user1", "prereq_flag", null))
                .thenReturn(new EvaluationResult("off", EvaluationLabels.DEFAULT_RULE));
        List<Prerequisite> prereqs = Collections.singletonList(
                new Prerequisite("prereq_flag", Collections.singletonList("on"))
        );
        TargetingRule rule = buildRule(false, Collections.singletonList(rolloutCondition("on")), prereqs, 100);
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals("off", result.treatment);
        assertEquals(EvaluationLabels.PREREQUISITES_NOT_MET, result.label);
    }

    @Test
    public void prerequisitesMetProceedsToEvaluation() throws Exception {
        Mockito.when(_context.evaluate("user1", "user1", "prereq_flag", null))
                .thenReturn(new EvaluationResult("on", EvaluationLabels.DEFAULT_RULE));
        List<Prerequisite> prereqs = Collections.singletonList(
                new Prerequisite("prereq_flag", Collections.singletonList("on"))
        );
        TargetingRule rule = buildRule(false, Collections.singletonList(rolloutCondition("on")), prereqs, 100);
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals("on", result.treatment);
    }

    @Test
    public void bucketingKeyUsedWhenProvided() throws Exception {
        TargetingRule rule = buildRule(false, Collections.singletonList(rolloutCondition("on")), null, 100);
        EvaluationResult result = _engine.evaluate("user1", "bucket_user", rule, null, _context);
        assertEquals("on", result.treatment);
    }

    @Test
    public void configReturnedForTreatment() throws Exception {
        Map<String, String> configs = new HashMap<>();
        configs.put("on", "{\"color\":\"red\"}");
        TargetingRule rule = new TargetingRule(
                "test_flag", 12345, false, "off",
                Collections.singletonList(rolloutCondition("on")), "user", 1L,
                100, 12345, 2, configs, new HashSet<>(), false, null
        );
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals("on", result.treatment);
        assertEquals("{\"color\":\"red\"}", result.config);
    }

    @Test
    public void impressionsDisabledPreserved() throws Exception {
        TargetingRule rule = new TargetingRule(
                "test_flag", 12345, false, "off",
                Collections.singletonList(rolloutCondition("on")), "user", 1L,
                100, 12345, 2, null, new HashSet<>(), true, null
        );
        EvaluationResult result = _engine.evaluate("user1", null, rule, null, _context);
        assertEquals(true, result.impressionsDisabled);
    }
}
