package io.split.engine.sse;

import io.split.engine.sse.dtos.*;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.WorkerImp;
import org.junit.Test;
import org.mockito.Mockito;

public class NotificationProcessorTest {
    private final SplitsWorker _splitsWorker;
    private final WorkerImp<SegmentQueueDto> _segmentWorker;
    private final NotificationProcessor _notificationProcessor;

    public NotificationProcessorTest() {
        _splitsWorker = Mockito.mock(SplitsWorker.class);
        _segmentWorker = Mockito.mock(SegmentsWorkerImp.class);

        _notificationProcessor = new NotificationProcessorImp(_splitsWorker, _segmentWorker);
    }

    @Test
    public void processSplitUpdateAddToQueueInWorker() {
        long changeNumber = 1585867723838L;
        String channel = "splits";
        GenericNotificationData genericNotificationData = new GenericNotificationData(changeNumber, null, null, null, null, null, null, channel);
        SplitChangeNotification splitChangeNotification = new SplitChangeNotification(genericNotificationData);

        _notificationProcessor.process(splitChangeNotification);

        Mockito.verify(_splitsWorker, Mockito.times(1)).addToQueue(splitChangeNotification.getChangeNumber());
    }

    @Test
    public void processSplitKillAndAddToQueueInWorker() {
        long changeNumber = 1585867723838L;
        String defaultTreatment = "off";
        String splitName = "test-split";
        String channel = "splits";
        GenericNotificationData genericNotificationData = new GenericNotificationData(changeNumber, defaultTreatment, splitName, null, null, null, null, channel);
        SplitKillNotification splitKillNotification = new SplitKillNotification(genericNotificationData);

        _notificationProcessor.process(splitKillNotification);

        Mockito.verify(_splitsWorker, Mockito.times(1)).killSplit(splitKillNotification.getChangeNumber(), splitKillNotification.getSplitName(), splitKillNotification.getDefaultTreatment());
        Mockito.verify(_splitsWorker, Mockito.times(1)).addToQueue(splitKillNotification.getChangeNumber());
    }

    @Test
    public void processSegmentUpdateAddToQueueInWorker() {
        long changeNumber = 1585867723838L;
        String segmentName = "segment-test";
        String channel = "segments";
        GenericNotificationData genericNotificationData = new GenericNotificationData(changeNumber, null, null, null, null, segmentName, null, channel);
        SegmentChangeNotification segmentChangeNotification = new SegmentChangeNotification(genericNotificationData);

        _notificationProcessor.process(segmentChangeNotification);

        Mockito.verify(_segmentWorker, Mockito.times(1)).addToQueue(Mockito.any(SegmentQueueDto.class));
    }
}
