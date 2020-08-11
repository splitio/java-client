package io.split.engine.common;

import io.split.engine.sse.AuthApiClient;
import io.split.engine.sse.SSEHandler;
import io.split.engine.sse.dtos.AuthenticationResponse;
import org.junit.Test;
import org.mockito.Mockito;

public class PushManagerTest {
    private final AuthApiClient _authApiClient;
    private final SSEHandler _sseHandler;
    private final Backoff _backoff;
    private final PushManager _pushManager;

    public PushManagerTest() {
        _authApiClient = Mockito.mock(AuthApiClient.class);
        _sseHandler = Mockito.mock(SSEHandler.class);
        _backoff = Mockito.mock(Backoff.class);
        _pushManager = new PushManagerImp(_authApiClient, _sseHandler, _backoff);
    }

    @Test
    public void startWithPushEnabledShouldConnect() throws InterruptedException {
        AuthenticationResponse response = new AuthenticationResponse(true, "token-test", "channels-test", 1, false);
        AuthenticationResponse response2 = new AuthenticationResponse(true, "token-test-2", "channels-test-2", 1, false);

        Mockito.when(_authApiClient.Authenticate())
                .thenReturn(response)
                .thenReturn(response2);

        Mockito.when(_sseHandler.start(response.getToken(), response.getChannels()))
                .thenReturn(true);

        Mockito.when(_sseHandler.start(response2.getToken(), response2.getChannels()))
                .thenReturn(true);

        _pushManager.start();

        Mockito.verify(_authApiClient, Mockito.times(1)).Authenticate();
        Mockito.verify(_sseHandler, Mockito.times(1)).start(response.getToken(), response.getChannels());

        Thread.sleep(1500);
        Mockito.verify(_authApiClient, Mockito.times(2)).Authenticate();
        Mockito.verify(_sseHandler, Mockito.times(1)).start(response2.getToken(), response2.getChannels());
    }

    @Test
    public void startWithPushDisableShouldNotConnect() throws InterruptedException {
        AuthenticationResponse response = new AuthenticationResponse(false, false);

        Mockito.when(_authApiClient.Authenticate()).thenReturn(response);

        _pushManager.start();

        Mockito.verify(_authApiClient, Mockito.times(1)).Authenticate();
        Mockito.verify(_sseHandler, Mockito.never()).start(Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(_sseHandler, Mockito.times(1)).stop();

        Thread.sleep(1500);

        Mockito.verify(_authApiClient, Mockito.times(1)).Authenticate();
        Mockito.verify(_sseHandler, Mockito.never()).start(Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(_sseHandler, Mockito.times(1)).stop();
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
        Mockito.verify(_sseHandler, Mockito.never()).start(Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(_sseHandler, Mockito.times(1)).stop();

        Thread.sleep(1500);

        Mockito.verify(_authApiClient, Mockito.times(2)).Authenticate();
        Mockito.verify(_sseHandler, Mockito.times(1)).start(response2.getToken(), response2.getChannels());
    }
}
