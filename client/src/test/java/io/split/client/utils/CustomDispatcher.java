package io.split.client.utils;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;

public class CustomDispatcher extends Dispatcher {
    public static final String INITIAL_SPLIT_CHANGES = "/api/splitChanges?since=-1";
    public static final String INITIAL_FLAGS_BY_SETS = "/api/splitChanges?since=-1&sets=set1%2Cset2";
    public static final String SINCE_1602796638344 = "/api/splitChanges?since=1602796638344&sets=set1%2Cset2";
    public static final String AUTH_ENABLED = "/api/auth/enabled";
    public static final String AUTH_DISABLED = "/api/auth/disabled";
    public static final String SINCE_1585948850109 = "/api/splitChanges?since=1585948850109";
    public static final String SINCE_1585948850109_FLAG_SET = "/api/splitChanges?since=-1&sets=set_1%2Cset_2";
    public static final String SINCE_1585948850110 = "/api/splitChanges?since=1585948850110";
    public static final String SINCE_1585948850111 = "/api/splitChanges?since=1585948850111";
    public static final String SINCE_1585948850112 = "/api/splitChanges?since=1585948850112";
    public static final String SEGMENT_TEST_INITIAL = "/api/segmentChanges/segment-test?since=-1";
    public static final String SEGMENT3_INITIAL = "/api/segmentChanges/segment3?since=-1";
    public static final String SEGMENT3_SINCE_1585948850110 = "/api/segmentChanges/segment3?since=1585948850110";
    public static final String SEGMENT3_SINCE_1585948850111 = "/api/segmentChanges/segment3?since=1585948850111";
    public static final String SEGMENT_BY_FLAG_SET = "/api/segmentChanges/new_segment?since=-1";
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
            case CustomDispatcher.INITIAL_FLAGS_BY_SETS:
                return getResponse(CustomDispatcher.INITIAL_FLAGS_BY_SETS, new MockResponse().setBody("{\"splits\":[{\"trafficTypeName\":\"client\",\"name\":\"workm\",\"trafficAllocation\":100,\"trafficAllocationSeed\":147392224,\"seed\":524417105,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"on\",\"changeNumber\":1602796638344,\"algo\":2,\"configurations\":{},\"sets\":[\"set1\",\"set2\"],\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"new_segment\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":100},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"in segment new_segment\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":0},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"default rule\"}]},{\"trafficTypeName\":\"client\",\"name\":\"workm_set_3\",\"trafficAllocation\":100,\"trafficAllocationSeed\":147392224,\"seed\":524417105,\"status\":\"ACTIVE\",\"killed\":false,\"defaultTreatment\":\"on\",\"changeNumber\":1602796638344,\"algo\":2,\"configurations\":{},\"sets\":[\"set_3\"],\"conditions\":[{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"IN_SEGMENT\",\"negate\":false,\"userDefinedSegmentMatcherData\":{\"segmentName\":\"new_segment\"},\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":0},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":100},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"in segment new_segment\"},{\"conditionType\":\"ROLLOUT\",\"matcherGroup\":{\"combiner\":\"AND\",\"matchers\":[{\"keySelector\":{\"trafficType\":\"client\",\"attribute\":null},\"matcherType\":\"ALL_KEYS\",\"negate\":false,\"userDefinedSegmentMatcherData\":null,\"whitelistMatcherData\":null,\"unaryNumericMatcherData\":null,\"betweenMatcherData\":null,\"booleanMatcherData\":null,\"dependencyMatcherData\":null,\"stringMatcherData\":null}]},\"partitions\":[{\"treatment\":\"on\",\"size\":100},{\"treatment\":\"off\",\"size\":0},{\"treatment\":\"free\",\"size\":0},{\"treatment\":\"conta\",\"size\":0}],\"label\":\"default rule\"}]}],\"since\":-1,\"till\":1602796638344}"));
            case CustomDispatcher.AUTH_ENABLED:
                return getResponse(CustomDispatcher.AUTH_ENABLED,new MockResponse().setBody(inputStreamToString("streaming-auth-push-enabled.json")));
            case CustomDispatcher.AUTH_DISABLED:
                return getResponse(CustomDispatcher.AUTH_DISABLED,new MockResponse().setBody(inputStreamToString("streaming-auth-push-disabled.json")));
            case CustomDispatcher.SINCE_1585948850109:
                return getResponse(CustomDispatcher.SINCE_1585948850109, new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850110}"));
            case SINCE_1585948850109_FLAG_SET:
                return getResponse(SINCE_1585948850109_FLAG_SET, new MockResponse().setBody("{\"splits\": [], \"since\":1585948850109, \"till\":1585948850110}"));
            case CustomDispatcher.SINCE_1585948850110:
                return getResponse(CustomDispatcher.SINCE_1585948850110, new MockResponse().setBody(inputStreamToString("splits2.json")));
            case CustomDispatcher.SINCE_1585948850111:
                return getResponse(CustomDispatcher.SINCE_1585948850111, new MockResponse().setBody(inputStreamToString("splits_killed.json")));
            case CustomDispatcher.SINCE_1585948850112:
                return getResponse(CustomDispatcher.SINCE_1585948850112, new MockResponse().setBody("{\"splits\": [], \"since\":1585948850112, \"till\":1585948850112}"));
            case CustomDispatcher.SINCE_1602796638344:
                return getResponse(CustomDispatcher.SINCE_1602796638344, new MockResponse().setBody("{\"splits\": [], \"since\":1602796638344, \"till\":1602796638344}"));
            case CustomDispatcher.SEGMENT_TEST_INITIAL:
                return getResponse(CustomDispatcher.SEGMENT_TEST_INITIAL, new MockResponse().setBody("{\"name\": \"segment3\",\"added\": [],\"removed\": [],\"since\": -1,\"till\": -1}"));
            case CustomDispatcher.SEGMENT3_INITIAL:
                return getResponse(CustomDispatcher.SEGMENT3_INITIAL, new MockResponse().setBody(inputStreamToString("segment3.json")));
            case CustomDispatcher.SEGMENT3_SINCE_1585948850110:
                return getResponse(CustomDispatcher.SEGMENT3_SINCE_1585948850110, new MockResponse().setBody("{\"name\": \"segment3\",\"added\": [],\"removed\": [],\"since\": 1585948850110,\"till\": 1585948850110}"));
            case CustomDispatcher.SEGMENT3_SINCE_1585948850111:
                return getResponse(CustomDispatcher.SEGMENT3_SINCE_1585948850111, new MockResponse().setBody("{\"name\": \"segment3\",\"added\": [],\"removed\": [],\"since\": 1585948850111,\"till\": 1585948850111}"));
            case CustomDispatcher.SEGMENT_BY_FLAG_SET:
                return getResponse(CustomDispatcher.SEGMENT3_SINCE_1585948850111, new MockResponse().setBody("{\"name\":\"new_segment\",\"added\":[\"user-1\"],\"removed\":[\"user-2\",\"user-3\"],\"since\":-1,\"till\":-1}"));
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
