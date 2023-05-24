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

import java.io.UnsupportedEncodingException;

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
    public void processSplitUpdateAddToQueueInWorker() throws UnsupportedEncodingException {
        long changeNumber = 1585867723838L;
        String channel = "splits";
        GenericNotificationData genericNotificationData = new GenericNotificationData(changeNumber, null, null, null, null, null, null, channel, null, null, null);
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
        GenericNotificationData genericNotificationData = new GenericNotificationData(changeNumber, defaultTreatment, splitName, null, null, null, null, channel, null, null, null);
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
        GenericNotificationData genericNotificationData = new GenericNotificationData(changeNumber, null, null, null, null, segmentName, null, channel, null, null, null);
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
        GenericNotificationData genericNotificationData = new GenericNotificationData(null, null, null, null, null, null, null, "control_pri", null, null, null);
        OccupancyNotification occupancyNotification = new OccupancyNotification(genericNotificationData);

        _notificationProcessor.process(occupancyNotification);

        Mockito.verify(_pushStatusTracker, Mockito.times(1)).handleIncomingOccupancyEvent(Mockito.any(OccupancyNotification.class));
    }
}