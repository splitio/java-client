package io.split.engine.sse;

import io.split.engine.common.PushManager;
import io.split.engine.sse.dtos.*;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PushStatusTrackerTest {
    @Test
    public void HandleControlEventStreamingPausedShouldNotifyEvent() {
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_PAUSED);
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages);

        pushStatusTracker.handleIncomingControlEvent(controlNotification);
        assertThat(messages.size(), is(equalTo(1)));
        assertThat(messages.peek(), is(equalTo(PushManager.Status.STREAMING_DOWN)));
    }

    @Test
    public void HandleControlEventStreamingResumedShouldNotifyEvent() throws InterruptedException {
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages);

        pushStatusTracker.handleIncomingControlEvent(buildControlNotification(ControlType.STREAMING_PAUSED));
        pushStatusTracker.handleIncomingControlEvent(buildControlNotification(ControlType.STREAMING_RESUMED));
        assertThat(messages.size(), is(equalTo(2)));
        assertThat(messages.take(), is(equalTo(PushManager.Status.STREAMING_DOWN)));
        assertThat(messages.take(), is(equalTo(PushManager.Status.STREAMING_READY)));
    }

    @Test
    public void HandleControlEventStreamingResumedShouldNotNotifyEvent() {
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        OccupancyNotification occupancyNotification = buildOccupancyNotification(0, null);
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_RESUMED);

        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages);
        pushStatusTracker.handleIncomingOccupancyEvent(occupancyNotification);
        pushStatusTracker.handleIncomingControlEvent(controlNotification);

        pushStatusTracker.handleIncomingControlEvent(controlNotification);
        assertThat(messages.size(), is(equalTo(1)));
        assertThat(messages.peek(), is(equalTo(PushManager.Status.STREAMING_DOWN)));
    }

    @Test
    public void HandleControlEventStreamingDisabledShouldNotifyShutdownEvent() {
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        ControlNotification controlNotification = buildControlNotification(ControlType.STREAMING_DISABLED);

        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages);
        pushStatusTracker.handleIncomingControlEvent(controlNotification);

        pushStatusTracker.handleIncomingControlEvent(controlNotification);
        assertThat(messages.size(), is(equalTo(1)));
        assertThat(messages.peek(), is(equalTo(PushManager.Status.STREAMING_OFF)));
    }

    @Test
    public void HandleOccupancyEventWithPublishersFirstTimeShouldNotNotifyEvent() {
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        OccupancyNotification occupancyNotification = buildOccupancyNotification(2, null);

        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages);
        pushStatusTracker.handleIncomingOccupancyEvent(occupancyNotification);
        assertThat(messages.size(), is(equalTo(0)));
    }

    @Test
    public void HandleOccupancyEventWithPublishersAndWithStreamingDisabledShouldNotifyEvent() throws InterruptedException {
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages);
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
        LinkedBlockingQueue<PushManager.Status> messages = new LinkedBlockingQueue<>();
        PushStatusTracker pushStatusTracker = new PushStatusTrackerImp(messages);
        pushStatusTracker.handleIncomingOccupancyEvent(buildOccupancyNotification(0, "control_pri"));
        pushStatusTracker.handleIncomingOccupancyEvent(buildOccupancyNotification(2, "control_sec"));

        assertThat(messages.size(), is(equalTo(2)));
        PushManager.Status m1 = messages.take();
        assertThat(m1, is(equalTo(PushManager.Status.STREAMING_DOWN)));

        PushManager.Status m2 = messages.take();
        assertThat(m2, is(equalTo(PushManager.Status.STREAMING_READY)));
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
