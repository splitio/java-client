package io.split.engine.evaluator;

import io.split.client.dtos.ExcludedSegments;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.rules.engine.EvaluationResult;
import io.split.storages.RuleBasedSegmentCacheConsumer;
import io.split.storages.SegmentCacheConsumer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EvaluationContext implements io.split.rules.engine.EvaluationContext {
    private final Evaluator _evaluator;
    private final SegmentCacheConsumer _segmentCacheConsumer;
    private final RuleBasedSegmentCacheConsumer _ruleBasedSegmentCacheConsumer;

    public EvaluationContext(Evaluator evaluator, SegmentCacheConsumer segmentCacheConsumer,
                             RuleBasedSegmentCacheConsumer ruleBasedSegmentCacheConsumer) {
        _evaluator = Objects.requireNonNull(evaluator);
        _segmentCacheConsumer = Objects.requireNonNull(segmentCacheConsumer);
        _ruleBasedSegmentCacheConsumer = Objects.requireNonNull(ruleBasedSegmentCacheConsumer);
    }

    public Evaluator getEvaluator() {
        return _evaluator;
    }

    public SegmentCacheConsumer getSegmentCache() {
        return _segmentCacheConsumer;
    }

    public RuleBasedSegmentCacheConsumer getRuleBasedSegmentCache() {
        return _ruleBasedSegmentCacheConsumer;
    }

    @Override
    public EvaluationResult evaluate(String matchingKey, String bucketingKey, String ruleName, Map<String, Object> attributes) {
        EvaluatorImp.TreatmentLabelAndChangeNumber r = _evaluator.evaluateFeature(matchingKey, bucketingKey, ruleName, attributes);
        return new EvaluationResult(r.treatment, r.label);
    }

    @Override
    public boolean isInSegment(String segmentName, String key) {
        return _segmentCacheConsumer.isInSegment(segmentName, key);
    }

    @Override
    public boolean isInRuleBasedSegment(String segmentName, String key, String bucketingKey, Map<String, Object> attributes) {
        ParsedRuleBasedSegment parsedRuleBasedSegment = _ruleBasedSegmentCacheConsumer.get(segmentName);
        if (parsedRuleBasedSegment == null) {
            return false;
        }
        if (parsedRuleBasedSegment.excludedKeys().contains(key)) {
            return false;
        }
        for (ExcludedSegments excludedSegment : parsedRuleBasedSegment.excludedSegments()) {
            if (excludedSegment.isStandard() && _segmentCacheConsumer.isInSegment(excludedSegment.name, key)) {
                return false;
            }
            if (excludedSegment.isRuleBased() && isInRuleBasedSegment(excludedSegment.name, key, bucketingKey, attributes)) {
                return false;
            }
        }
        for (ParsedCondition condition : parsedRuleBasedSegment.parsedConditions()) {
            if (condition.matcher().match(key, bucketingKey, attributes, this)) {
                return true;
            }
        }
        return false;
    }
}
