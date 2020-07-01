package io.split.engine.sse.workers;

import io.split.engine.segments.SegmentFetcher;
import io.split.engine.sse.dtos.SegmentQueueDto;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public class SegmentsWorkerImp extends WorkerImp<SegmentQueueDto> {
    private final SegmentFetcher _segmentFetcher;

    public SegmentsWorkerImp(SegmentFetcher segmentFetcher) {
        super(LoggerFactory.getLogger(SegmentsWorkerImp.class), new LinkedBlockingQueue<>(), "Segments");
        _segmentFetcher = segmentFetcher;
    }

    @Override
    protected void executeRefresh(SegmentQueueDto segmentQueueDto) {
        if (segmentQueueDto.getChangeNumber() > _segmentFetcher.getChangeNumber(segmentQueueDto.getSegmentName())) {
            _segmentFetcher.forceRefresh(segmentQueueDto.getSegmentName());
        }
    }
}
