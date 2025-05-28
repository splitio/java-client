package io.split.engine.evaluator;

import io.split.storages.RuleBasedSegmentCacheConsumer;
import io.split.storages.SegmentCacheConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class EvaluationContext {
    private final Evaluator _evaluator;
    private final SegmentCacheConsumer _segmentCacheConsumer;
    private final RuleBasedSegmentCacheConsumer _ruleBasedSegmentCacheConsumer;

    public EvaluationContext(Evaluator evaluator, SegmentCacheConsumer segmentCacheConsumer,
                             RuleBasedSegmentCacheConsumer ruleBasedSegmentCacheConsumer) {
        _evaluator = checkNotNull(evaluator);
        _segmentCacheConsumer = checkNotNull(segmentCacheConsumer);
        _ruleBasedSegmentCacheConsumer = checkNotNull(ruleBasedSegmentCacheConsumer);
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
}
