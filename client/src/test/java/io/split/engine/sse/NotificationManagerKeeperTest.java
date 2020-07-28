package io.split.engine.sse;

import io.split.engine.sse.dtos.*;
import io.split.engine.sse.listeners.NotificationKeeperListener;
import org.junit.Test;
import org.mockito.Mockito;

public class NotificationManagerKeeperTest {
    @Test
    public void HandleControlEventStreamingPausedShouldNotifyEvent() {
        NotificationKeeperListener notificationKeeperListener = Mockito.mock(NotificationKeeperListener.class);

        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_PAUSED);
        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(notificationKeeperListener);

        notificationManagerKeeper.handleIncomingControlEvent(controlNotification);

        Mockito.verify(notificationKeeperListener, Mockito.times(1)).onStreamingDisabled();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingAvailable();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingShutdown();
    }

    @Test
    public void HandleControlEventStreamingResumedShouldNotifyEvent() {
        NotificationKeeperListener notificationKeeperListener = Mockito.mock(NotificationKeeperListener.class);

        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_RESUMED);
        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(notificationKeeperListener);

        notificationManagerKeeper.handleIncomingControlEvent(controlNotification);

        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingDisabled();
        Mockito.verify(notificationKeeperListener, Mockito.times(1)).onStreamingAvailable();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingShutdown();
    }

    @Test
    public void HandleControlEventStreamingResumedShouldNotNotifyEvent() {
        NotificationKeeperListener notificationKeeperListener = Mockito.mock(NotificationKeeperListener.class);
        OccupancyNotification occupancyNotification = buildOccupancyNotification(0);
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_RESUMED);

        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(notificationKeeperListener);

        notificationManagerKeeper.handleIncomingOccupancyEvent(occupancyNotification);
        notificationManagerKeeper.handleIncomingControlEvent(controlNotification);

        Mockito.verify(notificationKeeperListener, Mockito.times(1)).onStreamingDisabled();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingAvailable();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingShutdown();
    }

    @Test
    public void HandleControlEventStreamingDisabledShouldNotifyShutdownEvent() {
        NotificationKeeperListener notificationKeeperListener = Mockito.mock(NotificationKeeperListener.class);
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_DISABLED);

        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(notificationKeeperListener);

        notificationManagerKeeper.handleIncomingControlEvent(controlNotification);

        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingDisabled();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingAvailable();
        Mockito.verify(notificationKeeperListener, Mockito.times(1)).onStreamingShutdown();
    }

    @Test
    public void HandleOccupancyEventWithPublishersFirstTimeShouldNotNotifyEvent() {
        NotificationKeeperListener notificationKeeperListener = Mockito.mock(NotificationKeeperListener.class);
        OccupancyNotification occupancyNotification = buildOccupancyNotification(2);

        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(notificationKeeperListener);

        notificationManagerKeeper.handleIncomingOccupancyEvent(occupancyNotification);

        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingDisabled();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingAvailable();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingShutdown();
    }

    @Test
    public void HandleOccupancyEventWithPublishersAndWithStreamingDisabledShouldNotifyEvent() {
        NotificationKeeperListener notificationKeeperListener = Mockito.mock(NotificationKeeperListener.class);
        OccupancyNotification occupancyNotification = buildOccupancyNotification(0);
        OccupancyNotification occupancyNotificationWithPublishers = buildOccupancyNotification(2);

        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(notificationKeeperListener);

        notificationManagerKeeper.handleIncomingOccupancyEvent(occupancyNotification);
        notificationManagerKeeper.handleIncomingOccupancyEvent(occupancyNotificationWithPublishers);

        Mockito.verify(notificationKeeperListener, Mockito.times(1)).onStreamingDisabled();
        Mockito.verify(notificationKeeperListener, Mockito.times(1)).onStreamingAvailable();
        Mockito.verify(notificationKeeperListener, Mockito.times(0)).onStreamingShutdown();
    }

    private ControlNotification buildControlNotification(ControlType controlType) {
        return new ControlNotification(buildGenericData(controlType, IncomingNotification.Type.CONTROL,null));
    }

    private OccupancyNotification buildOccupancyNotification(int publishers) {
        return new OccupancyNotification(buildGenericData(null, IncomingNotification.Type.OCCUPANCY, publishers));
    }

    private GenericNotificationData buildGenericData(ControlType controlType, IncomingNotification.Type type, Integer publishers) {
        return new GenericNotificationData(
                null,
                null,
                null,
                controlType,
                publishers != null ? new OccupancyMetrics(publishers) : null,
                null,
                type,
                "channel-test");
    }
}
