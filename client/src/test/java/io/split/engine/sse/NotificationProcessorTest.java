package io.split.engine.sse;

import io.split.engine.sse.dtos.*;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.Worker;
import org.junit.Test;
import org.mockito.Mockito;

public class NotificationProcessorTest {
    private final SplitsWorker _splitsWorker;
    private final Worker<SegmentQueueDto> _segmentWorker;
    private final NotificationProcessor _notificationProcessor;
    private final NotificationManagerKeeper _notificationManagerKeeper;

    public NotificationProcessorTest() {
        _splitsWorker = Mockito.mock(SplitsWorker.class);
        _segmentWorker = Mockito.mock(SegmentsWorkerImp.class);
        _notificationManagerKeeper = Mockito.mock(NotificationManagerKeeper.class);

        _notificationProcessor = new NotificationProcessorImp(_splitsWorker, _segmentWorker, _notificationManagerKeeper);
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

    @Test
    public void processControlNotification() {
        GenericNotificationData genericNotificationData = Mockito.mock(GenericNotificationData.class);
        ControlNotification controlNotification = new ControlNotification(genericNotificationData);

        _notificationProcessor.process(controlNotification);

        Mockito.verify(_notificationManagerKeeper, Mockito.times(1)).handleIncomingControlEvent(Mockito.any(ControlNotification.class));
    }

    @Test
    public void processOccupancyNotification() {
        GenericNotificationData genericNotificationData = new GenericNotificationData(null, null, null, null, null, null, null, "control_pri");
        OccupancyNotification occupancyNotification = new OccupancyNotification(genericNotificationData);

        _notificationProcessor.process(occupancyNotification);

        Mockito.verify(_notificationManagerKeeper, Mockito.times(1)).handleIncomingOccupancyEvent(Mockito.any(OccupancyNotification.class));
    }
}
