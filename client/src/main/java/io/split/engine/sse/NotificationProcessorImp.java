package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.workers.SplitsWorker;

public class NotificationProcessorImp implements NotificationProcessor {
    private final SplitsWorker _splitsWorker;

    public NotificationProcessorImp(SplitsWorker splitsWorker) {
        _splitsWorker = splitsWorker;
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
        // TODO: implement this after segmentsWorker implementation
    }
}
