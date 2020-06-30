package io.split.engine.sse;

import io.split.engine.sse.dtos.*;
import io.split.engine.sse.workers.SplitsWorker;
import org.junit.Test;
import org.mockito.Mockito;

public class SSEHandlerTest {

    private final EventSourceClient _eventSourceClient;
    private final SplitsWorker _splitsWorker;
    private final NotificationProcessor _notificationProcessor;
    private final GenericNotificationData _genericNotificationData;
    private final String _baseUrl = "www.fake.io";

    public SSEHandlerTest() {
        _eventSourceClient = Mockito.mock(EventSourceClient.class);
        _splitsWorker = Mockito.mock(SplitsWorker.class);
        _notificationProcessor = Mockito.mock(NotificationProcessor.class);
        _genericNotificationData = Mockito.mock(GenericNotificationData.class);
    }

    @Test
    public void startShouldConnect() {
        String token = "token-test";
        String channels = "channels-test";
        String url = String.format("%s?channels=%s&v=1.1&accessToken=%s", _baseUrl, channels, token);

        SSEHandler sseHandler = new SSEHandlerImp(_eventSourceClient, _baseUrl, _splitsWorker, _notificationProcessor);
        sseHandler.start(token, channels);

        Mockito.verify(_eventSourceClient, Mockito.times(1)).start(url);
        Mockito.verify(_eventSourceClient, Mockito.never()).stop();
    }

    @Test
    public void stopShouldDisconnect() {
        String token = "token-test";
        String channels = "channels-test";
        String url = String.format("%s?channels=%s&v=1.1&accessToken=%s", _baseUrl, channels, token);

        SSEHandler sseHandler = new SSEHandlerImp(_eventSourceClient, _baseUrl, _splitsWorker, _notificationProcessor);
        sseHandler.start(token, channels);
        sseHandler.stop();

        Mockito.verify(_eventSourceClient, Mockito.times(1)).start(url);
        Mockito.verify(_eventSourceClient, Mockito.times(1)).stop();
    }

    @Test
    public void onMessageNotificationReceivedSplitUpdateShouldProcessNotification() {
        SplitChangeNotification splitChangeNotification = new SplitChangeNotification(_genericNotificationData);

        NotificationsListener sseHandler = new SSEHandlerImp(_eventSourceClient, _baseUrl, _splitsWorker, _notificationProcessor);

        sseHandler.onMessageNotificationReceived(splitChangeNotification);

        Mockito.verify(_notificationProcessor, Mockito.times(1)).process(splitChangeNotification);
    }

    @Test
    public void onMessageNotificationReceivedSplitKillShouldProcessNotification() {
        SplitKillNotification splitKillNotification = new SplitKillNotification(_genericNotificationData);

        NotificationsListener sseHandler = new SSEHandlerImp(_eventSourceClient, _baseUrl, _splitsWorker, _notificationProcessor);

        sseHandler.onMessageNotificationReceived(splitKillNotification);

        Mockito.verify(_notificationProcessor, Mockito.times(1)).process(splitKillNotification);
    }

    @Test
    public void onMessageNotificationReceivedSegmentChangeShouldProcessNotification() {
        SegmentChangeNotification segmentChangeNotification = new SegmentChangeNotification(_genericNotificationData);

        NotificationsListener sseHandler = new SSEHandlerImp(_eventSourceClient, _baseUrl, _splitsWorker, _notificationProcessor);

        sseHandler.onMessageNotificationReceived(segmentChangeNotification);

        Mockito.verify(_notificationProcessor, Mockito.times(1)).process(segmentChangeNotification);
    }

    @Test
    public void onMessageNotificationReceivedControlShouldProcessNotification() {
        ControlNotification controlNotification = new ControlNotification(_genericNotificationData);

        NotificationsListener sseHandler = new SSEHandlerImp(_eventSourceClient, _baseUrl, _splitsWorker, _notificationProcessor);

        sseHandler.onMessageNotificationReceived(controlNotification);

        Mockito.verify(_notificationProcessor, Mockito.never()).process(controlNotification);
    }

    @Test
    public void onMessageNotificationReceivedOccupancyShouldProcessNotification() {
        GenericNotificationData genericNotificationData = Mockito.mock(GenericNotificationData.class);
        OccupancyNotification occupancyNotification = new OccupancyNotification(genericNotificationData);

        NotificationsListener sseHandler = new SSEHandlerImp(_eventSourceClient, _baseUrl, _splitsWorker, _notificationProcessor);

        sseHandler.onMessageNotificationReceived(occupancyNotification);

        Mockito.verify(_notificationProcessor, Mockito.never()).process(occupancyNotification);
    }
}
