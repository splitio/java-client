package io.split.rules.matchers;

import io.split.rules.engine.EvaluationContext;

import java.util.Map;

public interface Matcher {
    boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, EvaluationContext context);
}
