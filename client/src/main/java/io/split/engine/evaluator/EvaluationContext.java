package io.split.engine.evaluator;

import io.split.storages.SegmentCacheConsumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class EvaluationContext {
    private final Evaluator _evaluator;
    private final SegmentCacheConsumer _segmentCacheConsumer;

    public EvaluationContext(Evaluator evaluator, SegmentCacheConsumer segmentCacheConsumer) {
        _evaluator = checkNotNull(evaluator);
        _segmentCacheConsumer = checkNotNull(segmentCacheConsumer);
    }

    public Evaluator getEvaluator() {
        return _evaluator;
    }

    public SegmentCacheConsumer getSegmentCache() {
        return _segmentCacheConsumer;
    }
}
