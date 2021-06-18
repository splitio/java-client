package io.split.engine.sse;

import io.split.engine.common.PushManager;
import io.split.engine.sse.client.SSEClient;
import io.split.engine.sse.dtos.*;
import io.split.telemetry.storage.InMemoryTelemetryStorage;
import io.split.telemetry.storage.TelemetryStorage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PushStatusTrackerTest {
    private static final String CONTROL_PRI = "control_pri";
    private static final String CONTROL_SEC = "control_sec";
    @Test
    public void HandleControlEventStreamingPausedShouldNotifyEvent() {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_PAUSED);
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);

        pushStatusTracker.handleIncomingControlEvent(controlNotification);
        assertThat(messages.size(), is(equalTo(1)));
        assertThat(messages.peek(), is(equalTo(PushManager.Status.STREAMING_DOWN)));
    }

    @Test
    public void HandleControlEventStreamingResumedShouldNotifyEvent() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);

        pushStatusTracker.handleIncomingControlEvent(buildControlNotification(ControlType.STREAMING_PAUSED));
        pushStatusTracker.handleIncomingControlEvent(buildControlNotification(ControlType.STREAMING_RESUMED));
        assertThat(messages.size(), is(equalTo(2)));
        assertThat(messages.take(), is(equalTo(PushManager.Status.STREAMING_DOWN)));
        assertThat(messages.take(), is(equalTo(PushManager.Status.STREAMING_READY)));
    }

    @Test
    public void HandleControlEventStreamingResumedShouldNotNotifyEvent() {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        OccupancyNotification occupancyNotification = buildOccupancyNotification(0, CONTROL_PRI);
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_RESUMED);

        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);
        pushStatusTracker.handleIncomingOccupancyEvent(occupancyNotification);
        pushStatusTracker.handleIncomingControlEvent(controlNotification);

        pushStatusTracker.handleIncomingControlEvent(controlNotification);
        assertThat(messages.size(), is(equalTo(1)));
        assertThat(messages.peek(), is(equalTo(PushManager.Status.STREAMING_DOWN)));
        Assert.assertEquals(1, telemetryStorage.popStreamingEvents().size());
    }

    @Test
    public void HandleControlEventStreamingDisabledShouldNotifyShutdownEvent() {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_DISABLED);

        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);
        pushStatusTracker.handleIncomingControlEvent(controlNotification);

        pushStatusTracker.handleIncomingControlEvent(controlNotification);
        assertThat(messages.size(), is(equalTo(1)));
        assertThat(messages.peek(), is(equalTo(PushManager.Status.STREAMING_OFF)));
    }

    @Test
    public void HandleOccupancyEventWithPublishersFirstTimeShouldNotNotifyEvent() {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        OccupancyNotification occupancyNotification = buildOccupancyNotification(2, CONTROL_SEC);

        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);
        pushStatusTracker.handleIncomingOccupancyEvent(occupancyNotification);
        assertThat(messages.size(), is(equalTo(0)));
        Assert.assertEquals(1, telemetryStorage.popStreamingEvents().size());
    }

    @Test
    public void HandleOccupancyEventWithPublishersAndWithStreamingDisabledShouldNotifyEvent() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);
        pushStatusTracker.handleIncomingOccupancyEvent(buildOccupancyNotification(0, null));
        pushStatusTracker.handleIncomingOccupancyEvent(buildOccupancyNotification(2, null));

        assertThat(messages.size(), is(equalTo(2)));
        PushManager.Status m1 = messages.take();
        assertThat(m1, is(equalTo(PushManager.Status.STREAMING_DOWN)));

        PushManager.Status m2 = messages.take();
        assertThat(m2, is(equalTo(PushManager.Status.STREAMING_READY)));
    }

    @Test
    public void HandleOccupancyEventWithDifferentChannelsPublishersShouldNotifyEvent() throws InterruptedException {
        TelemetryStorage telemetryStorage = new InMemoryTelemetryStorage();
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);
        pushStatusTracker.handleIncomingOccupancyEvent(buildOccupancyNotification(0, "control_pri"));
        pushStatusTracker.handleIncomingOccupancyEvent(buildOccupancyNotification(2, "control_sec"));

        assertThat(messages.size(), is(equalTo(2)));
        PushManager.Status m1 = messages.take();
        assertThat(m1, is(equalTo(PushManager.Status.STREAMING_DOWN)));

        PushManager.Status m2 = messages.take();
        assertThat(m2, is(equalTo(PushManager.Status.STREAMING_READY)));
    }

    @Test
    public void handleSSESTatusRecordTelemetryStreamingEvent() {
        TelemetryStorage telemetryStorage = Mockito.mock(InMemoryTelemetryStorage.class);
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);
        pushStatusTracker.handleSseStatus(SSEClient.StatusMessage.CONNECTED);
        pushStatusTracker.handleSseStatus(SSEClient.StatusMessage.FIRST_EVENT);

        Mockito.verify(telemetryStorage, Mockito.times(1)).recordStreamingEvents(Mockito.any());
    }

    @Test
    public void handleAblyErrorRecordTelemetryStreamingEvent() {
        TelemetryStorage telemetryStorage = Mockito.mock(InMemoryTelemetryStorage.class);
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages, telemetryStorage);
        pushStatusTracker.handleIncomingAblyError(new ErrorNotification("Error", "Ably error", 401));

        Mockito.verify(telemetryStorage, Mockito.times(1)).recordStreamingEvents(Mockito.any());

    }

    private ControlNotification buildControlNotification(ControlType controlType) {
        return new ControlNotification(buildGenericData(controlType, IncomingNotification.Type.CONTROL,null, null));
    }

    private OccupancyNotification buildOccupancyNotification(int publishers, String channel) {
        return new OccupancyNotification(buildGenericData(null, IncomingNotification.Type.OCCUPANCY, publishers, channel));
    }

    private GenericNotificationData buildGenericData(ControlType controlType, IncomingNotification.Type type, Integer publishers, String channel) {
        return new GenericNotificationData(
                null,
                null,
                null,
                controlType,
                publishers != null ? new OccupancyMetrics(publishers) : null,
                null,
                type,
                channel == null ? "channel-test" : channel);
    }
}
