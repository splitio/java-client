package io.split.engine.sse;

import io.split.engine.sse.dtos.*;
import io.split.engine.sse.listeners.NotificationKeeperListener;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class NotificationManagerKeeperTest implements NotificationKeeperListener {
    private final AtomicBoolean _streamingAvailable;
    private final AtomicBoolean _streamingShutdown;

    public NotificationManagerKeeperTest() {
        _streamingAvailable = new AtomicBoolean(true);
        _streamingShutdown = new AtomicBoolean(false);
    }

    @Test
    public void HandleControlEventStreamingPausedShouldNotifyEvent() {
        _streamingAvailable.set(true);

        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_PAUSED);
        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(this);

        notificationManagerKeeper.handleIncomingControlEvent(controlNotification);

        assertFalse(_streamingAvailable.get());
    }

    @Test
    public void HandleControlEventStreamingResumedShouldNotifyEvent() {
        _streamingAvailable.set(false);

        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_RESUMED);
        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(this);

        notificationManagerKeeper.handleIncomingControlEvent(controlNotification);

        assertTrue(_streamingAvailable.get());
    }

    @Test
    public void HandleControlEventStreamingResumedShouldNotNotifyEvent() {
        _streamingAvailable.set(true);

        OccupancyNotification occupancyNotification = buildOccupancyNotification(0);
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_RESUMED);

        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(this);

        notificationManagerKeeper.handleIncomingOccupancyEvent(occupancyNotification);
        notificationManagerKeeper.handleIncomingControlEvent(controlNotification);

        assertFalse(_streamingAvailable.get());
    }

    @Test
    public void HandleControlEventStreamingDisabledShouldNotifyShutdownEvent() {
        _streamingShutdown.set(false);

        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_DISABLED);

        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(this);

        notificationManagerKeeper.handleIncomingControlEvent(controlNotification);

        assertTrue(_streamingShutdown.get());
    }

    @Test
    public void HandleOccupancyEventWithPublishersFirstTimeShouldNotNotifyEvent() {
        _streamingAvailable.set(true);

        OccupancyNotification occupancyNotification = buildOccupancyNotification(2);

        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(this);

        notificationManagerKeeper.handleIncomingOccupancyEvent(occupancyNotification);

        assertTrue(_streamingAvailable.get());
    }

    @Test
    public void HandleOccupancyEventWithPublishersAndWithStreamingDisabledShouldNotifyEvent() {
        _streamingAvailable.set(false);

        OccupancyNotification occupancyNotification = buildOccupancyNotification(0);
        OccupancyNotification occupancyNotificationWithPublishers = buildOccupancyNotification(2);

        NotificationManagerKeeper notificationManagerKeeper = new NotificationManagerKeeperImp();
        notificationManagerKeeper.registerNotificationKeeperListener(this);

        notificationManagerKeeper.handleIncomingOccupancyEvent(occupancyNotification);
        notificationManagerKeeper.handleIncomingOccupancyEvent(occupancyNotificationWithPublishers);

        assertTrue(_streamingAvailable.get());
    }

    @Override
    public void onStreamingAvailable() {
        _streamingAvailable.set(true);
    }

    @Override
    public void onStreamingDisabled() {
        _streamingAvailable.set(false);
    }

    @Override
    public void onStreamingShutdown() {
        _streamingShutdown.set(true);
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
