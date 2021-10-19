package io.split;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;

import java.io.IOException;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

public class SplitMockServer {
    private final MockWebServer _server;

    public SplitMockServer(Dispatcher dispatcher) {
        checkNotNull(dispatcher);
        _server = new MockWebServer();
        _server.setDispatcher(dispatcher);
    }

    public void start() throws IOException {
        _server.start();
    }

    public void stop() throws IOException {
        _server.shutdown();
    }

    public String getUrl() {
        return String.format("http://%s:%s", _server.getHostName(), _server.getPort());
    }
}
