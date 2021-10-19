package io.split.client.utils;

import io.split.client.SplitClientConfig;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mock;

import java.io.InputStream;
import java.util.*;

public class CustomDispatcher extends Dispatcher {
    public static final String INITIAL_SPLIT_CHANGES = "/api/splitChanges?since=-1";
    public static final String AUTH_ENABLED = "/api/auth/enabled";
    public static final String AUTH_DISABLED = "/api/auth/disabled";
    public static final String SINCE_1585948850109 = "/api/splitChanges?since=1585948850109";
    public static final String SINCE_1585948850110 = "/api/splitChanges?since=1585948850110";
    public static final String SINCE_1585948850111 = "/api/splitChanges?since=1585948850111";
    public static final String SINCE_1585948850112 = "/api/splitChanges?since=1585948850112";
    public static final String SEGMENT_TEST_INITIAL = "/api/segmentChanges/segment-test?since=-1";
    public static final String SEGMENT3_INITIAL = "/api/segmentChanges/segment3?since=-1";
    public static final String SEGMENT3_SINCE_1585948850110 = "/api/segmentChanges/segment3?since=1585948850110";
    public static final String SEGMENT3_SINCE_1585948850111 = "/api/segmentChanges/segment3?since=1585948850111";
    public static final String METRICS_TIME = "/api/metrics/time";
    public static final String METRICS_COUNTER = "api/metrics/counter";

    private final Map<String, Queue<MockResponse>>_responses;

    public CustomDispatcher(Map<String, Queue<MockResponse>> responses){
        _responses = responses;
    }

    public static CustomDispatcher.Builder builder() {
        return new CustomDispatcher.Builder();
    }

    @NotNull
    @Override
    public MockResponse dispatch(@NotNull RecordedRequest request) {
        switch (request.getPath()) {
            case CustomDispatcher.INITIAL_SPLIT_CHANGES:
                return getResponse(CustomDispatcher.INITIAL_SPLIT_CHANGES, new MockResponse().setBody(inputStreamToString("splits.json")));
            case CustomDispatcher.AUTH_ENABLED:
                return getResponse(CustomDispatcher.AUTH_ENABLED,new MockResponse().setBody(inputStreamToString("streaming-auth-push-enabled.json")));
            case CustomDispatcher.AUTH_DISABLED:
                return getResponse(CustomDispatcher.AUTH_DISABLED,new MockResponse().setBody(inputStreamToString("streaming-auth-push-disabled.json")));
            case CustomDispatcher.SINCE_1585948850109:
                return getResponse(CustomDispatcher.SINCE_1585948850109, new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850110}"));
            case CustomDispatcher.SINCE_1585948850110:
                return getResponse(CustomDispatcher.SINCE_1585948850110, new MockResponse().setBody(inputStreamToString("splits2.json")));
            case CustomDispatcher.SINCE_1585948850111:
                return getResponse(CustomDispatcher.SINCE_1585948850111, new MockResponse().setBody(inputStreamToString("splits_killed.json")));
            case CustomDispatcher.SINCE_1585948850112:
                return getResponse(CustomDispatcher.SINCE_1585948850112, new MockResponse().setBody("{\"splits\": [], \"since\":1585948850112, \"till\":1585948850112}"));
            case CustomDispatcher.SEGMENT_TEST_INITIAL:
                return getResponse(CustomDispatcher.SEGMENT_TEST_INITIAL, new MockResponse().setBody("{\"name\": \"segment3\",\"added\": [],\"removed\": [],\"since\": -1,\"till\": -1}"));
            case CustomDispatcher.SEGMENT3_INITIAL:
                return getResponse(CustomDispatcher.SEGMENT3_INITIAL, new MockResponse().setBody(inputStreamToString("segment3.json")));
            case CustomDispatcher.SEGMENT3_SINCE_1585948850110:
                return getResponse(CustomDispatcher.SEGMENT3_SINCE_1585948850110, new MockResponse().setBody("{\"name\": \"segment3\",\"added\": [],\"removed\": [],\"since\": 1585948850110,\"till\": 1585948850110}"));
            case CustomDispatcher.SEGMENT3_SINCE_1585948850111:
                return getResponse(CustomDispatcher.SEGMENT3_SINCE_1585948850111, new MockResponse().setBody("{\"name\": \"segment3\",\"added\": [],\"removed\": [],\"since\": 1585948850111,\"till\": 1585948850111}"));
            case CustomDispatcher.METRICS_TIME:
            case CustomDispatcher.METRICS_COUNTER:
                return getResponse(CustomDispatcher.METRICS_COUNTER, new MockResponse().setResponseCode(200));
        }
        return new MockResponse().setResponseCode(404);
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

    private MockResponse getResponse(String target, MockResponse mockedResponse) {
        Queue<MockResponse> responses = _responses.get(target);
        if(responses != null) {
            MockResponse finalResponse = responses.poll();
            return finalResponse == null ? mockedResponse : finalResponse;
        }
        return mockedResponse;
    }



    public static final class Builder {
        private Map<String, Queue<MockResponse>> _responses = new HashMap<>();
        public Builder(){};

        /**
         * Add responses to an specific path
         * @param path
         * @param responses
         * @return
         */
        public Builder path(String path, Queue<MockResponse> responses) {
            _responses.put(path, responses);
            return this;
        }

        public CustomDispatcher build() {
            return new CustomDispatcher(_responses);
        }
    }
}
