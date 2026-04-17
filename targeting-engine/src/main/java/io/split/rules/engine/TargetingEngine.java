package io.split.rules.engine;

import io.split.rules.exceptions.VersionedExceptionWrapper;
import io.split.rules.model.TargetingRule;

import java.util.Map;

/**
 * Evaluates a targeting rule against a key and attributes.
 * This is the core contract of the targeting engine.
 */
public interface TargetingEngine {
    EvaluationResult evaluate(String matchingKey, String bucketingKey,
                              TargetingRule rule, Map<String, Object> attributes,
                              EvaluationContext context) throws VersionedExceptionWrapper;
}
