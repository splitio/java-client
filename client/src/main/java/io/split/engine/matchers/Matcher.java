package io.split.engine.matchers;

import io.split.engine.evaluator.EvaluationContext;

import java.util.Map;

public interface Matcher {
    boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext evaluationContext);
}
