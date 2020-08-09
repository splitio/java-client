package io.split.engine.sse.workers;

import io.split.engine.common.Synchronizer;
import io.split.engine.sse.dtos.SegmentQueueDto;

import static com.google.common.base.Preconditions.checkNotNull;

public class SegmentsWorkerImp extends Worker<SegmentQueueDto> {
    private final Synchronizer _synchronizer;

    public SegmentsWorkerImp(Synchronizer synchronizer) {
        super("Segments");
        _synchronizer = checkNotNull(synchronizer);
    }

    @Override
    protected void executeRefresh(SegmentQueueDto segmentQueueDto) {
        _synchronizer.refreshSegment(segmentQueueDto.getSegmentName(), segmentQueueDto.getChangeNumber());
    }
}
