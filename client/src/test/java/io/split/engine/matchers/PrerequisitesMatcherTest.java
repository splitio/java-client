package io.split.engine.matchers;

import io.split.client.dtos.Prerequisites;
import io.split.client.utils.Json;
import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.evaluator.EvaluatorImp;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.SegmentCache;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for Prerequisites matcher
 */
public class PrerequisitesMatcherTest {

    @Test
    public void works() {
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        EvaluationContext evaluationContext = new EvaluationContext(evaluator, Mockito.mock(SegmentCache.class), Mockito.mock(RuleBasedSegmentCache.class));
        List<Prerequisites> prerequisites = Arrays.asList(Json.fromJson("{\"n\": \"split1\", \"ts\": [\"on\"]}", Prerequisites.class), Json.fromJson("{\"n\": \"split2\", \"ts\": [\"off\"]}", Prerequisites.class));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        Assert.assertEquals("prerequisites: split1 [on], split2 [off]", matcher.toString());
        PrerequisitesMatcher matcher2 = new PrerequisitesMatcher(prerequisites);
        Assert.assertTrue(matcher.equals(matcher2));
        Assert.assertTrue(matcher.hashCode() != 0);

        Mockito.when(evaluator.evaluateFeature("user", "user", "split1", null)).thenReturn(new EvaluatorImp.TreatmentLabelAndChangeNumber("on", ""));
        Mockito.when(evaluator.evaluateFeature("user", "user", "split2", null)).thenReturn(new EvaluatorImp.TreatmentLabelAndChangeNumber("off", ""));
        Assert.assertTrue(matcher.match("user", "user", null, evaluationContext));

        Mockito.when(evaluator.evaluateFeature("user", "user", "split2", null)).thenReturn(new EvaluatorImp.TreatmentLabelAndChangeNumber("on", ""));
        Assert.assertFalse(matcher.match("user", "user", null, evaluationContext));
    }

    @Test
    public void invalidParams() {
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        EvaluationContext evaluationContext = new EvaluationContext(evaluator, Mockito.mock(SegmentCache.class), Mockito.mock(RuleBasedSegmentCache.class));

        List<Prerequisites> prerequisites = Arrays.asList(Json.fromJson("{\"n\": \"split1\", \"ts\": [\"on\"]}", Prerequisites.class), Json.fromJson("{\"n\": \"split2\", \"ts\": [\"off\"]}", Prerequisites.class));
        PrerequisitesMatcher matcher = new PrerequisitesMatcher(prerequisites);
        Mockito.when(evaluator.evaluateFeature("user", "user", "split1", null)).thenReturn(new EvaluatorImp.TreatmentLabelAndChangeNumber("on", ""));
        Assert.assertFalse(matcher.match(null, null, null, evaluationContext));
        Assert.assertFalse(matcher.match(123, null, null, evaluationContext));
    }
}