package io.split;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class SplitMockServer {
    private final MockWebServer _server;

    public SplitMockServer() {
        _server = new MockWebServer();
        _server.setDispatcher(buildDispatcher());
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

    private Dispatcher buildDispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/splitChanges?since=-1":
                        return new MockResponse().setBody(inputStreamToString("splits.json"));
                    case "/api/auth/enabled":
                        return new MockResponse().setBody(inputStreamToString("streaming-auth-push-enabled.json"));
                    case "/api/auth/disabled":
                        return new MockResponse().setBody(inputStreamToString("streaming-auth-push-disabled.json"));
                    case "/api/splitChanges?since=1585948850109":
                        return new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850110}");
                    case "/api/splitChanges?since=1585948850110":
                        return new MockResponse().setBody(inputStreamToString("splits2.json"));
                    case "/api/splitChanges?since=1585948850111":
                        return new MockResponse().setBody(inputStreamToString("splits_killed.json"));
                    case "/api/segmentChanges/segment-test?since=-1":
                        return new MockResponse().setBody("{\"name\": \"segment3\",\"added\": [],\"removed\": [],\"since\": -1,\"till\": -1}");
                    case "/api/segmentChanges/segment3?since=-1":
                        return new MockResponse().setBody(inputStreamToString("segment3.json"));
                    case "/api/segmentChanges/segment3?since=1585948850110":
                        return new MockResponse().setBody("{\"name\": \"segment3\",\"added\": [],\"removed\": [],\"since\": 1585948850110,\"till\": 1585948850110}");
                    case "/api/metrics/time":
                    case "api/metrics/counter":
                        return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private String inputStreamToString(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        Scanner sc = new Scanner(inputStream);
        StringBuffer sb = new StringBuffer();
        while(sc.hasNext()){
            sb.append(sc.nextLine());
        }

        return sb.toString();
    }
}
