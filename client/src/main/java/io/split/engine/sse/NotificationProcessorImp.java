package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.WorkerImp;

public class NotificationProcessorImp implements NotificationProcessor {
    private final SplitsWorker _splitsWorker;
    private final WorkerImp<SegmentQueueDto> _segmentWorker;

    public NotificationProcessorImp(SplitsWorker splitsWorker,
                                    WorkerImp<SegmentQueueDto> segmentWorker) {
        _splitsWorker = splitsWorker;
        _segmentWorker = segmentWorker;
    }

    @Override
    public void process(IncomingNotification notification) {
        notification.handler(this);
    }

    @Override
    public void processSplitUpdate(long changeNumber) {
        _splitsWorker.addToQueue(changeNumber);
    }

    @Override
    public void processSplitKill(long changeNumber, String splitName, String defaultTreatment) {
        _splitsWorker.killSplit(changeNumber, splitName, defaultTreatment);
        _splitsWorker.addToQueue(changeNumber);
    }

    @Override
    public void processSegmentUpdate(long changeNumber, String segmentName) {
        _segmentWorker.addToQueue(new SegmentQueueDto(segmentName, changeNumber));
    }
}
