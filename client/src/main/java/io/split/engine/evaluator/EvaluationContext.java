package io.split.engine.evaluator;

import io.split.cache.SegmentCache;

import static com.google.common.base.Preconditions.checkNotNull;

public class EvaluationContext {
    private final Evaluator _evaluator;
    private final SegmentCache _segmentCache;

    public EvaluationContext(Evaluator evaluator, SegmentCache segmentCache) {
        _evaluator = checkNotNull(evaluator);
        _segmentCache = checkNotNull(segmentCache);
    }

    public Evaluator getEvaluator() {
        return _evaluator;
    }

    public SegmentCache getSegmentCache() {
        return _segmentCache;
    }
}
