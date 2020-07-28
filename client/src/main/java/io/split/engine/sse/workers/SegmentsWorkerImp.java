package io.split.engine.sse.workers;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.segments.SegmentFetcher;
import io.split.engine.sse.dtos.SegmentQueueDto;

import static com.google.common.base.Preconditions.checkNotNull;

public class SegmentsWorkerImp extends Worker<SegmentQueueDto> {
    private final SegmentFetcher _segmentFetcher;

    @VisibleForTesting
    /* package private */ SegmentsWorkerImp(SegmentFetcher segmentFetcher) {
        super("Segments");
        _segmentFetcher = checkNotNull(segmentFetcher);
    }

    public static SegmentsWorkerImp build(SegmentFetcher segmentFetcher) {
        return new SegmentsWorkerImp(segmentFetcher);
    }

    @Override
    protected void executeRefresh(SegmentQueueDto segmentQueueDto) {
        if (segmentQueueDto.getChangeNumber() > _segmentFetcher.getChangeNumber(segmentQueueDto.getSegmentName())) {
            _segmentFetcher.forceRefresh(segmentQueueDto.getSegmentName());
        }
    }
}
