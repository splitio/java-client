package io.split.rules.engine;

import java.util.Map;

/**
 * Provides recursive evaluation and segment membership checks to matchers.
 * Each SDK implements this interface to bridge to its own storage and evaluator.
 */
public interface EvaluationContext {

    /**
     * Evaluates a targeting rule by name. Used by DependencyMatcher and PrerequisitesMatcher.
     */
    EvaluationResult evaluate(String matchingKey, String bucketingKey, String ruleName, Map<String, Object> attributes);

    /**
     * Checks if the given key is a member of a standard segment.
     */
    boolean isInSegment(String segmentName, String key);

    /**
     * Checks if the given key is a member of a rule-based segment.
     */
    boolean isInRuleBasedSegment(String segmentName, String key, String bucketingKey, Map<String, Object> attributes);
}
