package io.split.engine;

import io.split.engine.sse.AuthApiClient;
import io.split.engine.sse.SSEHandler;
import io.split.engine.sse.dtos.AuthenticationResponse;
import org.junit.Test;
import org.mockito.Mockito;

public class PushManagerTest {
    private final AuthApiClient _authApiClient;
    private final SSEHandler _sseHandler;
    private final int _authRetryBackOffBase;
    private final PushManager _pushManager;

    public PushManagerTest() {
        _authApiClient = Mockito.mock(AuthApiClient.class);
        _sseHandler = Mockito.mock(SSEHandler.class);
        _authRetryBackOffBase = 1;
        _pushManager = new PushManagerImp(_authApiClient, _sseHandler, _authRetryBackOffBase);
    }

    @Test
    public void startWithPushEnabledShouldConnect() throws InterruptedException {
        AuthenticationResponse response = new AuthenticationResponse(true, "token-test", "channels-test", 1, false);
        AuthenticationResponse response2 = new AuthenticationResponse(true, "token-test-2", "channels-test-2", 1, false);

        Mockito.when(_authApiClient.Authenticate())
                .thenReturn(response)
                .thenReturn(response2);

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

        _pushManager.start();

        Mockito.verify(_authApiClient, Mockito.times(1)).Authenticate();
        Mockito.verify(_sseHandler, Mockito.never()).start(Mockito.any(String.class), Mockito.any(String.class));
        Mockito.verify(_sseHandler, Mockito.times(1)).stop();

        Thread.sleep(1500);

        Mockito.verify(_authApiClient, Mockito.times(2)).Authenticate();
        Mockito.verify(_sseHandler, Mockito.times(1)).start(response2.getToken(), response2.getChannels());
    }
}
