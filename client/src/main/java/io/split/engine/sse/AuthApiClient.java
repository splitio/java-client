package io.split.engine.sse;

import io.split.engine.sse.dtos.AuthenticationResponse;

public interface AuthApiClient {
    AuthenticationResponse Authenticate();
}
