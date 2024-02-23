package io.split.engine.sse;

import com.google.common.annotations.VisibleForTesting;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.engine.sse.dtos.StatusNotification;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.workers.FeatureFlagsWorker;
import io.split.engine.sse.workers.Worker;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationProcessorImp implements NotificationProcessor {
    private final FeatureFlagsWorker _featureFlagsWorker;
    private final Worker<SegmentQueueDto> _segmentWorker;
    private final PushStatusTracker _pushStatusTracker;

    @VisibleForTesting
    /* package private */ NotificationProcessorImp(FeatureFlagsWorker featureFlagsWorker,
                                                   Worker<SegmentQueueDto> segmentWorker,
                                                   PushStatusTracker pushStatusTracker) {
        _featureFlagsWorker = checkNotNull(featureFlagsWorker);
        _segmentWorker = checkNotNull(segmentWorker);
        _pushStatusTracker = checkNotNull(pushStatusTracker);
    }

    public static NotificationProcessorImp build(FeatureFlagsWorker featureFlagsWorker, Worker<SegmentQueueDto> segmentWorker,
                                                 PushStatusTracker pushStatusTracker) {
        return new NotificationProcessorImp(featureFlagsWorker, segmentWorker, pushStatusTracker);
    }

    @Override
    public void process(IncomingNotification notification) {
        notification.handler(this);
    }

    @Override
    public void processSplitUpdate(FeatureFlagChangeNotification featureFlagChangeNotification) {
        _featureFlagsWorker.addToQueue(featureFlagChangeNotification);
    }

    @Override
    public void processSplitKill(SplitKillNotification splitKillNotification) {
        _featureFlagsWorker.kill(splitKillNotification);
        _featureFlagsWorker.addToQueue(new FeatureFlagChangeNotification(GenericNotificationData.builder()
                .changeNumber(splitKillNotification.getChangeNumber())
                .channel(splitKillNotification.getChannel())
                .build()));
    }

    @Override
    public void processSegmentUpdate(long changeNumber, String segmentName) {
        _segmentWorker.addToQueue(new SegmentQueueDto(segmentName, changeNumber));
    }

    @Override
    public void processStatus(StatusNotification statusNotification) {
        statusNotification.handlerStatus(_pushStatusTracker);
    }
}
