package io.split.engine.common;

import io.split.engine.sse.AuthApiClient;
import io.split.engine.sse.EventSourceClient;
import io.split.engine.sse.PushStatusTracker;
import io.split.engine.sse.PushStatusTrackerImp;
import io.split.engine.sse.dtos.AuthenticationResponse;
import io.split.engine.sse.workers.SegmentsWorkerImp;
import io.split.engine.sse.workers.SplitsWorker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.LinkedBlockingQueue;

public class PushManagerTest {
    private AuthApiClient _authApiClient;
    private EventSourceClient _eventSourceClient;
    private Backoff _backoff;
    private PushManager _pushManager;
    private PushStatusTracker _pushStatusTracker;

    @Before
    public void setUp() {
        _authApiClient = Mockito.mock(AuthApiClient.class);
        _eventSourceClient = Mockito.mock(EventSourceClient.class);
        _backoff = Mockito.mock(Backoff.class);
        _pushStatusTracker = Mockito.mock(PushStatusTrackerImp.class);
        _pushManager = new PushManagerImp(_authApiClient,
                _eventSourceClient,
                Mockito.mock(SplitsWorker.class),
                Mockito.mock(SegmentsWorkerImp.class),
                _pushStatusTracker);
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

        Mockito.verify(_pushStatusTracker, Mockito.times(0)).forceRetryableError();
        Mockito.verify(_pushStatusTracker, Mockito.times(0)).forcePushDisable();
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
        Mockito.verify(_pushStatusTracker, Mockito.times(1)).forceRetryableError();
    }
}
