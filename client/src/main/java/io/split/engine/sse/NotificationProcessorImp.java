package io.split.engine.sse;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.StatusNotification;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.Worker;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationProcessorImp implements NotificationProcessor {
    private final SplitsWorker _splitsWorker;
    private final Worker<SegmentQueueDto> _segmentWorker;
    private final NotificationManagerKeeper _notificationManagerKeeper;

    @VisibleForTesting
    /* package private */ NotificationProcessorImp(SplitsWorker splitsWorker,
                                                   Worker<SegmentQueueDto> segmentWorker,
                                                   NotificationManagerKeeper notificationManagerKeeper) {
        _splitsWorker = checkNotNull(splitsWorker);
        _segmentWorker = checkNotNull(segmentWorker);
        _notificationManagerKeeper = checkNotNull(notificationManagerKeeper);
    }

    public static NotificationProcessorImp build(SplitsWorker splitsWorker, Worker<SegmentQueueDto> segmentWorker, NotificationManagerKeeper notificationManagerKeeper) {
        return new NotificationProcessorImp(splitsWorker, segmentWorker, notificationManagerKeeper);
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

    @Override
    public void processStatus(StatusNotification statusNotification) {
        statusNotification.handlerStatus(_notificationManagerKeeper);
    }
}
