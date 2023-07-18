package io.split.engine.sse;

import io.split.engine.sse.dtos.ControlNotification;
import io.split.engine.sse.dtos.FeatureFlagChangeNotification;
import io.split.engine.sse.dtos.GenericNotificationData;
import io.split.engine.sse.dtos.OccupancyNotification;
import io.split.engine.sse.dtos.SegmentChangeNotification;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.dtos.SplitKillNotification;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.FeatureFlagsWorker;
import io.split.engine.sse.workers.Worker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class NotificationProcessorTest {
    private FeatureFlagsWorker _featureFlagsWorker;
    private Worker<SegmentQueueDto> _segmentWorker;
    private NotificationProcessor _notificationProcessor;
    private PushStatusTracker _pushStatusTracker;

    @Before
    public void setUp() {
        _featureFlagsWorker = Mockito.mock(FeatureFlagsWorker.class);
        _segmentWorker = Mockito.mock(SegmentsWorkerImp.class);
        _pushStatusTracker = Mockito.mock(PushStatusTracker.class);

        _notificationProcessor = new NotificationProcessorImp(_featureFlagsWorker, _segmentWorker, _pushStatusTracker);
    }

    @Test
    public void processSplitUpdateAddToQueueInWorker() {
        long changeNumber = 1585867723838L;
        String channel = "splits";
        GenericNotificationData genericNotificationData = GenericNotificationData.builder()
                .changeNumber(changeNumber)
                .channel(channel)
                .build();
        FeatureFlagChangeNotification splitChangeNotification = new FeatureFlagChangeNotification(genericNotificationData);

        _notificationProcessor.process(splitChangeNotification);

        Mockito.verify(_featureFlagsWorker, Mockito.times(1)).addToQueue(Mockito.anyObject());
    }

    @Test
    public void processSplitKillAndAddToQueueInWorker() {
        long changeNumber = 1585867723838L;
        String defaultTreatment = "off";
        String splitName = "test-split";
        String channel = "splits";
        GenericNotificationData genericNotificationData = GenericNotificationData.builder()
                .changeNumber(changeNumber)
                .defaultTreatment(defaultTreatment)
                .featureFlagName(splitName)
                .channel(channel)
                .build();
        SplitKillNotification splitKillNotification = new SplitKillNotification(genericNotificationData);

        _notificationProcessor.process(splitKillNotification);

        Mockito.verify(_featureFlagsWorker, Mockito.times(1)).kill(splitKillNotification);
        Mockito.verify(_featureFlagsWorker, Mockito.times(1)).addToQueue(Mockito.anyObject());
    }

    @Test
    public void processSegmentUpdateAddToQueueInWorker() {
        long changeNumber = 1585867723838L;
        String segmentName = "segment-test";
        String channel = "segments";
        GenericNotificationData genericNotificationData = GenericNotificationData.builder()
                .changeNumber(changeNumber)
                .segmentName(segmentName)
                .channel(channel)
                .build();
        SegmentChangeNotification segmentChangeNotification = new SegmentChangeNotification(genericNotificationData);

        _notificationProcessor.process(segmentChangeNotification);

        Mockito.verify(_segmentWorker, Mockito.times(1)).addToQueue(Mockito.any(SegmentQueueDto.class));
    }

    @Test
    public void processControlNotification() {
        GenericNotificationData genericNotificationData = Mockito.mock(GenericNotificationData.class);
        ControlNotification controlNotification = new ControlNotification(genericNotificationData);

        _notificationProcessor.process(controlNotification);

        Mockito.verify(_pushStatusTracker, Mockito.times(1)).handleIncomingControlEvent(Mockito.any(ControlNotification.class));
    }

    @Test
    public void processOccupancyNotification() {
        GenericNotificationData genericNotificationData = GenericNotificationData.builder()
                .channel("control_pri")
                .build();
        OccupancyNotification occupancyNotification = new OccupancyNotification(genericNotificationData);

        _notificationProcessor.process(occupancyNotification);

        Mockito.verify(_pushStatusTracker, Mockito.times(1)).handleIncomingOccupancyEvent(Mockito.any(OccupancyNotification.class));
    }
}