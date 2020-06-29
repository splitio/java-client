package io.split.engine.sse;

import io.split.engine.sse.dtos.IncomingNotification;
import io.split.engine.sse.dtos.SegmentChangeNotification;
import io.split.engine.sse.dtos.SplitChangeNotification;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.engine.sse.workers.SplitsWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationProcessorImp implements NotificationProcessor {
    private static final Logger _log = LoggerFactory.getLogger(NotificationProcessor.class);

    private final SplitsWorker _splitsWorker;

    public NotificationProcessorImp(SplitsWorker splitsWorker) {
        _splitsWorker = splitsWorker;
    }

    @Override
    public void process(IncomingNotification notification) {
        try {
            switch (notification.getType()){
                case SPLIT_UPDATE:
                    SplitChangeNotification splitChangeNotification = (SplitChangeNotification) notification;
                    _splitsWorker.addToQueue(splitChangeNotification.getChangeNumber());
                    break;
                case SEGMENT_UPDATE:
                    SegmentChangeNotification segmentChangeNotification = (SegmentChangeNotification) notification;
                    // TODO: implement this after segmentsWorker implementation
                    break;
                case SPLIT_KILL:
                    SplitKillNotification splitKillNotification = (SplitKillNotification) notification;
                    _splitsWorker.killSplit(splitKillNotification.getChangeNumber(), splitKillNotification.getSplitName(), splitKillNotification.getDefaultTreatment());
                    _splitsWorker.addToQueue(splitKillNotification.getChangeNumber());
                    break;
                default:
                    _log.error(String.format("Unknown notification arrived: %s", notification.toString()));
                    break;
            }
        }catch (Exception ex) {
            _log.error(String.format("Unknown error while processing incoming push notification: %s", ex.getMessage()));
        }
    }
}
