package io.split.engine.common;

import io.split.engine.sse.AuthApiClient;
import io.split.engine.sse.EventSourceClient;
import io.split.engine.sse.PushStatusTracker;
import io.split.engine.sse.PushStatusTrackerImp;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.dtos.SegmentQueueDto;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.SplitsWorker;
import io.split.engine.sse.workers.Worker;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.LinkedBlockingQueue;

public class PushManagerTest {
    private final AuthApiClient _authApiClient;
    private final EventSourceClient _eventSourceClient;
    private final Backoff _backoff;
    private final SplitsWorker _splitWorker;
    private final Worker<SegmentQueueDto> _segmentWorker;
    private final PushManager _pushManager;
    private final LinkedBlockingQueue<PushManager.Status> _messages = new LinkedBlockingQueue<>();
    private final PushStatusTracker _pushStatusTracker = new PushStatusTrackerImp(_messages);

    public PushManagerTest() {
        _authApiClient = Mockito.mock(AuthApiClient.class);
        _eventSourceClient = Mockito.mock(EventSourceClient.class);
        _backoff = Mockito.mock(Backoff.class);
        _splitWorker = Mockito.mock(SplitsWorker.class);
        _segmentWorker = Mockito.mock(SegmentsWorkerImp.class);


        _pushManager = new PushManagerImp(_authApiClient, _eventSourceClient, _splitWorker, _segmentWorker, _backoff, _pushStatusTracker);
    }

    @Test
    public void startWithPushEnabledShouldConnect() throws InterruptedException {
        AuthenticationResponse response = new AuthenticationResponse(true, "token-test", "channels-test", 1, false);
        AuthenticationResponse response2 = new AuthenticationResponse(true, "token-test-2", "channels-test-2", 1, false);

        Mockito.when(_authApiClient.Authenticate())
                .thenReturn(response)
                .thenReturn(response2);

        Mockito.when(_eventSourceClient.start(response.getChannels(), response.getToken()))
                .thenReturn(true);

        Mockito.when(_eventSourceClient.start(response2.getChannels(), response2.getToken()))
                .thenReturn(true);

        _pushManager.start();

        Mockito.verify(_authApiClient, Mockito.times(1)).Authenticate();
        Mockito.verify(_eventSourceClient, Mockito.times(1)).start(response.getChannels(), response.getToken());

        Thread.sleep(1500);
        Mockito.verify(_authApiClient, Mockito.times(2)).Authenticate();
        Mockito.verify(_eventSourceClient, Mockito.times(1)).start(response2.getChannels(), response2.getToken());
    }

    @Test
    public void startWithPushDisableShouldNotConnect() throws InterruptedException {
        AuthenticationResponse response = new AuthenticationResponse(false, false);

        Mockito.when(_authApiClient.Authenticate()).thenReturn(response);

        _pushManager.start();

        Mockito.verify(_authApiClient, Mockito.times(1)).Authenticate();
        Mockito.verify(_eventSourceClient, Mockito.never()).start(Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(_eventSourceClient, Mockito.times(1)).stop();

        Thread.sleep(1500);

        Mockito.verify(_authApiClient, Mockito.times(1)).Authenticate();
        Mockito.verify(_eventSourceClient, Mockito.never()).start(Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(_eventSourceClient, Mockito.times(1)).stop();
    }

    @Test
    public void startWithPushDisabledAndRetryTrueShouldConnect() throws InterruptedException {
        AuthenticationResponse response = new AuthenticationResponse(false, true);
        AuthenticationResponse response2 = new AuthenticationResponse(true, "token-test-2", "channels-test-2", 1, false);

        Mockito.when(_authApiClient.Authenticate())
                .thenReturn(response)
                .thenReturn(response2);

        Mockito.when(_backoff.interval())
                .thenReturn(1L);

        _pushManager.start();

        Mockito.verify(_authApiClient, Mockito.times(1)).Authenticate();
        Mockito.verify(_eventSourceClient, Mockito.never()).start(Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(_eventSourceClient, Mockito.times(1)).stop();

        Thread.sleep(1500);

        Mockito.verify(_authApiClient, Mockito.times(2)).Authenticate();
        Mockito.verify(_eventSourceClient, Mockito.times(1)).start(response2.getChannels(), response2.getToken());
    }
}
